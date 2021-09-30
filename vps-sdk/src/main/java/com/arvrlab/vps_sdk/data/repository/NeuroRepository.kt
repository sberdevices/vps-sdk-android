package com.arvrlab.vps_sdk.data.repository

import android.content.Context
import com.arvrlab.vps_sdk.data.api.NeuroApi
import java.io.File

internal class NeuroRepository(
    private val context: Context,
    private val neuroApi: NeuroApi
) : INeuroRepository {

    private companion object {
        const val TF_MODEL_NAME = "hfnet_i8_960.tflite"
    }

    override fun getNeuroModelFile(): File {
        val neuroModelFile = File(context.filesDir, TF_MODEL_NAME)
        if (!neuroModelFile.exists()) {
            neuroApi.loadNeuroModel()
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