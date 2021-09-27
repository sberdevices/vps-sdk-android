package com.arvrlab.vps_sdk.ui

import android.content.Context
import android.location.LocationManager
import com.arvrlab.vps_sdk.data.VpsConfig
import com.arvrlab.vps_sdk.data.api.IVpsApiManager
import com.arvrlab.vps_sdk.data.api.VpsApiManager
import com.arvrlab.vps_sdk.data.repository.IVpsRepository
import com.arvrlab.vps_sdk.data.repository.VpsRepository
import com.arvrlab.vps_sdk.domain.interactor.INeuroInteractor
import com.arvrlab.vps_sdk.domain.interactor.IVpsInteractor
import com.arvrlab.vps_sdk.domain.interactor.NeuroInteractor
import com.arvrlab.vps_sdk.domain.interactor.VpsInteractor
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.Node

interface VpsService {

    companion object {

        @JvmStatic
        fun newInstance(context: Context): VpsService {
            val vpsApiManager: IVpsApiManager = VpsApiManager()
            val vpsRepository: IVpsRepository = VpsRepository(vpsApiManager)
            val neuroInteractor: INeuroInteractor = NeuroInteractor(context)
            val vpsInteractor: IVpsInteractor = VpsInteractor(vpsRepository, neuroInteractor)
            val arManager = ArManager()
            val locationManager = context.getSystemService(LocationManager::class.java)
            return VpsServiceImpl(vpsInteractor, arManager, locationManager)
        }
    }

    val worldNode: Node

    fun bindArSceneView(arSceneView: ArSceneView)
    fun resume()
    fun pause()
    fun unbindArSceneView()
    fun destroy()

    fun setVpsConfig(vpsConfig: VpsConfig)
    fun setVpsCallback(vpsCallback: VpsCallback)

    fun startVpsService()
    fun stopVpsService()

}