package com.arvrlab.vps_sdk.data.api

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Streaming

internal interface NeuroApi {

    @Streaming
    @GET("vpsmobiletflite/230421/hfnet_i8_960.tflite")
    fun loadNeuroModel(): Call<ResponseBody>

}