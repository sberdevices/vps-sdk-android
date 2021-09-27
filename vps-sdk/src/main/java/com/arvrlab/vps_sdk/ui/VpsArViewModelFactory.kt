package com.arvrlab.vps_sdk.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

internal class VpsArViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        val vpsService = VpsService.newInstance(application)

        return VpsArViewModel(application, vpsService) as T
    }

}