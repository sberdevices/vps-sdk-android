package com.arvrlab.vps_sdk.data

data class VpsConfig(
    val url: String,
    val locationID: String,
    var onlyForce: Boolean = true,
    val intervalLocalizationMS: Long = 6000,
    val useGps: Boolean = false,
    val useNeuro: Boolean = false,
    val countImages: Int = 1,
    val intervalImagesMS: Long = 1000
)