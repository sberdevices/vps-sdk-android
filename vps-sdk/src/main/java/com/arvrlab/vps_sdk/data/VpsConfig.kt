package com.arvrlab.vps_sdk.data

data class VpsConfig(
    val url: String,
    val locationID: String,
    var onlyForce: Boolean = true,
    val timerInterval: Long = 6000,
    val needLocation: Boolean = false,
    val isNeuro: Boolean = false
)