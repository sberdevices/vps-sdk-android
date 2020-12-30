package ru.arvrlab.vps.service

import android.annotation.SuppressLint
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.Image
import android.util.Log
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Matrix
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import kotlinx.coroutines.*
import okhttp3.MultipartBody
import ru.arvrlab.vps.extentions.*
import ru.arvrlab.vps.network.VpsApi
import ru.arvrlab.vps.network.dto.*
import ru.arvrlab.vps.neuro.NeuroModel
import ru.arvrlab.vps.ui.VpsArFragment
import kotlin.math.PI

class VpsDelegate(
    private val coroutineScope: CoroutineScope,
    private var vpsArFragment: VpsArFragment,
    private var positionNode: Node,
    private var locationManager: LocationManager?,
    private var callback: VpsCallback? = null,
    private val settings: Settings
) {
    private var lastLocation: Location? = null

    private var timerJob: Job? = null

    private var cameraStartRotation = Quaternion()
    private var cameraStartPosition = Vector3()

    private var isModelCreated = false
    private var isTimerRunning = false

    private var cameraAlternativeNode: AnchorNode? = null
    private var rotationNode: AnchorNode? = null

    private var failureCount = 0
    private var force = true
    private var firstLocalize = true

    private var photoTransform: Matrix? = null
    private var successPhotoTransform: Matrix? = null
    private var rotationAngle: Float? = null

    private val neuro by lazy { NeuroModel(vpsArFragment.requireContext()) }

    private val locationListener: LocationListener = LocationListener { location ->
        lastLocation = location
    }

    private var lastResponse: ResponseRelativeDto? = null

    fun start() {
        if (isTimerRunning) {
            return
        }

        if (settings.needLocation) {
            checkPermissionAndLaunchLocalizationUpdate()
        } else {
            launchLocatizationUpdate()
        }
    }

    private fun checkPermissionAndLaunchLocalizationUpdate() {
        if (isGpsProviderEnabled()) {
            requestLocationUpdate()
            launchLocatizationUpdate()
        }
    }

    private fun launchLocatizationUpdate() {
        updateLocalization()

        timerJob = coroutineScope.launch {
            while (true) {
                delay(settings.timerInterval)
                if (isActive) {
                    updateLocalization()
                }
            }
        }

        isTimerRunning = true
    }

    @SuppressLint("MissingPermission")
    fun requestLocationUpdate() {
        locationManager?.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            MIN_INTERVAL_MS,
            MIN_DISTANCE_IN_METERS,
            locationListener
        )
    }

    private fun updateLocalization() {
        coroutineScope.launch(Dispatchers.Main) {

            cameraStartPosition = vpsArFragment.arSceneView.scene.camera.worldPosition

            cameraStartRotation = vpsArFragment.arSceneView.scene.camera.worldRotation

            sendRequest()?.run {
                localize(this)
            }
        }
    }

    private suspend fun sendRequest(): Pair<Quaternion, Vector3>? {
        return withContext(Dispatchers.Main) {
            try {
                Log.i(TAG, "force = $force")
                val responseDto = VpsApi.getApiService(settings.url).process(getRequestDto(), getMultipartBody())

                if (responseDto.responseData?.responseAttributes?.status == "done") {
                    Log.i(TAG, "done")
                    if (!settings.onlyForce) {
                        force = false
                    }

                    firstLocalize = false
                    failureCount = 0
                    callback?.onPositionVps(responseDto)
                    lastResponse = responseDto.responseData.responseAttributes.responseLocation?.responseRelative
                    successPhotoTransform = photoTransform

                    val m31: Float = photoTransform?.data?.get(13) ?: 0f
                    val m32: Float = photoTransform?.data?.get(14) ?: 0f
                    val m33: Float = photoTransform?.data?.get(15) ?: 0f

                    val yangl = Quaternion(Vector3(((lastResponse?.roll ?: 0f) * PI/180f).toFloat(), ((lastResponse?.yaw ?: 0f) * PI/180f).toFloat(), ((lastResponse?.pitch ?: 0f) * PI/180f).toFloat())).toEulerAngles().y
                    val cameraangl = Quaternion(Vector3(m31, m32, m33)).toEulerAngles().y

                    rotationAngle = yangl + cameraangl

                    responseDto.toNewRotationAndPositionPair()
                } else {
                    Log.i(TAG, "fail")
                    failureCount++
                    Log.i(TAG, "failureCount = $failureCount")
                    null
                }

            } catch (e: NotYetAvailableException) {
                null
            } catch (e: CancellationException) {
                null
            } catch (e: Exception) {
                stop()
                callback?.onError(e)
                null
            }
        }
    }

    private fun getRequestDto(): RequestDto {
        return if (settings.needLocation && isGpsProviderEnabled()) {
            createRequestDto(lastLocation)
        } else {
            createRequestDto()
        }
    }

    private fun createRequestDto(location: Location? = null): RequestDto {
        val request = RequestDto(RequestDataDto())

        if (!firstLocalize) {
            if (failureCount >= 2) {
                force = true
            }
        }

        if (!force) {
            Log.i(TAG, "send local")
            setLocalPos(request)
        }

        request.data.attributes.forcedLocalisation = force
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

        Log.i(TAG, "request = $request")
        return request
    }

    private fun setLocalPos(request: RequestDto) {
        val m31: Float = successPhotoTransform?.data?.get(13) ?: 0f
        val m32: Float = successPhotoTransform?.data?.get(14) ?: 0f
        val m33: Float = successPhotoTransform?.data?.get(15) ?: 0f

        val lastCamera = Node()
        val newCamera = Node()

        lastCamera.worldPosition = Vector3(m31, m32, m33)
        newCamera.worldPosition  = vpsArFragment.arSceneView.scene.camera.worldPosition
        newCamera.worldRotation = vpsArFragment.arSceneView.scene.camera.worldRotation

        val anchor = AnchorNode()
        anchor.addChild(lastCamera)
        anchor.addChild(newCamera)

        anchor.worldRotation = Quaternion(Vector3(0f, rotationAngle ?: 0f, 0f))
        val  correct = Vector3(newCamera.worldPosition.x - lastCamera.worldPosition.x,
            newCamera.worldPosition.y - lastCamera.worldPosition.y, newCamera.worldPosition.z - lastCamera.worldPosition.z)
        val eulerNode = Node()
        eulerNode.worldPosition = newCamera.worldPosition
        eulerNode.worldRotation = newCamera.worldRotation

        val newPos = Vector3(lastResponse?.x ?: 0f + correct.x, lastResponse?.y ?: 0f + correct.y, lastResponse?.z ?: 0f + correct.z)
        val newAngle = eulerNode.localRotation.toEulerAngles()

            request.data.attributes.location.localPos.run {
                x = newPos.x
                y = newPos.y
                z = newPos.z
                roll = newAngle.x
                pitch = newAngle.z
                yaw = newAngle.y
            }

        photoTransform = vpsArFragment.arSceneView.scene.camera.worldModelMatrix
    }

    private suspend fun getMultipartBody(): MultipartBody.Part {
        val image: Image = vpsArFragment.arSceneView.arFrame?.acquireCameraImage() ?: throw NotYetAvailableException("Failed to acquire camera image")

        val multipartBody = if (settings.isNeuro) image.toMultipartBodyNeuro(neuro) else image.toMultipartBodyServer()
        image.close()
        return multipartBody
    }

    private fun localize(newRotationAndPosition: Pair<Quaternion, Vector3>) {
        if (!isModelCreated) {
            createNodeHierarchy()
            isModelCreated = true
        }

        cameraAlternativeNode?.worldRotation = getConvertedCameraStartRotation(cameraStartRotation)
        cameraAlternativeNode?.worldPosition = cameraStartPosition
        cameraAlternativeNode?.worldPosition?.z = 0f

        rotationNode?.localRotation = newRotationAndPosition.first
        positionNode.localPosition = newRotationAndPosition.second
    }

    private fun createNodeHierarchy() {
        cameraAlternativeNode = AnchorNode()

        rotationNode = AnchorNode()

        vpsArFragment.arSceneView.scene.addChild(cameraAlternativeNode)
        cameraAlternativeNode?.addChild(rotationNode)
        rotationNode?.addChild(positionNode)

    }

    private fun isGpsProviderEnabled() = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false

    fun stop() {
        firstLocalize = true
        force = true
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
        positionNode.renderable = null

        cameraAlternativeNode = null
        rotationNode = null

        isModelCreated = false
    }

    fun enableForceLocalization(enabled: Boolean) {
        settings.onlyForce = enabled
        if (!enabled) {
            force = true
        }
    }

    companion object {
        private const val TAG = "VpsDelegate"
        private const val MIN_INTERVAL_MS = 1000L
        private const val MIN_DISTANCE_IN_METERS = 1f
    }
}