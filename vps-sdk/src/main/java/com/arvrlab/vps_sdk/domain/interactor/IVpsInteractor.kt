package com.arvrlab.vps_sdk.domain.interactor

import com.arvrlab.vps_sdk.data.LocalizationType
import com.arvrlab.vps_sdk.domain.model.GpsLocationModel
import com.arvrlab.vps_sdk.domain.model.LocalizationBySerialImages
import com.arvrlab.vps_sdk.domain.model.NodePositionModel

internal interface IVpsInteractor {

    suspend fun calculateNodePosition(
        url: String,
        locationID: String,
        source: ByteArray,
        localizationType: LocalizationType,
        nodePosition: NodePositionModel,
        force: Boolean = false,
        gpsLocation: GpsLocationModel? = null,
    ): NodePositionModel?

    suspend fun calculateNodePosition(
        url: String,
        locationID: String,
        sources: List<ByteArray>,
        localizationType: LocalizationType,
        nodePositions: List<NodePositionModel>,
        gpsLocations: List<GpsLocationModel>? = null,
    ): LocalizationBySerialImages?

    fun destroy()

}