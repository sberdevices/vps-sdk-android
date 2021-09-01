package com.arvrlab.vps_sdk.network

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.arvrlab.vps_sdk.network.dto.RequestDto
import com.arvrlab.vps_sdk.network.dto.ResponseDto
import okhttp3.MultipartBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface VpsApiService {

    @Multipart
    @POST("job")
    suspend fun process(
        @Part("json") json: RequestDto,
        @Part image: MultipartBody.Part
    ): ResponseDto

}

object VpsApi {

    var BASE_URL: String? = null
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    var retrofitService: VpsApiService? = null

    fun getApiService(baseUrl: String): VpsApiService {
        if (baseUrl != BASE_URL) {
            BASE_URL = baseUrl
            retrofitService = null
        }

        val service = retrofitService ?: getClient(baseUrl).create(VpsApiService::class.java)
        if(retrofitService == null) {
            retrofitService = service
        }

        return service
    }

    private fun getClient(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .baseUrl(baseUrl)
            .build()
    }
}