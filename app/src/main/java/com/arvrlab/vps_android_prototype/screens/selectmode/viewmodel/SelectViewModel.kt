package com.arvrlab.vps_android_prototype.screens.selectmode.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.arvrlab.vps_android_prototype.infrastructure.utils.BOOTCAMP_BASE_URL
import com.arvrlab.vps_android_prototype.infrastructure.utils.BOOTCAMP_LOCATION_ID
import com.arvrlab.vps_android_prototype.infrastructure.utils.POLYTECH_BASE_URL
import com.arvrlab.vps_android_prototype.infrastructure.utils.POLYTECH_LOCATION_ID
import com.arvrlab.vps_sdk.service.Settings

class SelectViewModel(app: Application) : AndroidViewModel(app) {

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