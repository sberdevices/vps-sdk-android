package com.arvrlab.vps_sdk.domain.interactor

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.Image
import androidx.core.content.ContextCompat
import com.arvrlab.vps_sdk.data.VpsConfig
import com.arvrlab.vps_sdk.data.repository.IVpsRepository
import com.arvrlab.vps_sdk.domain.model.LocalPositionModel
import com.arvrlab.vps_sdk.domain.model.VpsLocationModel
import com.arvrlab.vps_sdk.domain.neuro.NeuroModel
import com.arvrlab.vps_sdk.ui.VpsCallback
import com.arvrlab.vps_sdk.util.Logger
import com.arvrlab.vps_sdk.util.toNewRotationAndPositionPair
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import kotlinx.coroutines.*

internal class VpsInteractor(
    private val context: Context,
    private val arInteractor: IArInteractor,
    private val vpsRepository: IVpsRepository
) : IVpsInteractor {

    private companion object {
        const val MIN_INTERVAL_MS = 1000L
        const val MIN_DISTANCE_IN_METERS = 1f
    }

    override lateinit var vpsConfig: VpsConfig
        private set

    private var timerJob: Job? = null
    private var isTimerRunning = false

    private var lastLocalPosition: LocalPositionModel = LocalPositionModel()

    private var failureCount = 0
    private var force = true
    private var firstLocalize = true

    private var lastLocation: Location? = null
    private val locationListener: LocationListener by lazy {
        LocationListener { location -> lastLocation = location }
    }
    private val locationManager: LocationManager by lazy {
        ContextCompat.getSystemService(context, LocationManager::class.java) as LocationManager
    }

    private var vpsCallback: VpsCallback? = null

    private val neuroModel: NeuroModel by lazy { NeuroModel(context) }

    override fun setVpsConfig(vpsConfig: VpsConfig) {
        this.vpsConfig = vpsConfig
    }

    override fun setVpsCallback(vpsCallback: VpsCallback) {
        this.vpsCallback = vpsCallback
    }

    override fun enableForceLocalization(enabled: Boolean) {
        vpsConfig.onlyForce = enabled
        if (!enabled) {
            force = true
        }
    }

    override suspend fun startLocatization() {
        if (isTimerRunning) return

        if (vpsConfig.needLocation) {
            if (isGpsProviderEnabled()) {
                requestLocationUpdates()
                launchLocatizationUpdate()
            }
        } else {
            launchLocatizationUpdate()
        }
    }

    override fun stopLocatization() {
        firstLocalize = true
        force = true
        stopLocalizationUpdate()
        locationManager.removeUpdates(locationListener)
    }

    override fun destroy() {
        neuroModel.close()
        stopLocatization()
        arInteractor.destroyHierarchy()
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            MIN_INTERVAL_MS,
            MIN_DISTANCE_IN_METERS,
            locationListener
        )
    }

    private fun isGpsProviderEnabled(): Boolean =
        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

    private suspend fun launchLocatizationUpdate() {
        updateLocalization()
        coroutineScope {
            timerJob = launch(Dispatchers.Default) {
                while (true) {
                    delay(vpsConfig.timerInterval)
                    if (isActive) {
                        updateLocalization()
                    }
                }
            }
        }

        isTimerRunning = true
    }

    private fun stopLocalizationUpdate() {
        timerJob?.cancel()
        isTimerRunning = false
    }

    private suspend fun updateLocalization() {
        withContext(Dispatchers.Main) {
            arInteractor.updateLocalization()

            try {
                val image: Image = arInteractor.acquireCameraImage()
                    ?: throw NotYetAvailableException("Failed to acquire camera image")
                val location = if (vpsConfig.needLocation && isGpsProviderEnabled()) lastLocation else null
                val localPosition = arInteractor.getLocalPosition(lastLocalPosition)

                if (!firstLocalize) {
                    if (failureCount >= 2) {
                        force = true
                    }
                }

                val vpsLocationModel = VpsLocationModel(
                    vpsConfig.url,
                    vpsConfig.locationID,
                    location,
                    image,
                    vpsConfig.isNeuro,
                    localPosition,
                    force
                )

                val responseLocalPosition = vpsRepository.getLocation(vpsLocationModel)
                if (responseLocalPosition != null) {
                    val (rotation, position) = onSuccessResponse(responseLocalPosition)
                    arInteractor.localize(rotation, position)
                } else {
                    onFailResponse()
                }
            } catch (e: NotYetAvailableException) {
                Logger.error(e)
            } catch (e: CancellationException) {
                Logger.error(e)
            } catch (e: Exception) {
                Logger.error(e.stackTraceToString())
                stopLocatization()
                vpsCallback?.onError(e)
            }
        }
    }

    private fun onSuccessResponse(localPosition: LocalPositionModel): Pair<Quaternion, Vector3> {
        if (!vpsConfig.onlyForce) {
            force = false
        }

        firstLocalize = false
        failureCount = 0
        vpsCallback?.onPositionVps()
        lastLocalPosition = localPosition

        arInteractor.updateRotationAngle(lastLocalPosition)

        return localPosition.toNewRotationAndPositionPair()
    }

    private fun onFailResponse() {
        Logger.debug("fail")
        failureCount++
        Logger.debug("failureCount: $failureCount")
    }

}