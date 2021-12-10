package com.arvrlab.vps_sdk.data.repository

import com.arvrlab.vps_sdk.domain.model.LocalizationBySerialImagesModel
import com.arvrlab.vps_sdk.domain.model.LocalizationModel
import com.arvrlab.vps_sdk.domain.model.VpsLocationModel

internal interface IVpsRepository {

    suspend fun requestLocalizationBySingleImage(
        url: String,
        vpsLocationModel: VpsLocationModel
    ): LocalizationModel?

    suspend fun requestLocalizationBySerialImages(
        url: String,
        vararg vpsLocationModel: VpsLocationModel
    ): LocalizationBySerialImagesModel?

}