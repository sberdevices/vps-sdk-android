package com.arvrlab.vps_sdk.data

data class VpsConfig(
    val vpsUrl: String,
    val locationID: String,
    var onlyForce: Boolean = false,
    val intervalLocalizationMS: Long = 5000,
    val useGps: Boolean = false,
    val localizationType: LocalizationType = MobileVps(),
    val useSerialImages: Boolean = true,
    val countImages: Int = 5,
    val intervalImagesMS: Long = 1000
)