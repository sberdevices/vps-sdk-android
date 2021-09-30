package com.arvrlab.vps_sdk.data.api

import com.arvrlab.vps_sdk.data.model.response.ResponseVpsModel
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

internal interface VpsApi {

    @Multipart
    @POST("vps/api/v1/job")
    suspend fun requestLocalizationBySingleImage(@Part vararg parts: MultipartBody.Part): ResponseVpsModel

    @Multipart
    @POST("vps/api/v1/first_loc/job")
    suspend fun requestLocalizationBySerialImage(@Part vararg parts: MultipartBody.Part): ResponseVpsModel

}