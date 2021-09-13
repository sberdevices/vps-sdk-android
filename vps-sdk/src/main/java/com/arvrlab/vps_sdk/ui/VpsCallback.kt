package com.arvrlab.vps_sdk.ui

interface VpsCallback {
    fun onPositionVps()
    fun onError(error: Exception)
}