package com.arvrlab.vps_sdk.ui

import com.arvrlab.vps_sdk.ui.VpsService.State

interface VpsCallback {
    fun onSuccess()
    fun onFail()
    fun onStateChange(state: State)
    fun onError(error: Throwable)
}