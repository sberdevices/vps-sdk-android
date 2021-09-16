package com.arvrlab.vps_sdk.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.arvrlab.vps_sdk.data.api.IVpsApiManager
import com.arvrlab.vps_sdk.data.api.VpsApiManager
import com.arvrlab.vps_sdk.data.repository.IVpsRepository
import com.arvrlab.vps_sdk.data.repository.VpsRepository
import com.arvrlab.vps_sdk.domain.interactor.INeuroInteractor
import com.arvrlab.vps_sdk.domain.interactor.IVpsInteractor
import com.arvrlab.vps_sdk.domain.interactor.NeuroInteractor
import com.arvrlab.vps_sdk.domain.interactor.VpsInteractor

class VpsArViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        val arController = ArManager()
        val neuroInteractor: INeuroInteractor = NeuroInteractor(application)
        val vpsApiManager: IVpsApiManager = VpsApiManager()
        val vpsRepository: IVpsRepository = VpsRepository(vpsApiManager)
        val vpsInteractor: IVpsInteractor = VpsInteractor(vpsRepository, neuroInteractor)
        return VpsArViewModel(application, vpsInteractor, arController) as T
    }

}