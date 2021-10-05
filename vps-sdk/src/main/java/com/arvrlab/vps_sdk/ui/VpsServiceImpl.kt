package com.arvrlab.vps_sdk.ui

import android.annotation.SuppressLint
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import com.arvrlab.vps_sdk.data.VpsConfig
import com.arvrlab.vps_sdk.domain.interactor.IVpsInteractor
import com.arvrlab.vps_sdk.domain.model.GpsLocationModel
import com.arvrlab.vps_sdk.domain.model.LocalizationBySerialImages
import com.arvrlab.vps_sdk.domain.model.NodePositionModel
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

    private var lastNodePosition: NodePositionModel = NodePositionModel()

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

    private var isResumed: Boolean = false

    override fun bindArSceneView(arSceneView: ArSceneView) {
        arManager.bindArSceneView(arSceneView)
    }

    override fun resume() {
        isResumed = true
    }

    override fun pause() {
        isResumed = false
    }

    override fun unbindArSceneView() {
        arManager.unbind()
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

        if (vpsJob != null) return

        internalStartVpsService()
    }

    override fun stopVpsService() {
        if (vpsJob == null) return

        Logger.debug("stopVpsService")
        vpsCallback?.onStateChange(false)
        vpsJob?.cancel()
        vpsJob = null
        locationManager.removeUpdates(locationListener)
    }

    private fun internalStartVpsService() {
        gpsLocation = null
        requestLocationIfNeed()

        vpsJob = scope.launch(Dispatchers.Default) {
            while (!isResumed) delay(DELAY)

            Logger.debug("startVpsService")
            withContext(Dispatchers.Main) {
                vpsCallback?.onStateChange(true)
            }
            if (vpsConfig.countImages == 1) {
                localizationBySingleImage()
            } else {
                localizationBySerialImage()
            }
        }
    }

    private suspend fun localizationBySingleImage() {
        var firstLocalize = true
        var failureCount = 0
        var force = vpsConfig.onlyForce

        while (vpsJob?.isActive == true) {
            if (!firstLocalize && failureCount >= 2 && !force) {
                force = true
            }

            getNodePositionBySingleImage(force)?.let {
                firstLocalize = false
                lastNodePosition = it
                withContext(Dispatchers.Main) {
                    arManager.restoreWorldPosition(0, lastNodePosition)
                    vpsCallback?.onSuccess()
                }
            } ?: run {
                withContext(Dispatchers.Main) {
                    vpsCallback?.onFail()
                }
                failureCount++
                Logger.debug("localization fail: $failureCount")
            }
            delay(vpsConfig.intervalLocalizationMS)
        }
    }

    private suspend fun localizationBySerialImage() {
        var failureCount = 0
        while (vpsJob?.isActive == true) {
            getNodePositionBySerialImage()?.let { (nodePositionModel, indexImage) ->
                lastNodePosition = nodePositionModel
                withContext(Dispatchers.Main) {
                    arManager.restoreWorldPosition(indexImage, lastNodePosition)
                }
                vpsCallback?.onSuccess()
            } ?: run {
                failureCount++
                Logger.debug("localization fail: $failureCount")
            }
            delay(vpsConfig.intervalLocalizationMS)
        }
    }

    private suspend fun getNodePositionBySingleImage(force: Boolean): NodePositionModel? {
        val byteArray: ByteArray
        val currentNodePosition: NodePositionModel

        withContext(Dispatchers.Main) {
            arManager.saveWorldPosition(0)
            currentNodePosition = arManager.getWorldNodePosition(lastNodePosition)
            byteArray = waitAcquireCameraImage()
        }

        return vpsInteractor.calculateNodePosition(
            url = vpsConfig.vpsUrl,
            locationID = vpsConfig.locationID,
            source = byteArray,
            localizationType = vpsConfig.localizationType,
            nodePosition = currentNodePosition,
            force = force,
            gpsLocation = gpsLocation
        )
    }

    private suspend fun getNodePositionBySerialImage(): LocalizationBySerialImages? {
        val byteArrays = mutableListOf<ByteArray>()
        val nodePositions = mutableListOf<NodePositionModel>()

        withContext(Dispatchers.Main) {
            repeat(vpsConfig.countImages) { index ->
                val delay = waitIfNeedAsync(
                    { index < vpsConfig.countImages - 1 },
                    vpsConfig.intervalImagesMS
                )

                arManager.saveWorldPosition(index)
                nodePositions.add(arManager.getWorldNodePosition(lastNodePosition))
                byteArrays.add(waitAcquireCameraImage())

                Logger.debug("acquire image: ${index + 1}")
                delay?.await()
            }
        }

        return vpsInteractor.calculateNodePosition(
            url = vpsConfig.vpsUrl,
            locationID = vpsConfig.locationID,
            sources = byteArrays,
            localizationType = vpsConfig.localizationType,
            nodePositions = nodePositions,
            gpsLocations = gpsLocation?.let { listOf(it) }
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