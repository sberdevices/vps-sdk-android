package lab.ar.network

import android.graphics.Bitmap
import android.media.Image
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lab.ar.extentions.getResizedBitmap
import lab.ar.network.dto.RequestDto
import lab.ar.network.dto.ResponseDto
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.lang.Exception

object NetworkHelper {

    suspend fun takePhotoAndSendRequestToServer(
        view: ArSceneView,
        jsonToSend: RequestDto,
        url: String? = null
    ): Pair<Quaternion, Vector3> {
        return withContext(Dispatchers.IO) {
            var image: Image? = null
            try {
                image = view.arFrame?.acquireCameraImage() ?: throw Exception("Error while acquiring camera image")
                val resizedBitmap = getResizedBitmap(image)
                val imageMultipart = getImageMultipart(resizedBitmap)

                sendRequestToServer(imageMultipart, jsonToSend, url)
            } finally {
                image?.close()
            }
        }
    }

    private fun getImageMultipart(bitmap: Bitmap): MultipartBody.Part {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val byteArray = stream.toByteArray()
        bitmap.recycle()

        val requestBody = byteArray.toRequestBody("*/*".toMediaTypeOrNull(), 0, byteArray.size)
        return MultipartBody.Part.createFormData("image", "sceneform_photo", requestBody)
    }

    /**
     * can throw network exceptions
     */
    private suspend fun sendRequestToServer(
        imageMultipart: MultipartBody.Part,
        jsonToSend: RequestDto,
        url: String?
    ): Pair<Quaternion, Vector3> {
        return withContext(Dispatchers.IO) {
            val deferredResponse = RestApi.getApiService(url).process(jsonToSend, imageMultipart)
             extractResponseData(deferredResponse.await())
        }
    }

    private fun extractResponseData(response: ResponseDto): Pair<Quaternion, Vector3> {
        val coordinateData =
            response.responseData?.responseAttributes?.responseLocation?.responseRelative ?: throw Exception("Fail")

        val yaw = coordinateData.yaw ?: 0f
        val x = 0 - (coordinateData.x ?: 0f)
        val y = 0 - (coordinateData.y ?: 0f)
        val z = 0 - (coordinateData.z ?: 0f)

        val newPosition = Vector3(x, y, z)

        val newRotation = if (yaw > 0) {
            Quaternion(Vector3(0f, 180f - yaw, 0f)).inverted()
        } else {
            Quaternion(Vector3(0f, yaw, 0f)).inverted()
        }

        return Pair(newRotation, newPosition)
    }

}