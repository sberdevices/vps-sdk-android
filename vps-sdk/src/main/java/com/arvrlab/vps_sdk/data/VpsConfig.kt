package com.arvrlab.vps_sdk.data

import androidx.annotation.RawRes

data class VpsConfig(
    val url: String,
    val locationID: String,
    @RawRes val modelRawId: Int,
    var onlyForce: Boolean = true,
    val timerInterval: Long = 6000,
    val needLocation: Boolean = false,
    val isNeuro: Boolean = false
)