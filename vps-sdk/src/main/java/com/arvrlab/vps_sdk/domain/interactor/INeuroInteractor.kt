package com.arvrlab.vps_sdk.domain.interactor

import android.graphics.Bitmap
import com.arvrlab.vps_sdk.domain.model.NeuroModel
import java.io.IOException

internal interface INeuroInteractor {

    fun codingBitmap(bitmap: Bitmap, dstWidth: Int, dstHeight: Int): NeuroModel

    @Throws(exceptionClasses = [IOException::class])
    fun convertToByteArray(neuroModel: NeuroModel): ByteArray
    @Throws(exceptionClasses = [IOException::class])
    fun convertToByteArray(bitmap: Bitmap, dstWidth: Int, dstHeight: Int): ByteArray

    fun close()

}