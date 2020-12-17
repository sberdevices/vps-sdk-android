package ru.arvrlab.vps.service

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
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
import java.io.File
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

           // val b = image.planes[0].buffer
           // val bA = ByteArray(b.remaining())
           // b.get(bA)//попробовать байт буффер.array
           // val bytes = image.toByteArray()
           // val a = YuvImage().compressToJpeg() //погуглить
        //    val bitmap = BitmapFactory.decodeByteArray(bA, 0, bA.size)
           //     val b2 = bitmap.rotate(90f)

            //////////-----//////
            val out = ByteArrayOutputStream().use { out ->
                val bA = ByteBuffer.allocateDirect(image.width * image.height * 2)
                image.planes.forEach { image ->
                    bA.put(image.buffer)
                }
                val yuv = YuvImage(bA.array(), ImageFormat.NV21, image.width, image.height, null)
                yuv.compressToJpeg(Rect(0, 0, image.width, image.height), 90, out)
                out.toByteArray()

            }

            //hardcode
          //  val out = vpsArFragment.requireActivity().assets.open("bootcamp.jpg").readBytes()
            //todo измерь скорость
            val bitmap = BitmapFactory.decodeByteArray(out, 0, out.size).toBlackAndWhiteBitmap().rotate(90f)

            val neuroResult = neuro.run(bitmap)
            Log.i(TAG, "neuroResult = $neuroResult")
            val byteArray = neuroResult?.let { createFile(it) }

            bitmap.recycle()

            val requestBody = byteArray?.toRequestBody(
                "image/jpeg".toMediaTypeOrNull(),
                0,
                byteArray.size
            ) ?: return@withContext null
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

    private suspend fun createFile(neuroResult: NeuroResult) : ByteArray {
        return withContext(Dispatchers.IO) {

            ByteArrayOutputStream().use { filedata ->
                val version: Byte = 0x0
                val id: Byte = 0x0

                val bA = byteArrayOf(version, id)
                filedata.write(bA)

                val keyPoints = getByteFrom2(neuroResult.keyPoints)
                Log.i(TAG, "keyPoints = " + keyPoints.size)
                val buffer1 = ByteBuffer.allocate(Int.SIZE_BYTES)
                buffer1.putInt(keyPoints.size)
                filedata.write(buffer1.array())
                filedata.write(keyPoints)

                val scores = getByteFrom1(neuroResult.scores)
                Log.i(TAG, "scores = " + scores.size)
                val buffer2 = ByteBuffer.allocate(Int.SIZE_BYTES)
                buffer2.putInt(scores.size)
                filedata.write(buffer2.array())
                filedata.write(scores)

                val localDescriptors = getByteFrom2(neuroResult.localDescriptors)
                Log.i(TAG, "localDescriptors = " + localDescriptors.size)
                val buffer3 = ByteBuffer.allocate(Int.SIZE_BYTES)
                buffer3.putInt(localDescriptors.size)
                filedata.write(buffer3.array())
                filedata.write(localDescriptors)

                val globalDescriptor = getByteFrom1(neuroResult.globalDescriptor)
                Log.i(TAG, "globalDescriptor = " + globalDescriptor.size)
                val buffer4 = ByteBuffer.allocate(Int.SIZE_BYTES)
                buffer4.putInt( globalDescriptor.size)
                filedata.write(buffer4.array())
                filedata.write(globalDescriptor)

                val byteArray = filedata.toByteArray()
                Log.i(TAG, "byteArray.size = ${byteArray.count()}")
                byteArray
            }
        }
    }

    private fun getByteFrom1(floatArray: FloatArray) : ByteArray {
        return ByteArrayOutputStream().use { out ->
            val buff = getBuffer(floatArray)
            val base64Str = convertToBase64Bytes(buff)
            out.write(base64Str)

            Log.i(TAG, "fun1 = " + out.size())
            out.toByteArray()
        }

    }

//    private fun getByteFrom2(array: Array<FloatArray>) : ByteArray {
//        return ByteArrayOutputStream().use { out ->
//            array.forEach { floatArray ->
//                val buff = getBuffer(floatArray)
//                val base64Str = convertToBase64Bytes(buff)
//                out.write(base64Str)
//            }
//
//            Log.i(TAG, "fun2 = " + out.size())
//            out.toByteArray()
//        }
//    }

    private fun getByteFrom2(array: Array<FloatArray>): ByteArray {
        val arr = ByteArrayOutputStream().use { out ->
            array.forEach { floatArray ->
                val buff = getByteArrayFromFloatArray(floatArray)

                out.write(buff)
            }
            out.toByteArray()
        }

        return convertToBase64Bytes2(arr)
    }



//    private suspend fun createFile(neuroResult: NeuroResult) : ByteArray {
//        return withContext(Dispatchers.IO) {
//
//            val array = arrayOf(
//                neuroResult.keyPoints,
//                neuroResult.scores,
//                neuroResult.localDescriptors,
//                neuroResult.globalDescriptor
//            )
//
//            val version: Byte = 0x0
//            val indentificator: Byte = 0x0
//
//            val bA = byteArrayOf(version, indentificator)
//
//            val byteArray = ByteArrayOutputStream().use { stream ->
//                stream.write(bA)
//                for (arr in array) {
//                    val buff = getBuffer(arr)
//                    val base64Str = convertToBase64Bytes(buff)
//
//                    val count = base64Str.size
//
//                    val buffer = ByteBuffer.allocate(Int.SIZE_BYTES)
//                    buffer.putInt(count)
//
//                    Log.i(TAG, "c = " + buffer.array().fold("") { str, i -> "$str $i" })
//
//                    stream.write(buffer.array())
//                    stream.write(base64Str)
//                }
//                stream.toByteArray()
//            }
//
//            Log.i(TAG, "sb = ${byteArray.size}")
//            byteArray
//        }
//    }

//
//    private suspend fun createFile(neuroResult: NeuroResult) : ByteArray {
//        return withContext(Dispatchers.IO) {
//            var filedata = byteArrayOf()
//
//            val array = arrayOf(
//                neuroResult.keyPoints,
//                neuroResult.scores,
//                neuroResult.localDescriptors,
//                neuroResult.globalDescriptor
//            )
//
//            val version: Byte = 0x0
//            val indentificator: Byte = 0x0
//
//            filedata += version
//            filedata += indentificator
//
//            for (arr in array) {
//                val buff = getBuffer(arr)
//                val base64Str = convertToBase64Bytes(buff)
//               // val count = (base64Str?.size)?.toBigInteger()?.toByteArray()
//                //val count = buff.capacity()
//
//                val count = base64Str?.size ?: 0
//
//                val buffer = ByteBuffer.allocate(4)
//                buffer.order(ByteOrder.BIG_ENDIAN)
//                buffer.putInt(count)
//
//                filedata += buffer.array()
//                //filedata += count ?: byteArrayOf()
//                filedata += base64Str ?: byteArrayOf()
//
//                Log.i(TAG,  "buffff = " + buffer.array().fold("") {str, i -> "$str $i" })
//
//                //Log.i(TAG,  "buffff = " + count?.fold("") {str, i -> "$str $i" })
//
//                Log.i(TAG, "arr = " + base64Str?.size)
//
//            }
//
//            Log.i(TAG, "sb = ${filedata.size}")
//            filedata
//        }
//    }

//    private suspend fun createFile(neuroResult: NeuroResult) : ByteArray {
//        return withContext(Dispatchers.IO) {
//            var filedata = byteArrayOf()
//
//            val array = arrayOf(
//                neuroResult.keyPoints,
//                neuroResult.scores,
//                neuroResult.localDescriptors,
//                neuroResult.globalDescriptor
//            )
//
//            val version: Byte = 0x0
//            val indentificator: Byte = 0x0
//
//            filedata += version
//            filedata += indentificator
//
//            for (arr in array) {
//                val buff = getBuffer(arr)
//                val base64Str = convertToBase64Bytes(buff)
//                val count = base64Str?.size ?: 0
//                //val count = buff.capacity()
//
//                val buffer = ByteBuffer.allocate(Int.SIZE_BYTES)
//                buffer.order(ByteOrder.BIG_ENDIAN)
//                buffer.putInt(count)
//
//                filedata += buffer.array()
//                filedata += base64Str ?: byteArrayOf()
//
//                Log.i(TAG,  "buffff = " + buffer.array().fold("") {str, i -> "$str $i" })
//
//                Log.i(TAG, "arr = " + base64Str?.size)
//
//            }
//
//            Log.i(TAG, "sb = ${filedata.size}")
//            filedata
//        }
//    }


//    private suspend fun createFile(neuroResult: NeuroResult) : ByteArray {
//        return withContext(Dispatchers.IO) {
//            val filedata = ByteBuffer.allocate(574406)
//
//            val array = arrayOf(
//                neuroResult.keyPoints,
//                neuroResult.scores,
//                neuroResult.localDescriptors,
//                neuroResult.globalDescriptor
//            )
//
//            val version: Byte = 0x0
//            val indentificator: Byte = 0x0
//
//            filedata.put(version)
//            filedata.put(indentificator)
//
//            for (arr in array) {
//                val buff = getBuffer(arr)
//                val base64Str = convertToBase64Bytes(buff)
//                val count = buff.capacity()
//
//                Log.i(TAG, "capacity = $count")
//
//                filedata.putInt(count)
//                filedata.put(base64Str ?: byteArrayOf())
//
//                Log.i(TAG, "arr = " + base64Str?.size)
//
//            }
//
//            Log.i(TAG, "sb = ${filedata.array().size}")
//            filedata.array()
//        }
//    }

    private suspend fun saveFileExternalStorage(text: String): File {
        return withContext(Dispatchers.IO) {
            val file = File(vpsArFragment.requireContext().filesDir, "embedding.embd")
            file.appendText(text)
            file
        }
    }

//    private fun convertToBase64Bytes(floatArray: FloatArray): ByteArray? {
//        val buff: ByteBuffer = ByteBuffer.allocate( 4 * floatArray.size)
//        for (value in floatArray) {
//            buff.putFloat(value)
//        }
//
//        val array = Base64.encode(buff.array(), Base64.NO_WRAP)
//
//        Log.i(TAG, "array= ${buff.capacity()}")
//
//       return array
//    }

    private fun convertToBase64Bytes(buff: ByteBuffer): ByteArray {
        return Base64.encode(buff.array(), Base64.NO_WRAP)
    }

    private fun convertToBase64Bytes2(buff: ByteArray): ByteArray {
        return Base64.encode(buff, Base64.NO_WRAP)
    }

    private fun getBuffer(floatArray: FloatArray) : ByteBuffer {
        val buff: ByteBuffer = ByteBuffer.allocate(4 * floatArray.size)
        buff.order(ByteOrder.LITTLE_ENDIAN)
        for (value in floatArray) {
            buff.putFloat(value)
        }

        return buff
    }

    private fun getByteArrayFromFloatArray(floatArray: FloatArray) : ByteArray {
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