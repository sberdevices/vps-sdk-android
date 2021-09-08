package com.arvrlab.vps_sdk.neuro

import android.content.Context
import android.graphics.Bitmap
import com.arvrlab.vps_sdk.util.convertBitmapToBuffer
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil

class NeuroModel(
    private val context: Context,
    private val filename: String = TF_MODEL_NAME
) {

    private companion object {
        const val TF_MODEL_NAME = "hfnet_i8_960.tflite"
    }

    private val interpreter: Interpreter by lazy {
        val interpreterOptions = Interpreter.Options().apply {
            setNumThreads(4)
        }

        Interpreter(
            FileUtil.loadMappedFile(context, filename),
            interpreterOptions
        )
    }

    fun getFeatures(bitmap: Bitmap): NeuroResult {
        val byteBuffer = convertBitmapToBuffer(bitmap)

        val outputMap = initOutputMap(interpreter)

        interpreter?.runForMultipleInputsOutputs(arrayOf(byteBuffer), outputMap)

        val globalDescriptor = outputMap[0] as FloatArray
        val keyPoints = (outputMap[1] as Array<FloatArray>)
        val localDescriptors = (outputMap[2] as Array<FloatArray>)
        val scores = outputMap[3] as FloatArray

        return NeuroResult(globalDescriptor, keyPoints, localDescriptors, scores)
    }

    fun close() {
        interpreter.close()
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

}