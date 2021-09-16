package com.arvrlab.vps_sdk.data.api

internal interface IVpsApiManager {

    fun getVpsApi(url: String): VpsApi

}