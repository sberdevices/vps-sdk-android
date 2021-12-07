package com.arvrlab.vps_sdk.data.repository

import com.arvrlab.vps_sdk.domain.model.LocalizationBySerialImages
import com.arvrlab.vps_sdk.domain.model.PoseModel
import com.arvrlab.vps_sdk.domain.model.VpsLocationModel

internal interface IVpsRepository {

    suspend fun requestLocalizationBySingleImage(
        url: String,
        vpsLocationModel: VpsLocationModel
    ): PoseModel?

    suspend fun requestLocalizationBySerialImages(
        url: String,
        vararg vpsLocationModel: VpsLocationModel
    ): LocalizationBySerialImages?

}