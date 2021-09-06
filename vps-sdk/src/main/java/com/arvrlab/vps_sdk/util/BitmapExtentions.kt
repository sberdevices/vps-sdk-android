package com.arvrlab.vps_sdk.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.media.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val BITMAP_WIDTH = 960
private const val BITMAP_HEIGHT = 540
private const val FLOAT_SIZE = 4

suspend fun getResizedBitmap(image: Image): Bitmap {
    return withContext(Dispatchers.IO) {
        val bytes = image.toByteArrayServerVersion()
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size).toBlackAndWhiteBitmap()
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, BITMAP_WIDTH, BITMAP_HEIGHT, false)

        bitmap.recycle()

        scaledBitmap
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
