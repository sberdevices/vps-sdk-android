package com.arvrlab.vps_sdk.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class LocalizationType : Parcelable

@Parcelize
object Photo : LocalizationType()

@Parcelize
data class MobileVps(
    val mnvNeuroUrl: String = MNV_960X540X1_4096,
    val mspNeuroUrl: String = MSP_960X540X1_256_400
) : LocalizationType() {

    private companion object {
        const val MNV_960X540X1_4096 =
            "https://testable1.s3pd01.sbercloud.ru/mobilevpstflite/mnv_960x540x1_4096.tflite"
        const val MSP_960X540X1_256_400 =
            "https://testable1.s3pd01.sbercloud.ru/mobilevpstflite/msp_960x540x1_256_400.tflite"
    }

}