package com.arvrlab.vps_sdk.extentions

import android.graphics.*
import android.media.Image
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import com.arvrlab.vps_sdk.network.dto.ResponseDto
import com.arvrlab.vps_sdk.neuro.NeuroHelper
import com.arvrlab.vps_sdk.neuro.NeuroModel
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2

private const val BITMAP_WIDTH = 960
private const val BITMAP_HEIGHT = 540
private const val QUALITY = 90
private const val FLOAT_SIZE = 4

fun Image.toByteArrayNeuroVersion(): ByteArray {
    return ByteArrayOutputStream().use { out ->

        val yBuffer = this.planes[0].buffer
        val ySize = yBuffer.remaining()
        val nv21 = ByteArray(ySize)

        yBuffer.get(nv21, 0, ySize)

        val yuv = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
        yuv.compressToJpeg(Rect(0, 0, this.width, this.height), QUALITY, out)
        out.toByteArray()
    }
}

suspend fun getResizedBitmap(image: Image): Bitmap {
    return withContext(Dispatchers.IO) {
        val bytes = image.toByteArrayServerVersion()
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size).toBlackAndWhiteBitmap()
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, BITMAP_WIDTH, BITMAP_HEIGHT, false)

        bitmap.recycle()

        scaledBitmap
    }
}

fun Image.toByteArrayServerVersion(): ByteArray {
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
    return ByteArrayOutputStream().use { out ->
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), QUALITY, out)
        out.toByteArray()
    }
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

suspend fun Image.toMultipartBodyServer(): MultipartBody.Part {
    return withContext(Dispatchers.IO) {
        val resizedBitmap = getResizedBitmap(this@toMultipartBodyServer)

        val byteArray = ByteArrayOutputStream().use { stream ->
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, stream)
            stream.toByteArray()
        }

        resizedBitmap.recycle()

        val requestBody = byteArray.toRequestBody("*/*".toMediaTypeOrNull(), 0, byteArray.size)
        MultipartBody.Part.createFormData("image", "sceneform_photo", requestBody)
    }
}

suspend fun Image.toMultipartBodyNeuro(neuro: NeuroModel): MultipartBody.Part {
    return withContext(Dispatchers.IO) {

        val imageInByteArray = toByteArrayNeuroVersion()
        val bitmap = BitmapFactory.decodeByteArray(imageInByteArray, 0, imageInByteArray.size)

        val neuroResult = neuro.getFeatures(bitmap)
        val byteArray = NeuroHelper.getFileAsByteArray(neuroResult)

        bitmap.recycle()

        val requestBody =
            byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull(), 0, byteArray.size)
        MultipartBody.Part.createFormData("embedding", "embedding.embd", requestBody)
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

fun convertBitmapToBuffer(bitmap: Bitmap): ByteBuffer {
    val imageByteBuffer = ByteBuffer
        .allocateDirect(1 * BITMAP_WIDTH * BITMAP_HEIGHT * FLOAT_SIZE)
        .order(ByteOrder.nativeOrder())
    imageByteBuffer.rewind()

    val resizedBitmap = getPreProcessedBitmap(90f, bitmap, BITMAP_WIDTH, BITMAP_HEIGHT)
    bitmap.recycle()

    fillBuffer(resizedBitmap, imageByteBuffer)

    return imageByteBuffer
}

fun getPreProcessedBitmap(
    degrees: Float,
    src: Bitmap, dstWidth: Int, dstHeight: Int
): Bitmap {
    val matrix = Matrix()
    val width = src.width
    val height = src.height

    if (width != dstWidth || height != dstHeight) {
        val sx = dstWidth / width.toFloat()
        val sy = dstHeight / height.toFloat()
        matrix.setScale(sx, sy)
    }
    matrix.postRotate(degrees)

    return Bitmap.createBitmap(src, 0, 0, width, height, matrix, true)
}

private fun fillBuffer(bitmap: Bitmap, imgData: ByteBuffer) {
    for (y in 0 until bitmap.height) {
        for (x in 0 until bitmap.width) {
            val pixel = Color.green(bitmap.getPixel(x, y))
            imgData.putFloat(pixel.toFloat())
        }
    }

    bitmap.recycle()
}

fun Int.toByteArray(): ByteArray = byteArrayOf(
    (this ushr 24).toByte(),
    (this ushr 16).toByte(),
    (this ushr 8).toByte(),
    this.toByte()
)

fun com.google.ar.sceneform.math.Matrix.toPositionVector(): Vector3 {
    val m31: Float = data[13]
    val m32: Float = data[14]
    val m33: Float = data[15]

    return Vector3(m31, m32, m33)
}
