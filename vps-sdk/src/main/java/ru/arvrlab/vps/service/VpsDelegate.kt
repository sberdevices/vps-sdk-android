package ru.arvrlab.vps.service

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.Image
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.coroutines.*
import ru.arvrlab.vps.extentions.getConvertedCameraStartRotation
import ru.arvrlab.vps.extentions.toMultipartBody
import ru.arvrlab.vps.extentions.toNewRotationAndPositionPair
import ru.arvrlab.vps.network.VpsApi
import ru.arvrlab.vps.network.dto.RequestDataDto
import ru.arvrlab.vps.network.dto.RequestDto
import ru.arvrlab.vps.network.dto.RequestGpsDto
import ru.arvrlab.vps.network.dto.ResponseDto
import ru.arvrlab.vps.ui.VpsArFragment
import okhttp3.MultipartBody


class VpsDelegate(
    private val coroutineScope: CoroutineScope,
    private var vpsArFragment: VpsArFragment,
    private var renderableModel: Renderable,
    private var locationManager: LocationManager?,
    private var callback: VpsCallback? = null,
    private val settings: Settings,
    private val onCreateHierarchy: ((tranformableNode: TransformableNode) -> Unit)? = null
) {
    private var lastLocation: Location? = null

    private var timerJob: Job? = null

    private var cameraStartRotation = Quaternion()
    private var cameraStartPosition = Vector3()

    private var isModelCreated = false
    private var isTimerRunning = false

    private var cameraAlternativeNode: TransformableNode? = null
    private var rotationNode: TransformableNode? = null
    private var positionNode: TransformableNode? = null

    private val locationListener: LocationListener = LocationListener { location ->
        lastLocation = location
        //  logLocation(location)
    }

    private fun logLocation(location: Location) {
        if (location.provider == LocationManager.GPS_PROVIDER) {
            Log.i(TAG, getFormattedLocation(location))
        } else if (location.provider == LocationManager.NETWORK_PROVIDER) {
            Log.i(TAG, getFormattedLocation(location))
        }
    }

    private fun getFormattedLocation(location: Location): String {
        return "Coordinates: lat = ${location.latitude}, lon = ${location.longitude}, time = ${location.elapsedRealtimeNanos}," +
                "altitude = ${location.altitude}, accuracy = ${location.accuracy}"
    }

    fun start() {
        if(isTimerRunning) {
            return
        }

        if (settings.needLocation) {
            checkPermissionAndLaunchLocalizationUpdate()
        } else {
            launchLocatizationUpdate()
        }
    }

    private fun checkPermissionAndLaunchLocalizationUpdate() {
        if (!foregroundPermissionApproved()) {
            callback?.onVpsServiceWasNotStarted()
            requestForegroundPermissions()
        } else if (isProvidersEnabled()) {
            requestLocationUpdate()
            launchLocatizationUpdate()
        }
    }

    private fun launchLocatizationUpdate() {
        if (!isTimerRunning) {
            updateLocalization()
        }

        timerJob = coroutineScope.launch {
            while (true) {
                delay(settings.timerInterval)
                ensureActive()
                updateLocalization()
            }
        }

        isTimerRunning = true
    }

    private fun requestForegroundPermissions() {
        ActivityCompat.requestPermissions(
            vpsArFragment.requireActivity(),
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        )
    }

    @SuppressLint("MissingPermission")
    fun requestLocationUpdate() {
        locationManager?.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            MIN_INTERVAL_MS,
            MIN_DISTANCE_IN_METERS,
            locationListener
        )
        locationManager?.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER,
            MIN_INTERVAL_MS,
            MIN_DISTANCE_IN_METERS,
            locationListener
        )
    }

    @SuppressLint("MissingPermission")
    private fun updateLocalization() {
        coroutineScope.launch(Dispatchers.Main) {

            cameraStartPosition = vpsArFragment.arSceneView.scene.camera.worldPosition

            cameraStartRotation = vpsArFragment.arSceneView.scene.camera.worldRotation

            sendRequest()?.run {
                localize(this)
            }
        }
    }

    private suspend fun getMultipart(): MultipartBody.Part {
        val image: Image = vpsArFragment.arSceneView.arFrame?.acquireCameraImage() ?: throw NotYetAvailableException("Failed to acquire camera image")
        val multipartBody = image.toMultipartBody()
        image.close()
        return multipartBody
    }

    private suspend fun sendRequest(): Pair<Quaternion, Vector3>? {
        return withContext(Dispatchers.Main) {
            try {
                val responseDto = VpsApi.getApiService(settings.url).process(getRequestDto(), getMultipart())
                callback?.onPositionVps(responseDto)
                responseDto.toNewRotationAndPositionPair()
            }catch (e: NotYetAvailableException) {
                null
            } catch (e: Exception) {
                stop()
                callback?.onError(e)
                null
            }
        }
    }

    private fun getRequestDto(): RequestDto {
        return if (settings.needLocation && isProvidersEnabled() && foregroundPermissionApproved()) {
            createRequestDto(lastLocation)
        } else {
            createRequestDto()
        }
    }

    private fun createRequestDto(location: Location? = null): RequestDto {
        val request = RequestDto(RequestDataDto())

        request.data.attributes.forcedLocalisation = settings.onlyForce
        request.data.attributes.location.locationId = settings.locationID

        location?.run {
            request.data.attributes.location.gps = RequestGpsDto(
                accuracy.toDouble(),
                altitude,
                latitude,
                longitude,
                elapsedRealtimeNanos.toDouble()
            )
        }

        return request
    }

    private fun localize(newRotationAndPosition: Pair<Quaternion, Vector3>) {
        if(!isTimerRunning) {
            return
        }

        if (!isModelCreated) {
            createNodeHierarchy()
            isModelCreated = true
        }

        cameraAlternativeNode?.worldRotation = getConvertedCameraStartRotation(cameraStartRotation)
        cameraAlternativeNode?.worldPosition = cameraStartPosition

        rotationNode?.localRotation = newRotationAndPosition.first
        positionNode?.localPosition = newRotationAndPosition.second
    }

    private fun createNodeHierarchy() {
        cameraAlternativeNode = TransformableNode(vpsArFragment.transformationSystem)

        rotationNode = TransformableNode(vpsArFragment.transformationSystem)

        positionNode = TransformableNode(vpsArFragment.transformationSystem).apply {
            renderable = renderableModel
            onCreateHierarchy?.let { it(this) }
        }

        vpsArFragment.arSceneView.scene.addChild(cameraAlternativeNode)
        cameraAlternativeNode?.addChild(rotationNode)
        rotationNode?.addChild(positionNode)

    }

    private fun isProvidersEnabled() =
        locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false
                || locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ?: false

    private fun foregroundPermissionApproved(): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            vpsArFragment.requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    fun stop() {
        stopLocalizationUpdate()
        locationManager?.removeUpdates(locationListener)
    }

    fun localizeWithMockData(mockData: ResponseDto) {
        stopLocalizationUpdate()

        try {
            localize(mockData.toNewRotationAndPositionPair())
        } catch (e: Exception) {
            callback?.onError(e)
        }
    }

    private fun stopLocalizationUpdate() {
        timerJob?.cancel()
        isTimerRunning = false
    }

    fun destroy() {
        stop()
        destroyHierarchy()
    }

    private fun destroyHierarchy() {
        cameraAlternativeNode?.setParent(null)
        rotationNode?.setParent(null)
        cameraAlternativeNode?.setParent(null)
        positionNode?.renderable = null

        cameraAlternativeNode = null
        rotationNode = null
        positionNode = null

        isModelCreated = false
    }

    fun enableForceLocalization(enabled: Boolean) {
        settings.onlyForce = enabled
    }

    companion object {
        private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
        private const val TAG = "VpsDelegate"
        private const val MIN_INTERVAL_MS = 1000L
        private const val MIN_DISTANCE_IN_METERS = 1f
    }
}