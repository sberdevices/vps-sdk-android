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
    var timerInterval: Long = 6000,
    var needLocation: Boolean = false,
    var isNeuro: Boolean = false
) : Parcelable
