package com.arvrlab.vps_sdk.ui

import android.annotation.SuppressLint
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationManager.GPS_PROVIDER
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
        const val MIN_INTERVAL_MS = 0L
        const val MIN_DISTANCE_IN_METERS = 1f
        const val DELAY = 100L
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

    private val locationListener: LocationListener by lazy {
        DummyLocationListener()
    }

    private var hasFocus: Boolean = false

    override fun bindArSceneView(arSceneView: ArSceneView) {
        arManager.init(arSceneView, vpsConfig)
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

        arManager.detachWorldNode()
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

            delay(vpsConfig.intervalLocalizationMS)
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

        val gpsLocation = if (vpsConfig.useGps)
            getLastKnownLocation() ?: return null
        else
            null

        withContext(Dispatchers.Main) {
            byteArray = waitAcquireCameraImage()
            cameraIntrinsics = arManager.getCameraIntrinsics()
            arManager.saveCameraPose(0)
            currentNodePose = if (force)
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
        val gpsLocations = mutableListOf<GpsLocationModel?>()

        withContext(Dispatchers.Main) {
            repeat(vpsConfig.countImages) { index ->
                val delay = waitIfNeedAsync(
                    { index < vpsConfig.countImages - 1 },
                    vpsConfig.intervalImagesMS
                )

                val gpsLocation = if (vpsConfig.useGps)
                    getLastKnownLocation() ?: return@withContext
                else
                    null

                byteArrays.add(waitAcquireCameraImage())
                cameraIntrinsics.add(arManager.getCameraIntrinsics())
                arManager.saveCameraPose(index)
                nodePoses.add(arManager.getCameraLocalPose())
                gpsLocations.add(gpsLocation)

                Logger.debug("acquire image: ${index + 1}")
                delay?.await()
            }
        }

        if (byteArrays.size != vpsConfig.countImages) return null

        return vpsInteractor.calculateNodePose(
            url = vpsConfig.vpsUrl,
            locationID = vpsConfig.locationID,
            sources = byteArrays,
            localizationType = vpsConfig.localizationType,
            nodePoses = nodePoses,
            gpsLocations = gpsLocations,
            cameraIntrinsics = cameraIntrinsics
        )
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationIfNeed() {
        if (vpsConfig.useGps) {
            locationManager.requestLocationUpdates(
                GPS_PROVIDER,
                MIN_INTERVAL_MS,
                MIN_DISTANCE_IN_METERS,
                locationListener
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation(): GpsLocationModel? =
        if (locationManager.isProviderEnabled(GPS_PROVIDER)) {
            locationManager.getLastKnownLocation(GPS_PROVIDER)
                ?.let { GpsLocationModel.from(it) }
        } else {
            null
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

    private class DummyLocationListener : LocationListener {
        override fun onLocationChanged(location: Location) = Unit
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        override fun onProviderEnabled(provider: String) = Unit
        override fun onProviderDisabled(provider: String) = Unit
    }

}