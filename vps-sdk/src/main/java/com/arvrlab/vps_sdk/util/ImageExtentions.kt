package com.arvrlab.vps_sdk.util

import android.graphics.*
import android.media.Image
import com.arvrlab.vps_sdk.neuro.NeuroHelper
import com.arvrlab.vps_sdk.neuro.NeuroModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

private const val QUALITY = 90

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

        val requestBody = byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull(), 0, byteArray.size)
        MultipartBody.Part.createFormData("embedding", "embedding.embd", requestBody)
    }
}