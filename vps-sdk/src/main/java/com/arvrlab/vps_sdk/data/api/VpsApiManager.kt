package com.arvrlab.vps_sdk.data.api

import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import org.koin.java.KoinJavaComponent.inject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

internal class VpsApiManager : IVpsApiManager {

    private val cacheVpsApi: MutableMap<String, VpsApi> = mutableMapOf()

    private val okHttpClient: OkHttpClient by inject(OkHttpClient::class.java)
    private val moshi: Moshi by inject(Moshi::class.java)

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