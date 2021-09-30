package com.arvrlab.vps_android_prototype.ui.select_mode

import androidx.lifecycle.ViewModel
import com.arvrlab.vps_android_prototype.R
import com.arvrlab.vps_android_prototype.data.SceneModel
import com.arvrlab.vps_android_prototype.util.BOOTCAMP_BASE_URL
import com.arvrlab.vps_android_prototype.util.BOOTCAMP_LOCATION_ID
import com.arvrlab.vps_android_prototype.util.POLYTECH_BASE_URL
import com.arvrlab.vps_android_prototype.util.POLYTECH_LOCATION_ID
import com.arvrlab.vps_sdk.data.LocalizationType

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
        sceneModel.intervalLocalizationMS = try {
            inteval.toLong()
        } catch (e: Exception) {
            6000L
        }
    }

    fun onOnlyForseChanged(onlyForce: Boolean) {
        sceneModel.onlyForce = onlyForce
    }

    fun onNeedLocationChanged(useGps: Boolean) {
        sceneModel.useGps = useGps
    }

    fun onUseNeuroChanged(useNeuro: Boolean) {
        sceneModel.localizationType = if (useNeuro)
            LocalizationType.MOBILE_VPS
        else
            LocalizationType.PHOTO
    }

    fun onImagesCountChanged(imagesCount: String) {
        sceneModel.imagesCount = try {
            imagesCount.toInt()
        } catch (e: Exception) {
            3
        }
    }

    fun onImagesIntervalChanged(intevalImages: String) {
        sceneModel.intervalImagesMS = try {
            intevalImages.toLong()
        } catch (e: Exception) {
            1000L
        }
    }

}