package com.arvrlab.vps_android_prototype.data

import android.os.Parcelable
import androidx.annotation.RawRes
import com.arvrlab.vps_sdk.data.LocalizationType
import com.arvrlab.vps_sdk.data.Photo
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class SceneModel(
    var url: String,
    var locationID: String,
    @RawRes var modelRawId: Int,
    var onlyForce: Boolean = true,
    var intervalLocalizationMS: Long = 6000,
    var useGps: Boolean = false,
    var localizationType: @RawValue LocalizationType = Photo,
    var imagesCount: Int = 1,
    var intervalImagesMS: Long = 0
) : Parcelable
