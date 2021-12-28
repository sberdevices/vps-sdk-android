package com.arvrlab.vps_sdk.util

import android.graphics.Color
import androidx.annotation.ColorInt

object ColorUtil {

    /**
     * Values from android.graphics.ColorMatrix for gray-scale
     */
    private const val SATURATION_RED = 0.213f
    private const val SATURATION_GREEN = 0.715f
    private const val SATURATION_BLUE = 0.072f

    fun gray(@ColorInt color: Int): Int {
        val pixelRed = Color.red(color) * SATURATION_RED
        val pixelGreen = Color.green(color) * SATURATION_GREEN
        val pixelBlue = Color.blue(color) * SATURATION_BLUE
        return (pixelRed + pixelGreen + pixelBlue).toInt()
    }

}