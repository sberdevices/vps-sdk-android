package com.arvrlab.vps_sdk.util

import android.graphics.*

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