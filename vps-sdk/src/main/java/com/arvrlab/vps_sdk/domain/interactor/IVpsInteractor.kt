package com.arvrlab.vps_sdk.domain.interactor

import com.arvrlab.vps_sdk.domain.model.GpsLocationModel
import com.arvrlab.vps_sdk.domain.model.LocalizationBySerialImages
import com.arvrlab.vps_sdk.domain.model.NodePositionModel

internal interface IVpsInteractor {

    suspend fun calculateNodePosition(
        url: String,
        locationID: String,
        source: ByteArray,
        useNeuro: Boolean,
        nodePosition: NodePositionModel,
        force: Boolean = false,
        gpsLocation: GpsLocationModel? = null,
    ): NodePositionModel?

    suspend fun calculateNodePosition(
        url: String,
        locationID: String,
        sources: List<ByteArray>,
        useNeuro: Boolean,
        nodePositions: List<NodePositionModel>,
        gpsLocations: List<GpsLocationModel>? = null,
    ): LocalizationBySerialImages?

    fun destroy()

}