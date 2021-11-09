package com.arvrlab.vps_android_prototype.data

import android.os.Parcelable
import androidx.annotation.RawRes
import com.arvrlab.vps_sdk.data.LocalizationType
import com.arvrlab.vps_sdk.data.MobileVps
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class SceneModel(
    var url: String,
    var locationID: String,
    @RawRes var modelRawId: Int,
    var onlyForce: Boolean = false,
    var intervalLocalizationMS: Long = 5000,
    var useGps: Boolean = false,
    var localizationType: @RawValue LocalizationType = MobileVps(),
    var useSerialImages: Boolean = true,
    var imagesCount: Int = 5,
    var intervalImagesMS: Long = 1000
) : Parcelable
