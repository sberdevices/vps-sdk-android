package com.arvrlab.vps_sdk.domain.neuro

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder

class NeuroModel(
    private val context: Context,
    private val filename: String = TF_MODEL_NAME
) {

    private companion object {
        const val TF_MODEL_NAME = "hfnet_i8_960.tflite"

        const val FLOAT_SIZE = 4
    }

    private var interpreter: Interpreter? = null

    fun getFeatures(bitmap: Bitmap, dstWidth: Int, dstHeight: Int): NeuroResult {
        initInterpreterIfNeed()

        val byteBuffer = convertBitmapToBuffer(bitmap, dstWidth, dstHeight)

        val outputMap = initOutputMap(interpreter)

        interpreter?.runForMultipleInputsOutputs(arrayOf(byteBuffer), outputMap)

        val globalDescriptor = outputMap[0] as FloatArray
        val keyPoints = (outputMap[1] as Array<FloatArray>)
        val localDescriptors = (outputMap[2] as Array<FloatArray>)
        val scores = outputMap[3] as FloatArray

        return NeuroResult(globalDescriptor, keyPoints, localDescriptors, scores)
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }

    private fun initInterpreterIfNeed() {
        if (interpreter != null) return

        val interpreterOptions = Interpreter.Options().apply {
            setNumThreads(4)
        }

        interpreter = Interpreter(
            FileUtil.loadMappedFile(context, filename),
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

        val resizedBitmap = getPreProcessedBitmap(90f, bitmap, dstWidth, dstHeight)
        bitmap.recycle()

        fillBuffer(resizedBitmap, imageByteBuffer)

        return imageByteBuffer
    }

    private fun getPreProcessedBitmap(
        degrees: Float,
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
        matrix.postRotate(degrees)

        return Bitmap.createBitmap(src, 0, 0, width, height, matrix, true)
    }

    private fun fillBuffer(bitmap: Bitmap, imgData: ByteBuffer) {
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val pixel = Color.green(bitmap.getPixel(x, y))
                imgData.putFloat(pixel.toFloat())
            }
        }

        bitmap.recycle()
    }

}