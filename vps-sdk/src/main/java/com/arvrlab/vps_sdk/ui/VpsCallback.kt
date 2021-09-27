package com.arvrlab.vps_sdk.ui

interface VpsCallback {
    fun onSuccess()
    fun onStateChange(isEnable: Boolean)
    fun onError(error: Throwable)
}