package com.arvrlab.vps_sdk.domain.interactor

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.util.Base64
import com.arvrlab.vps_sdk.data.repository.INeuroRepository
import com.arvrlab.vps_sdk.domain.model.NeuroModel
import org.tensorflow.lite.Interpreter
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal class NeuroInteractor(
    private val neuroRepository: INeuroRepository
) : INeuroInteractor {

    private companion object {
        const val FLOAT_SIZE = 4
        const val MATRIX_ROTATE = 90f
    }

    private var interpreter: Interpreter? = null

    override fun codingBitmap(bitmap: Bitmap, dstWidth: Int, dstHeight: Int): ByteArray {
        val neuroModel = convertToNeuroModel(bitmap, dstWidth, dstHeight)
        return convertToByteArray(neuroModel)
    }

    override fun close() {
        interpreter?.close()
        interpreter = null
    }

    private fun convertToNeuroModel(bitmap: Bitmap, dstWidth: Int, dstHeight: Int): NeuroModel {
        initInterpreterIfNeed()

        val byteBuffer = convertBitmapToBuffer(bitmap, dstWidth, dstHeight)

        val outputMap = initOutputMap(interpreter)

        interpreter?.runForMultipleInputsOutputs(arrayOf(byteBuffer), outputMap)

        val globalDescriptor = outputMap[0] as FloatArray
        val keyPoints = (outputMap[1] as Array<FloatArray>)
        val localDescriptors = (outputMap[2] as Array<FloatArray>)
        val scores = outputMap[3] as FloatArray

        return NeuroModel(globalDescriptor, keyPoints, localDescriptors, scores)
    }

    private fun convertToByteArray(neuroModel: NeuroModel): ByteArray =
        ByteArrayOutputStream().use { fileData ->
            val version: Byte = 0x0
            val id: Byte = 0x0
            fileData.write(byteArrayOf(version, id))

            val keyPoints = getByteFrom2(neuroModel.keyPoints)
            fileData.write(keyPoints.size.toByteArray())
            fileData.write(keyPoints)

            val scores = getByteFrom1(neuroModel.scores)
            fileData.write(scores.size.toByteArray())
            fileData.write(scores)

            val localDescriptors = getByteFrom2(neuroModel.localDescriptors)
            fileData.write(localDescriptors.size.toByteArray())
            fileData.write(localDescriptors)

            val globalDescriptor = getByteFrom1(neuroModel.globalDescriptor)
            fileData.write(globalDescriptor.size.toByteArray())
            fileData.write(globalDescriptor)

            fileData.toByteArray()
        }

    private fun initInterpreterIfNeed() {
        if (interpreter != null) return

        val interpreterOptions = Interpreter.Options()
            .apply { setNumThreads(4) }

        interpreter = Interpreter(
            neuroRepository.getNeuroModelFile(),
            interpreterOptions
        )
    }

    private fun initOutputMap(interpreter: Interpreter?): Map<Int, Any> {
        val outputMap = mutableMapOf<Int, Any>()

        if (interpreter == null) {
            return mapOf()
        }
        val keyPointsShape = interpreter.getOutputTensor(0).shape()
        outputMap[0] = FloatArray(keyPointsShape[0])

        val scoresShape = interpreter.getOutputTensor(1).shape()
        outputMap[1] = Array(scoresShape[0]) { FloatArray(scoresShape[1]) }

        val localDescriptorsShape = interpreter.getOutputTensor(2).shape()
        outputMap[2] = Array(localDescriptorsShape[0]) { FloatArray(localDescriptorsShape[1]) }

        val globalDescriptorShape = interpreter.getOutputTensor(3).shape()
        outputMap[3] = FloatArray(globalDescriptorShape[0])

        return outputMap
    }

    private fun convertBitmapToBuffer(
        bitmap: Bitmap,
        dstWidth: Int,
        dstHeight: Int
    ): ByteBuffer {
        val imageByteBuffer = ByteBuffer
            .allocateDirect(1 * dstWidth * dstHeight * FLOAT_SIZE)
            .order(ByteOrder.nativeOrder())
        imageByteBuffer.rewind()

        val resizedBitmap = getPreProcessedBitmap(bitmap, dstWidth, dstHeight)

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

    private fun getByteFrom1(floatArray: FloatArray): ByteArray =
        ByteArrayOutputStream().use { out ->
            val buff = getBuffer(floatArray)
            val base64Str = convertToBase64Bytes(buff.array())
            out.write(base64Str)

            out.toByteArray()
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

    private fun convertToBase64Bytes(buff: ByteArray): ByteArray =
        Base64.encode(buff, Base64.NO_WRAP)

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

    private fun Int.toByteArray(): ByteArray = byteArrayOf(
        (this ushr 24).toByte(),
        (this ushr 16).toByte(),
        (this ushr 8).toByte(),
        this.toByte()
    )

}