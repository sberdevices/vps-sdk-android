package com.arvrlab.vps_sdk.ui

import android.annotation.SuppressLint
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import com.arvrlab.vps_sdk.data.VpsConfig
import com.arvrlab.vps_sdk.data.model.CameraIntrinsics
import com.arvrlab.vps_sdk.domain.interactor.IVpsInteractor
import com.arvrlab.vps_sdk.domain.model.GpsLocationModel
import com.arvrlab.vps_sdk.domain.model.LocalizationBySerialImages
import com.arvrlab.vps_sdk.domain.model.NodePoseModel
import com.arvrlab.vps_sdk.ui.VpsService.State
import com.arvrlab.vps_sdk.util.Logger
import com.arvrlab.vps_sdk.util.waitIfNeedAsync
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.Node
import kotlinx.coroutines.*

internal class VpsServiceImpl(
    private val vpsInteractor: IVpsInteractor,
    private val arManager: ArManager,
    private val locationManager: LocationManager
) : VpsService {

    private companion object {
        const val MIN_INTERVAL_MS = 1000L
        const val MIN_DISTANCE_IN_METERS = 1f
        const val DELAY = 100L

        const val NO_DELAY = 0L
    }

    override val worldNode: Node
        get() = arManager.worldNode

    override val isRun: Boolean
        get() = vpsJob != null

    private val vpsHandlerException: CoroutineExceptionHandler =
        CoroutineExceptionHandler { _, throwable ->
            Logger.error(throwable)
            scope.launch {
                stopVpsService()
                vpsCallback?.onError(throwable)
            }
        }

    private val scope: CoroutineScope = CoroutineScope(
        SupervisorJob() +
                Dispatchers.Main +
                vpsHandlerException
    )

    private var vpsJob: Job? = null
    private var state: State = State.STOP

    private var lastNodePose: NodePoseModel = NodePoseModel.EMPTY

    private lateinit var vpsConfig: VpsConfig
    private var vpsCallback: VpsCallback? = null

    private var gpsLocation: GpsLocationModel? = null
    private val locationListener: LocationListener by lazy {
        object : LocationListener {
            override fun onLocationChanged(location: Location) {
                gpsLocation = GpsLocationModel.from(location)
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }
    }

    private var hasFocus: Boolean = false

    override fun bindArSceneView(arSceneView: ArSceneView) {
        arManager.bindArSceneView(arSceneView)
    }

    override fun resume() {
        hasFocus = true
        if (state == State.PAUSE) {
            internalStartVpsService()
        }
    }

    override fun pause() {
        hasFocus = false
        if (state == State.RUN) {
            state = State.PAUSE
            internalStopVpsService()
        }
    }

    override fun destroy() {
        stopVpsService()
        arManager.destroy()
        vpsInteractor.destroy()
    }

    override fun setVpsConfig(vpsConfig: VpsConfig) {
        this.vpsConfig = vpsConfig
    }

    override fun setVpsCallback(vpsCallback: VpsCallback) {
        this.vpsCallback = vpsCallback
    }

    override fun startVpsService() {
        if (!::vpsConfig.isInitialized)
            throw IllegalStateException("VpsConfig not set. First call setVpsConfig(VpsConfig)")

        internalStartVpsService()
    }

    override fun stopVpsService() {
        if (state != State.STOP) {
            state = State.STOP
            internalStopVpsService()
        }
    }

    private fun internalStartVpsService() {
        if (vpsJob != null) return

        gpsLocation = null
        requestLocationIfNeed()

        vpsJob = scope.launch(Dispatchers.Default) {
            while (!hasFocus) delay(DELAY)

            state = State.RUN
            withContext(Dispatchers.Main) {
                vpsCallback?.onStateChange(state)
            }
            localization()
        }
    }

    private fun internalStopVpsService() {
        vpsCallback?.onStateChange(state)

        if (vpsJob == null) return

        locationManager.removeUpdates(locationListener)
        vpsJob?.cancel()
        vpsJob = null
    }

    private suspend fun localization() {
        val force = vpsConfig.onlyForce
        var firstLocalize = true
        var failureCount = 0

        while (vpsJob?.isActive == true) {
            val result: Any? = if (firstLocalize && vpsConfig.useSerialImages) {
                getNodePoseBySerialImage()
            } else {
                var _force = force || firstLocalize
                if (!firstLocalize && failureCount >= 5 && !force) {
                    _force = true
                }
                getNodePoseBySingleImage(_force)
            }

            if (result == null) {
                failLocalization(++failureCount)
            } else {
                failureCount = 0
                firstLocalize = false
                when (result) {
                    is LocalizationBySerialImages -> successLocalization(
                        result.nodePoseModel,
                        result.indexImage
                    )
                    is NodePoseModel -> successLocalization(result)
                }
            }

            val timeMillis = if (result != null || !firstLocalize)
                vpsConfig.intervalLocalizationMS
            else
                NO_DELAY
            delay(timeMillis)
        }
    }

    private suspend fun successLocalization(
        nodePoseModel: NodePoseModel,
        nodePositionIndex: Int = 0
    ) {
        lastNodePose = nodePoseModel
        withContext(Dispatchers.Main) {
            arManager.restoreCameraPose(nodePositionIndex, lastNodePose)
            vpsCallback?.onSuccess()
        }
    }

    private suspend fun failLocalization(failureCount: Int) {
        withContext(Dispatchers.Main) {
            vpsCallback?.onFail()
        }
        Logger.debug("localization fail: $failureCount")
    }

    private suspend fun getNodePoseBySingleImage(force: Boolean): NodePoseModel? {
        val byteArray: ByteArray
        val currentNodePose: NodePoseModel
        val cameraIntrinsics: CameraIntrinsics

        withContext(Dispatchers.Main) {
            byteArray = waitAcquireCameraImage()
            cameraIntrinsics = arManager.getCameraIntrinsics()
            arManager.saveCameraPose(0)
            currentNodePose =
                if (force)
                    NodePoseModel.EMPTY
                else
                    arManager.getCameraLocalPose()
        }

        return vpsInteractor.calculateNodePose(
            url = vpsConfig.vpsUrl,
            locationID = vpsConfig.locationID,
            source = byteArray,
            localizationType = vpsConfig.localizationType,
            nodePose = currentNodePose,
            force = force,
            gpsLocation = gpsLocation,
            cameraIntrinsics = cameraIntrinsics
        )
    }

    private suspend fun getNodePoseBySerialImage(): LocalizationBySerialImages? {
        val byteArrays = mutableListOf<ByteArray>()
        val nodePoses = mutableListOf<NodePoseModel>()
        val cameraIntrinsics = mutableListOf<CameraIntrinsics>()

        withContext(Dispatchers.Main) {
            repeat(vpsConfig.countImages) { index ->
                val delay = waitIfNeedAsync(
                    { index < vpsConfig.countImages - 1 },
                    vpsConfig.intervalImagesMS
                )

                byteArrays.add(waitAcquireCameraImage())
                cameraIntrinsics.add(arManager.getCameraIntrinsics())
                arManager.saveCameraPose(index)
                nodePoses.add(arManager.getCameraLocalPose())

                Logger.debug("acquire image: ${index + 1}")
                delay?.await()
            }
        }

        return vpsInteractor.calculateNodePose(
            url = vpsConfig.vpsUrl,
            locationID = vpsConfig.locationID,
            sources = byteArrays,
            localizationType = vpsConfig.localizationType,
            nodePoses = nodePoses,
            gpsLocations = gpsLocation?.let { listOf(it) },
            cameraIntrinsics = cameraIntrinsics
        )
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationIfNeed() {
        if (vpsConfig.useGps && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                MIN_INTERVAL_MS,
                MIN_DISTANCE_IN_METERS,
                locationListener
            )
        }
    }

    private suspend fun waitAcquireCameraImage(): ByteArray {
        var byteArray: ByteArray? = null
        while (byteArray == null) {
            try {
                delay(DELAY)
                byteArray = arManager.acquireCameraImageAsByteArray()
            } catch (e: NotYetAvailableException) {
            }
        }
        return byteArray
    }

}