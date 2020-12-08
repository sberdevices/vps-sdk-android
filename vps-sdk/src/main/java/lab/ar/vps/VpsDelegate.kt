package lab.ar.vps

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import lab.ar.extentions.getConvertedCameraStartRotation
import lab.ar.extentions.toNewRotationAndPositionPair
import lab.ar.network.NetworkHelper
import lab.ar.network.dto.RequestDataDto
import lab.ar.network.dto.RequestDto
import lab.ar.network.dto.RequestGpsDto
import lab.ar.network.dto.ResponseDto
import lab.ar.ui.VpsArFragment
import java.util.*


class VpsDelegate(
    private val coroutineScope: CoroutineScope,
    private val vpsArFragment: VpsArFragment,
    private val modelRenderable: ModelRenderable,
    private val locationManager: LocationManager,
    private var callback: VpsCallback? = null,
    private val vpsSettings: VpsSettings,
    private val onCreateHierarchy: ((tranformableNode: TransformableNode) -> Unit)? = null
) {

    companion object{
        private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
        private const val TAG = "VpsDelegate"
    }

    private val networkHelper = NetworkHelper(vpsSettings.url, callback)
    private var cameraStartRotation = Quaternion()
    private var cameraStartPosition = Vector3()

    private var isModelCreated = false
    private var isTimerRunning = false

    private var cameraAlternativeNode: TransformableNode? = null
    private var rotationSettableNode: TransformableNode? = null
    private var positionSettableNode: TransformableNode? = null

    private var timer: Timer? = null
    private var lastLocation: Location? = null

    private val locationListener: LocationListener = LocationListener { location ->
        lastLocation = location
        logLocation(location)
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
        if(vpsSettings.needLocation) {
            requestLocationPermissionAndInitTimer()
        } else {
            initTimer()
        }
    }

    private fun requestLocationPermissionAndInitTimer() {
        if (!foregroundPermissionApproved()) {
            callback?.onRequestPermission()
            requestForegroundPermissions()
        }else if (isProvidersEnabled()) {
            requestLocationUpdate()
            initTimer()
        }
    }

    private fun initTimer() {
        timer = timer ?: Timer()

        if (!isTimerRunning) {
            timer?.schedule(object : TimerTask() {
                override fun run() {
                    updateLocalization()
                }
            }, 1000, vpsSettings.timerInterval)

            isTimerRunning = true
        }
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
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1f, locationListener)
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1f, locationListener)
    }

    @SuppressLint("MissingPermission")
    private fun updateLocalization() {
        coroutineScope.launch(Dispatchers.Main) {

            cameraStartPosition = vpsArFragment.arSceneView.scene.camera.worldPosition
            cameraStartRotation = vpsArFragment.arSceneView.scene.camera.worldRotation

            val json = if(vpsSettings.needLocation && isProvidersEnabled() && foregroundPermissionApproved()){
                createJsonToSend(lastLocation)
            } else {
                createJsonToSend()
            }
            networkHelper.takePhotoAndSendRequestToServer(vpsArFragment.arSceneView, json)?.run {
                localize(this)
            }
        }
    }

    private fun createJsonToSend(location: Location? = null): RequestDto {
        val request = RequestDto(RequestDataDto())

        request.data.attributes.forcedLocalisation = vpsSettings.onlyForce
        request.data.attributes.location.locationId = vpsSettings.locationID

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
        if (!isModelCreated) {
            createNodeHierarchy()
            isModelCreated = true
        }

        cameraAlternativeNode?.worldRotation = getConvertedCameraStartRotation(cameraStartRotation)
        cameraAlternativeNode?.worldPosition = cameraStartPosition

        rotationSettableNode?.localRotation = newRotationAndPosition.first
        positionSettableNode?.localPosition = newRotationAndPosition.second
    }

    private fun createNodeHierarchy() {
        cameraAlternativeNode = TransformableNode(vpsArFragment.transformationSystem)

        rotationSettableNode = TransformableNode(vpsArFragment.transformationSystem)

        positionSettableNode = TransformableNode(vpsArFragment.transformationSystem).apply {
            renderable = modelRenderable
            scaleController.isEnabled = true
            scaleController.minScale = 0.01f
            scaleController.maxScale = 1f
            onCreateHierarchy?.let { it(this) }
        }

        vpsArFragment.arSceneView.scene.addChild(cameraAlternativeNode)
        cameraAlternativeNode?.addChild(rotationSettableNode)
        rotationSettableNode?.addChild(positionSettableNode)

    }

    fun stop() {
        stopTimer()
        vpsSettings.onlyForce = true
        locationManager.removeUpdates(locationListener)
    }

    fun localizeWithMockData(mockData: ResponseDto) {
        stopTimer()

        try {
            localize(mockData.toNewRotationAndPositionPair())
        } catch (e: Exception) {
            callback?.onError(e)
        }
    }

    private fun stopTimer() {
        isTimerRunning = false
        timer?.cancel()
        timer = null
    }

    fun destroy() {
        stopTimer()
        destroyHierarchy()
    }

    private fun destroyHierarchy() {
        cameraAlternativeNode?.setParent(null)
        rotationSettableNode?.setParent(null)
        cameraAlternativeNode?.setParent(null)
        positionSettableNode?.renderable = null

        cameraAlternativeNode = null
        rotationSettableNode = null
        positionSettableNode = null

        isModelCreated = false
    }

    fun enableForceLocalization(enabled: Boolean) {
        vpsSettings.onlyForce = enabled
    }

    private fun isProvidersEnabled() =
        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

    private fun foregroundPermissionApproved(): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            vpsArFragment.requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
}