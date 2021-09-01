package com.arvrlab.vps_sdk.neuro

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.arvrlab.vps_sdk.extentions.toByteArray
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object NeuroHelper {
    suspend fun getFileAsByteArray(neuroResult: NeuroResult): ByteArray {
        return withContext(Dispatchers.IO) {

            ByteArrayOutputStream().use { fileData ->
                val version: Byte = 0x0
                val id: Byte = 0x0
                fileData.write(byteArrayOf(version, id))

                val keyPoints = getByteFrom2(neuroResult.keyPoints)
                fileData.write(keyPoints.size.toByteArray())
                fileData.write(keyPoints)

                val scores = getByteFrom1(neuroResult.scores)
                fileData.write(scores.size.toByteArray())
                fileData.write(scores)

                val localDescriptors = getByteFrom2(neuroResult.localDescriptors)
                fileData.write(localDescriptors.size.toByteArray())
                fileData.write(localDescriptors)

                val globalDescriptor = getByteFrom1(neuroResult.globalDescriptor)
                fileData.write(globalDescriptor.size.toByteArray())
                fileData.write(globalDescriptor)

                fileData.toByteArray()
            }
        }
    }

    private fun getByteFrom1(floatArray: FloatArray): ByteArray {
        return ByteArrayOutputStream().use { out ->
            val buff = getBuffer(floatArray)
            val base64Str = convertToBase64Bytes(buff.array())
            out.write(base64Str)

            out.toByteArray()
        }

    }

    private fun getByteFrom2(array: Array<FloatArray>): ByteArray {
        val arr = ByteArrayOutputStream().use { out ->
            array.forEach { floatArray ->
                val buff = getByteArrayFromFloatArray(floatArray)

                out.write(buff)
            }
            out.toByteArray()
        }

        return convertToBase64Bytes(arr)
    }

    private fun convertToBase64Bytes(buff: ByteArray): ByteArray {
        return Base64.encode(buff, Base64.NO_WRAP)
    }

    private fun getBuffer(floatArray: FloatArray): ByteBuffer {
        val buff: ByteBuffer = ByteBuffer.allocate(4 * floatArray.size)
        buff.order(ByteOrder.LITTLE_ENDIAN)
        for (value in floatArray) {
            buff.putFloat(value)
        }

        return buff
    }

    private fun getByteArrayFromFloatArray(floatArray: FloatArray): ByteArray {
        val buff: ByteBuffer = ByteBuffer.allocate(4 * floatArray.size)
        buff.order(ByteOrder.LITTLE_ENDIAN)
        for (value in floatArray) {
            buff.putFloat(value)
        }

        return buff.array()
    }
}