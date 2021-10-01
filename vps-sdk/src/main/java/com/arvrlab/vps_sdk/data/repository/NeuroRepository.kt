package com.arvrlab.vps_sdk.data.repository

import android.content.Context
import com.arvrlab.vps_sdk.data.api.NeuroApi
import com.arvrlab.vps_sdk.util.Constant.URL_DELIMITER
import java.io.File

internal class NeuroRepository(
    private val context: Context,
    private val neuroApi: NeuroApi
) : INeuroRepository {

    override fun getNeuroModelFile(url: String): File {
        val fileName = url.substringAfterLast(URL_DELIMITER)
        val neuroModelFile = File(context.filesDir, fileName)
        if (!neuroModelFile.exists()) {
            neuroApi.loadNeuroModel(url)
                .execute()
                .body()
                ?.byteStream()
                ?.use {
                    neuroModelFile.writeBytes(it.readBytes())
                }
        }
        return neuroModelFile
    }

}