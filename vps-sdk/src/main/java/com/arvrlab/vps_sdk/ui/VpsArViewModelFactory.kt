package com.arvrlab.vps_sdk.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.arvrlab.vps_sdk.data.repository.IVpsRepository
import com.arvrlab.vps_sdk.data.repository.VpsRepository
import com.arvrlab.vps_sdk.domain.interactor.ArInteractor
import com.arvrlab.vps_sdk.domain.interactor.IArInteractor
import com.arvrlab.vps_sdk.domain.interactor.IVpsInteractor
import com.arvrlab.vps_sdk.domain.interactor.VpsInteractor
import com.google.ar.sceneform.ArSceneView

class VpsArViewModelFactory(
    private val application: Application,
    private val arSceneViewLazy: Lazy<ArSceneView>
) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        val arInteractor: IArInteractor = ArInteractor(arSceneViewLazy)
        val vpsRepository: IVpsRepository = VpsRepository(application)
        val vpsInteractor: IVpsInteractor = VpsInteractor(application, arInteractor, vpsRepository)
        return VpsArViewModel(application, vpsInteractor, arInteractor) as T
    }

}