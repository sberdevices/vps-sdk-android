package com.arvrlab.vps_sdk.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class LocalizationType : Parcelable

@Parcelize
object Photo : LocalizationType()

@Parcelize
data class MobileVps(val neuroModelUrl: String = DEFAULT_HFNET_I8_960) : LocalizationType() {

    private companion object {
        const val DEFAULT_HFNET_I8_960 =
            "https://testable1.s3pd01.sbercloud.ru/vpsmobiletflite/230421/hfnet_i8_960.tflite"
    }

}