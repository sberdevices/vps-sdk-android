package com.arvrlab.vps_sdk.ui

import android.annotation.SuppressLint
import android.location.LocationListener
import android.location.LocationManager
import android.media.Image
import com.arvrlab.vps_sdk.data.VpsConfig
import com.arvrlab.vps_sdk.domain.interactor.IVpsInteractor
import com.arvrlab.vps_sdk.domain.model.GpsLocationModel
import com.arvrlab.vps_sdk.domain.model.NodePositionModel
import com.arvrlab.vps_sdk.util.Logger
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

    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)

    private var vpsJob: Job? = null
    private val vpsHandlerException: CoroutineExceptionHandler =
        CoroutineExceptionHandler { _, throwable ->
            Logger.error(throwable)
            scope.launch {
                stopVpsService()
                vpsCallback?.onError(throwable)
            }
        }

    private var lastNodePosition: NodePositionModel = NodePositionModel()

    private lateinit var vpsConfig: VpsConfig
    private var vpsCallback: VpsCallback? = null

    private var gpsLocation: GpsLocationModel? = null
    private val locationListener: LocationListener by lazy {
        LocationListener { location ->
            gpsLocation = GpsLocationModel.from(location)
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
        scope.launch(vpsHandlerException) {
            gpsLocation = null
            requestLocationIfNeed()

            var firstLocalize = true
            var failureCount = 0
            var force = vpsConfig.onlyForce
            vpsJob = launch(Dispatchers.Default) {
                waitResumed()

                Logger.debug("startVpsService")
                withContext(Dispatchers.Main) {
                    vpsCallback?.onStateChange(true)
                }
                while (isActive) {
                    if (!firstLocalize && failureCount >= 2 && !force) {
                        force = true
                    }

                    arManager.savePositions()

                    val image: Image
                    val currentNodePosition: NodePositionModel
                    withContext(Dispatchers.Main) {
                        image = waitAcquireCameraImage()
                        currentNodePosition = arManager.getCurrentNodePosition(lastNodePosition)
                    }

                    vpsInteractor.calculateNodePosition(
                        url = vpsConfig.url,
                        locationID = vpsConfig.locationID,
                        image = image,
                        isNeuro = vpsConfig.isNeuro,
                        nodePosition = currentNodePosition,
                        force = force,
                        gpsLocation = gpsLocation
                    )?.let {
                        firstLocalize = false
                        lastNodePosition = it
                        withContext(Dispatchers.Main) {
                            arManager.updatePositions(lastNodePosition)
                        }
                        vpsCallback?.onSuccess()
                    } ?: run {
                        failureCount++
                        Logger.debug("localization fail: $failureCount")
                    }
                    delay(vpsConfig.timerInterval)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationIfNeed() {
        if (vpsConfig.needLocation && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                MIN_INTERVAL_MS,
                MIN_DISTANCE_IN_METERS,
                locationListener
            )
        }
    }

    private suspend fun waitResumed() {
        while (!isResumed) delay(DELAY)
    }

    private suspend fun waitAcquireCameraImage(): Image {
        var image: Image? = null
        while (image == null) {
            try {
                delay(DELAY)
                image = arManager.acquireCameraImage()
            } catch (e: NotYetAvailableException) {
            }
        }
        return image
    }
}