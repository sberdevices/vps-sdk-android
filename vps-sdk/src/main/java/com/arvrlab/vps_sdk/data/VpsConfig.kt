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
    val intervalImagesMS: Long = 1000,
    val worldInterpolationDurationMS: Long = 500,
    val worldInterpolationDistanceLimit: Float = 2f,
    val worldInterpolationAngleLimit: Float = 10f
) {
    companion object {

        fun getIndoorConfig(vpsUrl: String, locationID: String): VpsConfig =
            VpsConfig(
                vpsUrl = vpsUrl,
                locationID = locationID,
                onlyForce = false,
                useGps = false,
                useSerialImages = true
            )

        fun getOutdoorConfig(vpsUrl: String, locationID: String): VpsConfig =
            VpsConfig(
                vpsUrl = vpsUrl,
                locationID = locationID,
                onlyForce = true,
                useGps = true,
                useSerialImages = false
            )

    }
}