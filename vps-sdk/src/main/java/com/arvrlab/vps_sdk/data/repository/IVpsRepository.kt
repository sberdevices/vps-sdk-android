package com.arvrlab.vps_sdk.data.repository

import com.arvrlab.vps_sdk.domain.model.NodePositionModel
import com.arvrlab.vps_sdk.domain.model.VpsLocationModel

internal interface IVpsRepository {

    suspend fun calculateNodePosition(url: String, vpsLocationModel: VpsLocationModel): NodePositionModel?

}