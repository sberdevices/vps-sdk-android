package lab.ar.network

import android.graphics.Bitmap
import android.media.Image
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lab.ar.extentions.getResizedBitmap
import lab.ar.extentions.toNewRotationAndPositionPair
import lab.ar.network.dto.RequestDto
import lab.ar.vps.VpsCallback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

class NetworkHelper(private val url: String,
                    private val callback: VpsCallback? = null) {

    suspend fun takePhotoAndSendRequestToServer(
        view: ArSceneView,
        jsonToSend: RequestDto
    ): Pair<Quaternion, Vector3>? {
        return withContext(Dispatchers.Main) {
            var image: Image? = null
            try {
                image = view.arFrame?.acquireCameraImage() ?: throw Exception("Failed to acquire camera image")
                sendRequestToServer(getImageMultipart(image), jsonToSend)
            } catch (e: Exception) {
                callback?.onError(e)
                null
            } finally {
                image?.close()
            }
        }
    }

    private suspend fun getImageMultipart(image: Image): MultipartBody.Part {
        return withContext(Dispatchers.IO) {
            val resizedBitmap = getResizedBitmap(image)

            val byteArray = ByteArrayOutputStream().use { stream ->
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                stream.toByteArray()
            }

            resizedBitmap.recycle()

            val requestBody = byteArray.toRequestBody("*/*".toMediaTypeOrNull(), 0, byteArray.size)
            MultipartBody.Part.createFormData("image", "sceneform_photo", requestBody)
        }
    }

    private suspend fun sendRequestToServer(
        imageMultipart: MultipartBody.Part,
        jsonToSend: RequestDto
    ): Pair<Quaternion, Vector3> {
        return withContext(Dispatchers.IO) {
            val deferredResponse = RestApi.getApiService(url).process(jsonToSend, imageMultipart)
            val responseDto = deferredResponse.await()
            callback?.onPositionVps(responseDto)
            responseDto.toNewRotationAndPositionPair()
        }
    }
}