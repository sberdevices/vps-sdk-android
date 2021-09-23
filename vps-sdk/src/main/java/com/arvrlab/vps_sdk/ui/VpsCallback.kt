package com.arvrlab.vps_sdk.ui

interface VpsCallback {
    fun onSuccess()
    fun onError(error: Throwable)
}