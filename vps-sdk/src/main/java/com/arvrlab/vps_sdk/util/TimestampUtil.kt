package com.arvrlab.vps_sdk.util

import java.util.*

object TimestampUtil {

    fun getTimestampInSec(): Double =
        convertMSecToSec(Date().time)

    fun convertMSecToSec(milliseconds: Long): Double =
        milliseconds / 1000.0

}