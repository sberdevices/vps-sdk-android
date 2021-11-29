package com.arvrlab.vps_sdk.domain.interactor

import android.graphics.Bitmap
import androidx.annotation.WorkerThread
import com.arvrlab.vps_sdk.data.MobileVps
import java.io.IOException

internal interface INeuroInteractor {

    @WorkerThread
    suspend fun loadNeuroModel(mobileVps: MobileVps)

    @Throws(exceptionClasses = [IOException::class])
    suspend fun codingBitmap(bitmap: Bitmap, dstWidth: Int, dstHeight: Int): ByteArray

    fun close()

}