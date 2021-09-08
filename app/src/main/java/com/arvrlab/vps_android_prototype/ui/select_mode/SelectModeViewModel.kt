package com.arvrlab.vps_android_prototype.ui.select_mode

import androidx.lifecycle.ViewModel
import com.arvrlab.vps_android_prototype.R
import com.arvrlab.vps_android_prototype.data.SceneModel
import com.arvrlab.vps_android_prototype.util.BOOTCAMP_BASE_URL
import com.arvrlab.vps_android_prototype.util.BOOTCAMP_LOCATION_ID
import com.arvrlab.vps_android_prototype.util.POLYTECH_BASE_URL
import com.arvrlab.vps_android_prototype.util.POLYTECH_LOCATION_ID

class SelectModeViewModel : ViewModel() {

    val sceneModel = SceneModel(
        url = BOOTCAMP_BASE_URL,
        locationID = BOOTCAMP_LOCATION_ID,
        modelRawId = R.raw.bootcamp
    )

    fun onPolytechSelected() {
        with(sceneModel) {
            url = POLYTECH_BASE_URL
            locationID = POLYTECH_LOCATION_ID
            modelRawId = R.raw.polytech
        }
    }

    fun onBootcampSelected() {
        with(sceneModel) {
            url = BOOTCAMP_BASE_URL
            locationID = BOOTCAMP_LOCATION_ID
            modelRawId = R.raw.bootcamp
        }
    }

    fun onIntervalChanged(inteval: String) {
        sceneModel.timerInterval = try {
            inteval.toLong()
        } catch (e: Exception) {
            6000L
        }
    }

    fun onOnlyForseChanged(onlyForce: Boolean) {
        sceneModel.onlyForce = onlyForce
    }

    fun onNeedLocationChanged(isNeedLocation: Boolean) {
        sceneModel.needLocation = isNeedLocation
    }

    fun onUseNeuroChanged(isNeedNeuro: Boolean) {
        sceneModel.isNeuro = isNeedNeuro
    }

}