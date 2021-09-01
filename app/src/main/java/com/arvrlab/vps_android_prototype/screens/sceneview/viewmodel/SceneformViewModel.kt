package com.arvrlab.vps_android_prototype.screens.sceneview.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.arvrlab.vps_android_prototype.infrastructure.utils.SingleLiveEvent
import com.arvrlab.vps_sdk.network.dto.ResponseDto

class SceneformViewModel(app: Application) : AndroidViewModel(app) {

    val vpsError = SingleLiveEvent<java.lang.Exception>()
    val positionVps = SingleLiveEvent<ResponseDto>()

    fun onVpsErrorCallback(e: Exception) {
        vpsError.postValue(e)
    }

    fun onPositionVps(responseDto: ResponseDto) {
        positionVps.postValue(responseDto)
    }

}