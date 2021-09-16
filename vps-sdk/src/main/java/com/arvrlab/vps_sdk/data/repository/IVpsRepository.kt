package com.arvrlab.vps_sdk.data.repository

import com.arvrlab.vps_sdk.domain.model.LocalPositionModel
import com.arvrlab.vps_sdk.domain.model.VpsLocationModel

internal interface IVpsRepository {

    suspend fun getLocation(url: String, vpsLocationModel: VpsLocationModel): LocalPositionModel?

}