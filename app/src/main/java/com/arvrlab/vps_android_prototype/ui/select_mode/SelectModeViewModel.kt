package com.arvrlab.vps_android_prototype.ui.select_mode

import androidx.lifecycle.ViewModel
import com.arvrlab.vps_android_prototype.R
import com.arvrlab.vps_android_prototype.data.SceneModel
import com.arvrlab.vps_android_prototype.util.*
import com.arvrlab.vps_sdk.data.MobileVps
import com.arvrlab.vps_sdk.data.Photo

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
            5000L
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
            MobileVps()
        else
            Photo
    }

    fun onUseSerialImagesChanged(useSerialImages: Boolean) {
        sceneModel.useSerialImages = useSerialImages
    }

    fun onImagesCountChanged(imagesCount: String) {
        sceneModel.imagesCount = try {
            imagesCount.toInt()
        } catch (e: Exception) {
            Logger.error(e)
            1
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