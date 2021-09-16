package com.arvrlab.vps_sdk.domain.interactor

import android.graphics.Bitmap
import java.io.IOException

internal interface INeuroInteractor {

    @Throws(exceptionClasses = [IOException::class])
    fun codingBitmap(bitmap: Bitmap, dstWidth: Int, dstHeight: Int): ByteArray

    fun close()

}