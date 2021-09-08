package com.arvrlab.vps_sdk.service

import com.arvrlab.vps_sdk.network.dto.ResponseDto

interface VpsCallback {
    fun onPositionVps(responseDto: ResponseDto)
    fun onError(error: Exception)
}