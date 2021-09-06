package com.arvrlab.vps_android_prototype.ui.scene

import androidx.lifecycle.ViewModel
import com.arvrlab.vps_android_prototype.util.SingleLiveEvent
import com.arvrlab.vps_sdk.network.dto.ResponseDto

class SceneViewModel : ViewModel() {

    val vpsError = SingleLiveEvent<java.lang.Exception>()
    val positionVps = SingleLiveEvent<ResponseDto>()

    fun onVpsErrorCallback(e: Exception) {
        vpsError.postValue(e)
    }

    fun onPositionVps(responseDto: ResponseDto) {
        positionVps.postValue(responseDto)
    }

}