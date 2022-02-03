package com.arvrlab.vps_sdk.util

object TimestampUtil {

    fun getTimestampInSec(): Double =
        convertMSecToSec(System.currentTimeMillis())

    fun convertMSecToSec(milliseconds: Long): Double =
        milliseconds / 1_000.0

}