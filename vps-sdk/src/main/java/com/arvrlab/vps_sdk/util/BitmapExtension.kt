package com.arvrlab.vps_sdk.util

import android.graphics.*

private const val RATIO_16_9 = 16f / 9f

fun Bitmap.toGrayscale(): Bitmap {
    val result = Bitmap.createBitmap(width, height, config)
    val colorMatrix = ColorMatrix()
        .apply { setSaturation(0f) }
    val paint = Paint()
        .apply { colorFilter = ColorMatrixColorFilter(colorMatrix) }
    Canvas(result)
        .drawBitmap(this, 0f, 0f, paint)
    return result
}

fun Bitmap.cropTo16x9(): Bitmap {
    var newWidth: Int = width
    var newHeight: Int

    newHeight = (width / RATIO_16_9).toInt()
    if (newHeight == height) return this

    if (newHeight > height) {
        newHeight = height
        newWidth = (newHeight * RATIO_16_9).toInt()
    }

    return Bitmap.createBitmap(
        this,
        (width - newWidth) / 2,
        (height - newHeight) / 2,
        newWidth,
        newHeight
    )
}