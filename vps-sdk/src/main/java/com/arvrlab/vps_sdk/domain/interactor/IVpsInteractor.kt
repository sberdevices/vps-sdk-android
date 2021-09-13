package com.arvrlab.vps_sdk.domain.interactor

import com.arvrlab.vps_sdk.data.VpsConfig
import com.arvrlab.vps_sdk.ui.VpsCallback

internal interface IVpsInteractor {

    val vpsConfig: VpsConfig

    fun setVpsConfig(vpsConfig: VpsConfig)
    fun setVpsCallback(vpsCallback: VpsCallback)
    fun enableForceLocalization(enabled: Boolean)

    fun startLocatization()
    fun stopLocatization()

    fun destroy()

}