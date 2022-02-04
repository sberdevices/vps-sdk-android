package com.arvrlab.vps_sdk.util

import android.graphics.*

private const val RATIO_9_16 = 9f / 16f

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

fun Bitmap.cropTo9x16(): Bitmap {
    var newHeight: Int = height
    var newWidth: Int = width
    if (width < height) {
        newHeight = (width / RATIO_9_16).toInt()
        if (newHeight == height) return this

        if (newHeight > height) {
            newHeight = height
            newWidth = (newHeight * RATIO_9_16).toInt()
        }
    } else {
        newWidth = (height / RATIO_9_16).toInt()
        if (newWidth == width) return this

        if (newWidth > width) {
            newWidth = width
            newHeight = (newWidth * RATIO_9_16).toInt()
        }
    }

    return Bitmap.createBitmap(
        this,
        (width - newWidth) / 2,
        (height - newHeight) / 2,
        newWidth,
        newHeight
    )
}