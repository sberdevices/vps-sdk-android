package com.arvrlab.vps_android_prototype.data

import android.os.Parcelable
import androidx.annotation.RawRes
import kotlinx.parcelize.Parcelize

@Parcelize
data class SceneModel(
    var url: String,
    var locationID: String,
    @RawRes var modelRawId: Int,
    var onlyForce: Boolean = true,
    var intervalLocalizationMS: Long = 6000,
    var useGps: Boolean = false,
    var useNeuro: Boolean = false,
    var imagesCount: Int = 1,
    var intervalImagesMS: Long = 0
) : Parcelable
