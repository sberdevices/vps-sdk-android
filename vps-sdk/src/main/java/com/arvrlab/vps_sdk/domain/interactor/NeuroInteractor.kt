package com.arvrlab.vps_sdk.domain.interactor

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.os.Looper
import com.arvrlab.vps_sdk.data.MobileVps
import com.arvrlab.vps_sdk.data.repository.INeuroRepository
import com.arvrlab.vps_sdk.domain.model.NeuroModel
import com.arvrlab.vps_sdk.util.Constant.URL_DELIMITER
import com.arvrlab.vps_sdk.util.toHalf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal class NeuroInteractor(
    private val neuroRepository: INeuroRepository
) : INeuroInteractor {

    private companion object {
        const val MATRIX_ROTATE = 90f
    }

    private var mvnInterpreter: Interpreter? = null
    private var mspInterpreter: Interpreter? = null
    private var mvnNeuroFile: File? = null
    private var mspNeuroFile: File? = null

    override suspend fun loadNeuroModel(mobileVps: MobileVps) {
        if (Looper.myLooper() == Looper.getMainLooper())
            throw IllegalThreadStateException("Must be called from non UI thread.")

        @Suppress("DeferredResultUnused")
        withContext(Dispatchers.IO) {
            async {
                val mnvNeuroUrl = mobileVps.mnvNeuroUrl
                if (mnvNeuroUrl.substringAfterLast(URL_DELIMITER) != mvnNeuroFile?.name) {
                    mvnNeuroFile = neuroRepository.getNeuroModelFile(mnvNeuroUrl)
                }
            }

            async {
                val mspNeuroUrl = mobileVps.mspNeuroUrl
                if (mspNeuroUrl.substringAfterLast(URL_DELIMITER) != mspNeuroFile?.name) {
                    mspNeuroFile = neuroRepository.getNeuroModelFile(mspNeuroUrl)
                }
            }
        }
    }

    override suspend fun codingBitmap(bitmap: Bitmap, dstWidth: Int, dstHeight: Int): ByteArray {
        val neuroModel = convertToNeuroModel(bitmap, dstWidth, dstHeight)
        return convertToByteArray(neuroModel)
    }

    override fun close() {
        mvnInterpreter?.close()
        mvnInterpreter = null
        mspInterpreter?.close()
        mspInterpreter = null
    }

    private suspend fun convertToNeuroModel(
        bitmap: Bitmap,
        dstWidth: Int,
        dstHeight: Int
    ): NeuroModel {
        initInterpreterIfNeed()

        val byteBuffer = convertBitmapToBuffer(bitmap, dstWidth, dstHeight)

        val globalDescriptor: Array<FloatArray>
        val outputMap: Map<Int, Any>
        withContext(Dispatchers.Default) {
            val mvnDerred = async { runMvnNeuro(byteBuffer) }
            val mspDerred = async { runMspNeuro(byteBuffer) }

            globalDescriptor = mvnDerred.await()
            outputMap = mspDerred.await()
        }

        val keyPoints = outputMap[0] as Array<FloatArray>
        val descriptors = outputMap[1] as Array<FloatArray>
        val scores = outputMap[2] as FloatArray

        return NeuroModel(
            keyPoints = keyPoints,
            scores = scores,
            descriptors = descriptors,
            globalDescriptor = globalDescriptor
        )
    }

    private fun convertToByteArray(neuroModel: NeuroModel): ByteArray =
        ByteArrayOutputStream().use { fileData ->
            val version: Byte = 0x1
            val id: Byte = 0x2
            fileData.write(byteArrayOf(version, id))

            val keyPoints = getByteFromArrayFloatArray(neuroModel.keyPoints)
            fileData.write(keyPoints.size.toByteArray())
            fileData.write(keyPoints)

            val scores = getByteArrayFromFloatArray(neuroModel.scores)
            fileData.write(scores.size.toByteArray())
            fileData.write(scores)

            val descriptors = getByteFromArrayFloatArray(neuroModel.descriptors)
            fileData.write(descriptors.size.toByteArray())
            fileData.write(descriptors)

            val globalDescriptor = getByteFromArrayFloatArray(neuroModel.globalDescriptor)
            fileData.write(globalDescriptor.size.toByteArray())
            fileData.write(globalDescriptor)

            fileData.toByteArray()
        }

    private fun initInterpreterIfNeed() {
        val interpreterOptions = Interpreter.Options()
            .apply { setNumThreads(4) }

        if (mvnInterpreter == null) {
            mvnInterpreter = Interpreter(
                mvnNeuroFile ?: throw IllegalStateException("Need load neuro model first."),
                interpreterOptions
            )
        }

        if (mspInterpreter == null) {
            mspInterpreter = Interpreter(
                mspNeuroFile ?: throw IllegalStateException("Need load neuro model first."),
                interpreterOptions
            )
        }
    }

    private fun runMvnNeuro(byteBuffer: ByteBuffer): Array<FloatArray> {
        val outputMap = mutableMapOf<Int, Any>()

        mvnInterpreter?.let {
            val globalDescriptorShape = it.getOutputTensor(0).shape()
            outputMap[0] = Array(globalDescriptorShape[0]) { FloatArray(globalDescriptorShape[1]) }
        }

        mvnInterpreter?.runForMultipleInputsOutputs(arrayOf(byteBuffer), outputMap)

        return outputMap[0] as Array<FloatArray>
    }

    private fun runMspNeuro(byteBuffer: ByteBuffer): Map<Int, Any> {
        val outputMap = mutableMapOf<Int, Any>()

        mspInterpreter?.let {
            val keyPointsShape = it.getOutputTensor(0).shape()
            outputMap[0] = Array(keyPointsShape[0]) { FloatArray(keyPointsShape[1]) }

            val descriptorsShape = it.getOutputTensor(1).shape()
            outputMap[1] = Array(descriptorsShape[0]) { FloatArray(descriptorsShape[1]) }

            val scoresShape = it.getOutputTensor(2).shape()
            outputMap[2] = FloatArray(scoresShape[0])
        }

        mspInterpreter?.runForMultipleInputsOutputs(arrayOf(byteBuffer), outputMap)

        return outputMap
    }

    private fun convertBitmapToBuffer(
        bitmap: Bitmap,
        dstWidth: Int,
        dstHeight: Int
    ): ByteBuffer {
        val resizedBitmap = getPreProcessedBitmap(bitmap, dstWidth, dstHeight)

        val imageByteBuffer = ByteBuffer
            .allocateDirect(resizedBitmap.allocationByteCount)
            .order(ByteOrder.nativeOrder())

        fillBuffer(resizedBitmap, imageByteBuffer)

        return imageByteBuffer
    }

    private fun getPreProcessedBitmap(
        src: Bitmap,
        dstWidth: Int,
        dstHeight: Int
    ): Bitmap {
        val matrix = Matrix()
        val width = src.width
        val height = src.height

        if (width != dstWidth || height != dstHeight) {
            val sx = dstWidth / width.toFloat()
            val sy = dstHeight / height.toFloat()
            matrix.setScale(sx, sy)
        }
        matrix.postRotate(MATRIX_ROTATE)

        return Bitmap.createBitmap(src, 0, 0, width, height, matrix, true)
    }

    private fun fillBuffer(bitmap: Bitmap, imgData: ByteBuffer) {
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val pixel = Color.green(bitmap.getPixel(x, y))
                imgData.putFloat(pixel.toFloat())
            }
        }
    }

    private fun getByteFromArrayFloatArray(array: Array<FloatArray>): ByteArray =
        ByteArrayOutputStream().use { out ->
            array.forEach { floatArray ->
                val buff = getByteArrayFromFloatArray(floatArray)
                out.write(buff)
            }
            out.toByteArray()
        }

    private fun getByteArrayFromFloatArray(floatArray: FloatArray): ByteArray {
        val buff: ByteBuffer = ByteBuffer.allocate(2 * floatArray.size)
        buff.order(ByteOrder.LITTLE_ENDIAN)
        for (value in floatArray) {
            buff.putShort(value.toHalf())
        }
        return buff.array()
    }

    private fun Int.toByteArray(): ByteArray = byteArrayOf(
        (this ushr 24).toByte(),
        (this ushr 16).toByte(),
        (this ushr 8).toByte(),
        this.toByte()
    )

}