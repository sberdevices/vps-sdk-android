package lab.ar.extentions

import android.graphics.*
import android.media.Image
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.EngineInstance
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lab.ar.network.dto.ResponseDto
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

private const val BITMAP_WIDTH = 960
private const val BITMAP_HEIGHT = 540
private const val QUALITY = 100

fun Image.toByteArray(): ByteArray {
    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), QUALITY, out)
    return out.toByteArray()
}

fun Bitmap.toBlackAndWhiteBitmap(): Bitmap {
    val blackAndWhieBitmap = Bitmap.createBitmap(
        this.width, this.height, this.config
    )
    for (x in 0 until this.width) {
        for (y in 0 until this.height) {
            val pixelColor = this.getPixel(x, y)
            val pixelAlpha: Int = Color.alpha(pixelColor)
            val pixelRed: Int = Color.red(pixelColor)
            val pixelGreen: Int = Color.green(pixelColor)
            val pixelBlue: Int = Color.blue(pixelColor)
            val pixelBW = (pixelRed + pixelGreen + pixelBlue) / 3
            val newPixel: Int = Color.argb(pixelAlpha, pixelBW, pixelBW, pixelBW)
            blackAndWhieBitmap.setPixel(x, y, newPixel)
        }
    }
    return blackAndWhieBitmap
}

suspend fun getResizedBitmap(image: Image): Bitmap {
    return withContext(Dispatchers.IO) {
        val bytes = image.toByteArray()
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size).toBlackAndWhiteBitmap()
        Bitmap.createScaledBitmap(bitmap, BITMAP_WIDTH, BITMAP_HEIGHT, false)
    }
}

fun getConvertedCameraStartRotation(cameraRotation: Quaternion): Quaternion {
    val dir = Quaternion.rotateVector(cameraRotation, Vector3(0f, 0f, 1f))
    dir.y = 0f
    return Quaternion.rotationBetweenVectors(Vector3(0f, 0f, 1f), dir)
}

fun ResponseDto.toNewRotationAndPositionPair(): Pair<Quaternion, Vector3> {
    val coordinateData = responseData?.responseAttributes?.responseLocation?.responseRelative
            ?: throw IllegalArgumentException("Failed to convert ResponseDto to new position and rotation, ResponseDto is null")

    val yaw = coordinateData.yaw ?: 0f
    val x = -(coordinateData.x ?: 0f)
    val y = -(coordinateData.y ?: 0f)
    val z = -(coordinateData.z ?: 0f)

    val newPosition = Vector3(x, y, z)

    val newRotation = if (yaw > 0) {
        Quaternion(Vector3(0f, 180f - yaw, 0f)).inverted()
    } else {
        Quaternion(Vector3(0f, yaw, 0f)).inverted()
    }

    return Pair(newRotation, newPosition)
}

suspend fun Image.toMultipartBody(): MultipartBody.Part {
    return withContext(Dispatchers.IO) {
        val resizedBitmap = getResizedBitmap(this@toMultipartBody)

        val byteArray = ByteArrayOutputStream().use { stream ->
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, stream)
            stream.toByteArray()
        }

        resizedBitmap.recycle()

        val requestBody = byteArray.toRequestBody("*/*".toMediaTypeOrNull(), 0, byteArray.size)
        MultipartBody.Part.createFormData("image", "sceneform_photo", requestBody)
    }
}