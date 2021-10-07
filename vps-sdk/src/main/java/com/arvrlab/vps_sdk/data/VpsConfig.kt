package com.arvrlab.vps_sdk.data

data class VpsConfig(
    val vpsUrl: String,
    val locationID: String,
    var onlyForce: Boolean = true,
    val intervalLocalizationMS: Long = 6000,
    val useGps: Boolean = false,
    val localizationType: LocalizationType = Photo,
    val countImages: Int = 1,
    val intervalImagesMS: Long = 1000
)