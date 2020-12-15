package ru.arvrlab.vps.extentions

import android.graphics.*
import android.media.Image
import android.util.Log
import android.util.Size
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import ru.arvrlab.vps.network.dto.ResponseDto
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2

private const val BITMAP_WIDTH = 960
private const val BITMAP_HEIGHT = 540
private const val QUALITY = 90
private const val IMAGE_MEAN = 127.5f
private const val IMAGE_STD = 127.5f

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

suspend fun getResizedBitmapRotated(image: Image): Bitmap {
    return withContext(Dispatchers.IO) {
        val bytes = image.toByteArray()
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size).toBlackAndWhiteBitmap().rotate(90f)
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

//x y z => roll yaw pitch
fun Quaternion.toEulerAngles(): Vector3 {

    val test = x * y + z * w
    if (test > 0.499) { // singularity at north pole
        val y = 2 * atan2(x, w)
        val z = Math.PI / 2
        val x = 0f
        return Vector3(x, y, z.toFloat())
    }
    if (test < -0.499) { // singularity at south pole
        val y = -2 * atan2(x, w)
        val z = -Math.PI / 2;
        val x = 0f
        return Vector3(x, y, z.toFloat())
    }
    val sqx = x * x
    val sqy = y * y
    val sqz = z * z
    val y = atan2(2 * y * w - 2 * x * z, 1 - 2 * sqy - 2 * sqz);
    val z = asin(2 * test);
    val x = atan2(2 * x * w - 2 * y * z, 1 - 2 * sqx - 2 * sqz)

    return Vector3((x * 180 / PI).toFloat(), (y * 180 / PI).toFloat(), (z * 180 / PI).toFloat())
}

//fun Bitmap.toGrayScaledByteBuffer(batchSize: Int, inputImageWidth: Int, inputImageHeight: Int): ByteBuffer {
//    val byteBuffer = ByteBuffer.allocateDirect(batchSize * 4 * this.height * this.width * 3)
//    byteBuffer.order(ByteOrder.nativeOrder())
//
//    val pixels = IntArray(inputImageWidth * inputImageHeight)
//    this.getPixels(pixels, 0, this.width, 0, 0, this.width, this.height)
//
//    for (pixelValue in pixels) {
//        val r = (pixelValue shr 16 and 0xFF)
//        val g = (pixelValue shr 8 and 0xFF)
//        val b = (pixelValue and 0xFF)
//
//        // Convert RGB to grayscale and normalize pixel value to [0..1].
//        val normalizedPixelValue = (r + g + b) / 3.0f / 255.0f
//        byteBuffer.putFloat(normalizedPixelValue)
//    }
//
//    return byteBuffer
//}

fun convertBitmapToBuffer(image: Bitmap, inputImageWidth: Int, inputImageHeight: Int): ByteBuffer {
    val imageByteBuffer = ByteBuffer.allocateDirect(1 * inputImageWidth * inputImageHeight * 4)
    imageByteBuffer.rewind()

    val resizedImage = Bitmap.createScaledBitmap(image, inputImageWidth, inputImageHeight, false)

    resizedImage.apply {
        val pixels = IntArray(width * height)
        getPixels(pixels, 0, width, 0, 0, width, height)
        fillBuffer(imageByteBuffer, pixels, inputImageWidth, inputImageHeight, true)
    }

    return imageByteBuffer
}

private fun fillBuffer(
    imgData: ByteBuffer,
    pixels: IntArray,
    width: Int,
    height: Int,
    isModelQuantized: Boolean
) {

    for (i in 0 until height) {
        for (j in 0 until width) {
            val pixelValue = pixels[i * height + j]
            if (isModelQuantized) {
                imgData.put((pixelValue shr 16 and 0xFF).toByte())
                imgData.put((pixelValue shr 8 and 0xFF).toByte())
                imgData.put((pixelValue and 0xFF).toByte())
            } else {
                imgData.putFloat(((pixelValue shr 16 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                imgData.putFloat(((pixelValue shr 8 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                imgData.putFloat(((pixelValue and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
            }
        }
    }
}

fun Bitmap.rotate(degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}