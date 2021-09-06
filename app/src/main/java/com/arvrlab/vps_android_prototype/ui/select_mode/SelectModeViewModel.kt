package com.arvrlab.vps_android_prototype.ui.select_mode

import androidx.lifecycle.ViewModel
import com.arvrlab.vps_android_prototype.util.BOOTCAMP_BASE_URL
import com.arvrlab.vps_android_prototype.util.BOOTCAMP_LOCATION_ID
import com.arvrlab.vps_android_prototype.util.POLYTECH_BASE_URL
import com.arvrlab.vps_android_prototype.util.POLYTECH_LOCATION_ID
import com.arvrlab.vps_sdk.service.Settings

class SelectModeViewModel : ViewModel() {

    val settings = Settings(
        url = BOOTCAMP_BASE_URL,
        locationID = BOOTCAMP_LOCATION_ID,
        onlyForce = true,
        timerInterval = 6000,
        needLocation = false,
        isNeuro = false
    )

    fun onPolytechSelected() {
        settings.locationID = POLYTECH_LOCATION_ID
        settings.url = POLYTECH_BASE_URL
    }

    fun onBootcampSelected() {
        settings.locationID = BOOTCAMP_LOCATION_ID
        settings.url = BOOTCAMP_BASE_URL
    }

    fun onIntervalChanged(inteval: String) {
        try {
            settings.timerInterval = inteval.toLong()
        } catch (e: Exception) {
            settings.timerInterval = 6000
        }
    }

    fun onOnlyForseChanged(onlyForce: Boolean) {
        settings.onlyForce = onlyForce
    }

    fun onNeedLocationChanged(isNeedLocation: Boolean) {
        settings.needLocation = isNeedLocation
    }

    fun onUseNeuroChanged(isNeedNeuro: Boolean) {
        settings.isNeuro = isNeedNeuro
    }

}