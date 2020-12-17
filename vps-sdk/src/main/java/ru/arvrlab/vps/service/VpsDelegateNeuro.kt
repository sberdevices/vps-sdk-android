package ru.arvrlab.vps.service

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.Image
import android.util.Base64
import android.util.Log
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import ru.arvrlab.vps.extentions.*
import ru.arvrlab.vps.network.VpsApi
import ru.arvrlab.vps.network.dto.RequestDataDto
import ru.arvrlab.vps.network.dto.RequestDto
import ru.arvrlab.vps.network.dto.RequestGpsDto
import ru.arvrlab.vps.network.dto.ResponseDto
import ru.arvrlab.vps.neuro.NeuroModel
import ru.arvrlab.vps.neuro.NeuroResult
import ru.arvrlab.vps.ui.VpsArFragment
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder


class VpsDelegateNeuro(
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

    private val neuro by lazy { NeuroModel(vpsArFragment.requireContext()) }

    private val locationListener: LocationListener = LocationListener { location ->
        lastLocation = location
    }

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
        if (!isTimerRunning) {
            updateLocalization()
        }

//        timerJob = coroutineScope.launch {
//            while (true) {
//                delay(settings.timerInterval)
//                if (isActive) {
//                    updateLocalization()
//                }
//            }
//        }

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
                Log.i(TAG, "failureCount = $failureCount")
                val responseDto = VpsApi.getApiService(settings.url).process(
                    getRequestDto(),
                    getMultipart() ?: return@withContext null
                )

                Log.i(TAG, "status = ${responseDto.responseData?.responseAttributes?.status}")
                if (responseDto.responseData?.responseAttributes?.status == "done") {
                    if (!settings.onlyForce) {
                        force = false
                    }

                    firstLocalize = false
                    failureCount = 0
                    callback?.onPositionVps(responseDto)
                    responseDto.toNewRotationAndPositionPair()
                } else {
                    failureCount++
                    null
                }

            } catch (e: NotYetAvailableException) {
                null
            } catch (e: CancellationException) {
                null
            } catch (e: Exception) {
                stop()
                Log.e(TAG, e.toString())
                callback?.onError(e)
                null
            }
        }
    }

    // создать функцию которая сохраняла последнюю битмапу
    private suspend fun getMultipart(): MultipartBody.Part? {
        val image: Image = vpsArFragment.arSceneView.arFrame?.acquireCameraImage()
            ?: throw NotYetAvailableException(
                "Failed to acquire camera image"
            )
        val multipartBody = multipartBody(image)
        image.close()
        return multipartBody
    }

    private suspend fun multipartBody(image: Image): MultipartBody.Part? {
        return withContext(Dispatchers.IO) {

            val imageInByteArray = image.toByteArrayNeuroVersion()
            val bitmap = BitmapFactory.decodeByteArray(imageInByteArray, 0, imageInByteArray.size)

            val neuroResult = neuro.getFeatures(bitmap)
            val byteArray = getFileAsByteArray(neuroResult)

            bitmap.recycle()

            val requestBody =
                byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull(), 0, byteArray.size)
            MultipartBody.Part.createFormData("embedding", "embedding.embd", requestBody)
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

        return request
    }

    private fun setLocalPos(request: RequestDto) {
        val anchor =
            vpsArFragment.arSceneView.session?.createAnchor(vpsArFragment.arSceneView.arFrame?.camera?.pose)
        val anchorNode = AnchorNode(anchor)
        anchorNode.setParent(positionNode)

        val localPos = anchorNode.localPosition
        val localRot = anchorNode.localRotation.toEulerAngles()

        localPos.let {
            request.data.attributes.location.localPos.run {
                x = it.x
                y = it.y
                z = it.z
                roll = localRot.z
                pitch = localRot.x
                yaw = localRot.y
            }
        }

        anchorNode.setParent(null)
    }

    private fun localize(newRotationAndPosition: Pair<Quaternion, Vector3>) {
//        if (!isTimerRunning) {
//            return
//        }

        if (!isModelCreated) {
            createNodeHierarchy()
            isModelCreated = true
        }

        cameraAlternativeNode?.worldRotation = getConvertedCameraStartRotation(cameraStartRotation)
        cameraAlternativeNode?.worldPosition = cameraStartPosition

//test
//        val euler = newRotationAndPosition.first.toEulerAngles()
//        val newQuart = Quaternion(euler)
//        rotationNode?.localRotation = newQuart

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

    private fun isGpsProviderEnabled() =
        locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false

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
        //destroyHierarchy()
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

    private suspend fun getFileAsByteArray(neuroResult: NeuroResult): ByteArray {
        return withContext(Dispatchers.IO) {

            ByteArrayOutputStream().use { fileData ->
                val version: Byte = 0x0
                val id: Byte = 0x0
                fileData.write(byteArrayOf(version, id))

                val keyPoints = getByteFrom2(neuroResult.keyPoints)
                fileData.write(keyPoints.size.toByteArray())
                fileData.write(keyPoints)

                val scores = getByteFrom1(neuroResult.scores)
                fileData.write(scores.size.toByteArray())
                fileData.write(scores)

                val localDescriptors = getByteFrom2(neuroResult.localDescriptors)
                fileData.write(localDescriptors.size.toByteArray())
                fileData.write(localDescriptors)

                val globalDescriptor = getByteFrom1(neuroResult.globalDescriptor)
                fileData.write(globalDescriptor.size.toByteArray())
                fileData.write(globalDescriptor)

                fileData.toByteArray()
            }
        }
    }

    private fun getByteFrom1(floatArray: FloatArray): ByteArray {
        return ByteArrayOutputStream().use { out ->
            val buff = getBuffer(floatArray)
            val base64Str = convertToBase64Bytes(buff.array())
            out.write(base64Str)

            out.toByteArray()
        }

    }

    private fun getByteFrom2(array: Array<FloatArray>): ByteArray {
        val arr = ByteArrayOutputStream().use { out ->
            array.forEach { floatArray ->
                val buff = getByteArrayFromFloatArray(floatArray)

                out.write(buff)
            }
            out.toByteArray()
        }

        return convertToBase64Bytes(arr)
    }

    private fun convertToBase64Bytes(buff: ByteArray): ByteArray {
        return Base64.encode(buff, Base64.NO_WRAP)
    }

    private fun getBuffer(floatArray: FloatArray): ByteBuffer {
        val buff: ByteBuffer = ByteBuffer.allocate(4 * floatArray.size)
        buff.order(ByteOrder.LITTLE_ENDIAN)
        for (value in floatArray) {
            buff.putFloat(value)
        }

        return buff
    }

    private fun getByteArrayFromFloatArray(floatArray: FloatArray): ByteArray {
        val buff: ByteBuffer = ByteBuffer.allocate(4 * floatArray.size)
        buff.order(ByteOrder.LITTLE_ENDIAN)
        for (value in floatArray) {
            buff.putFloat(value)
        }

        return buff.array()
    }

    companion object {
        private const val TAG = "VpsDelegate"
        private const val MIN_INTERVAL_MS = 1000L
        private const val MIN_DISTANCE_IN_METERS = 1f
    }
}