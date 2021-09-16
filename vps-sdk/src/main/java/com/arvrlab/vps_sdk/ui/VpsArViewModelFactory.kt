package com.arvrlab.vps_sdk.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.arvrlab.vps_sdk.data.api.IVpsApiManager
import com.arvrlab.vps_sdk.data.api.VpsApiManager
import com.arvrlab.vps_sdk.data.repository.IVpsRepository
import com.arvrlab.vps_sdk.data.repository.VpsRepository
import com.arvrlab.vps_sdk.domain.interactor.*
import com.google.ar.sceneform.ArSceneView

class VpsArViewModelFactory(
    private val application: Application,
    private val arSceneViewLazy: Lazy<ArSceneView>
) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        val arInteractor: IArInteractor = ArInteractor(arSceneViewLazy)
        val neuroInteractor: INeuroInteractor = NeuroInteractor(application)
        val vpsApiManager: IVpsApiManager = VpsApiManager()
        val vpsRepository: IVpsRepository = VpsRepository(vpsApiManager)
        val vpsInteractor: IVpsInteractor = VpsInteractor(application, arInteractor, vpsRepository, neuroInteractor)
        return VpsArViewModel(application, vpsInteractor, arInteractor) as T
    }

}