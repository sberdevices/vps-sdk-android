package com.arvrlab.vps_sdk.util

import android.util.Log

internal object Logger {

    private const val TAG: String = "VPS-SDK"

    fun debug(msg: Any?) =
        Log.d(getTag(), msg.toString())

    fun warn(msg: Any?) =
        Log.w(getTag(), msg.toString())

    fun warn(throwable: Throwable) =
        Log.w(getTag(), Constant.EMPTY, throwable)

    fun warn(msg: String, throwable: Throwable) =
        Log.w(getTag(), msg, throwable)

    fun error(msg: Any?) =
        Log.e(getTag(), msg.toString())

    fun error(throwable: Throwable) =
        Log.e(getTag(), Constant.EMPTY, throwable)

    fun error(msg: String, throwable: Throwable) =
        Log.e(getTag(), msg, throwable)

    private fun getTag(): String {
        val stackTrace = Thread.currentThread().stackTrace
        for (i in stackTrace.indices) {
            if (stackTrace[i].methodName == "getTag") {
                val trace = stackTrace[i + 2]
                return "$TAG (${trace.fileName}:${trace.lineNumber})"
            }
        }
        return TAG
    }

}