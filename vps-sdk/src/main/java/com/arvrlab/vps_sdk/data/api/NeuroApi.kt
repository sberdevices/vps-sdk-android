package com.arvrlab.vps_sdk.data.api

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

internal interface NeuroApi {

    @Streaming
    @GET
    fun loadNeuroModel(@Url url: String): Call<ResponseBody>

}