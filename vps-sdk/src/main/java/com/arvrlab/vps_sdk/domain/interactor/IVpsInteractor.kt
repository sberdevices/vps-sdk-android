package com.arvrlab.vps_sdk.domain.interactor

import android.media.Image
import com.arvrlab.vps_sdk.domain.model.GpsLocationModel
import com.arvrlab.vps_sdk.domain.model.NodePositionModel

internal interface IVpsInteractor {

    suspend fun calculateNodePosition(
        url: String,
        locationID: String,
        image: Image,
        isNeuro: Boolean,
        nodePosition: NodePositionModel,
        force: Boolean = false,
        gpsLocation: GpsLocationModel? = null,
    ): NodePositionModel?

    fun destroy()

}