package com.arvrlab.vps_sdk.service

internal interface RequestPermissions {

    fun requestPermissions(permissions: Array<String>, requestCode: Int)

}