package com.arvrlab.vps_android_prototype.util

import android.util.Log

object Logger {

    private const val TAG: String = "VPS-Prototype"
    private const val EMPTY: String = ""

    fun debug(msg: Any?) =
        Log.d(getTag(), msg.toString())

    fun warn(msg: Any?) =
        Log.w(getTag(), msg.toString())

    fun warn(throwable: Throwable) =
        Log.w(getTag(), EMPTY, throwable)

    fun warn(msg: String, throwable: Throwable) =
        Log.w(getTag(), msg, throwable)

    fun error(msg: Any?) =
        Log.e(getTag(), msg.toString())

    fun error(throwable: Throwable) =
        Log.e(getTag(), EMPTY, throwable)

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