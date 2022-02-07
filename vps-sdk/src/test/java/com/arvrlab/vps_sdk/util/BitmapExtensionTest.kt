package com.arvrlab.vps_sdk.util

import android.graphics.Bitmap
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BitmapExtensionTest {

    private companion object {
        const val RATIO_16_9 = Constant.BITMAP_WIDTH.toFloat() / Constant.BITMAP_HEIGHT.toFloat()
    }

    @Test
    fun checkCropTo9x16() {
        val bitmapList = listOf(
            Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888),
            Bitmap.createBitmap(1024, 768, Bitmap.Config.ARGB_8888),
            Bitmap.createBitmap(768, 1024, Bitmap.Config.ARGB_8888),
            Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888),
            Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888),
            Bitmap.createBitmap(1920, 1200, Bitmap.Config.ARGB_8888),
            Bitmap.createBitmap(1200, 1920, Bitmap.Config.ARGB_8888),
        )

        bitmapList.forEach { bitmap ->
            val cropedBitmap = bitmap.cropTo16x9()
            Assert.assertTrue(
                "bitmap ${bitmap.sizeToString()} aspectRatio!=${RATIO_16_9}",
                checkRatio(cropedBitmap)
            )
        }
    }

    private fun checkRatio(bitmap: Bitmap): Boolean {
        val currentRatio = bitmap.width.toFloat() / bitmap.height.toFloat()

        if (currentRatio == RATIO_16_9) return true

        return if (currentRatio > RATIO_16_9) {
            val newRatio = bitmap.width.toFloat() / (bitmap.height + 1).toFloat()
            RATIO_16_9 in (newRatio..currentRatio)
        } else {
            val newRatio = bitmap.width.toFloat() / (bitmap.height - 1).toFloat()
            RATIO_16_9 in (currentRatio..newRatio)
        }
    }

    private fun Bitmap.sizeToString(): String =
        "$width:$height"

}