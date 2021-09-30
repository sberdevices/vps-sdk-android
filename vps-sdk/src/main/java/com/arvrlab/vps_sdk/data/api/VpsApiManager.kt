package com.arvrlab.vps_sdk.data.api

import com.squareup.moshi.Moshi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

internal class VpsApiManager(
    private val defaultClient: OkHttpClient,
    private val moshi: Moshi
) : IVpsApiManager {

    private val cacheVpsApi: MutableMap<String, VpsApi> = mutableMapOf()

    private val ignore401Error: Interceptor = Interceptor {
        val response = it.proceed(it.request())
        if (response.code == 401) {
            response.newBuilder()
                .code(200)
                .build()
        } else {
            response
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        defaultClient.newBuilder()
            .addInterceptor(ignore401Error)
            .build()
    }

    override fun getVpsApi(url: String): VpsApi =
        cacheVpsApi[url]
            ?: getClient(url)
                .create(VpsApi::class.java)
                .also { cacheVpsApi[url] = it }

    private fun getClient(baseUrl: String): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

}