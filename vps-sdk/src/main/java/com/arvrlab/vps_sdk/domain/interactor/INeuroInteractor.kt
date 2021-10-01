package com.arvrlab.vps_sdk.domain.interactor

import android.graphics.Bitmap
import androidx.annotation.WorkerThread
import java.io.IOException

internal interface INeuroInteractor {

    @WorkerThread
    fun loadNeuroModel(url: String)

    @Throws(exceptionClasses = [IOException::class])
    fun codingBitmap(bitmap: Bitmap, dstWidth: Int, dstHeight: Int): ByteArray

    fun close()

}