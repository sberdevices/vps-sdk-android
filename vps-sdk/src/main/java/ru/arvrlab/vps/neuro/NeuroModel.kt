package ru.arvrlab.vps.neuro

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import ru.arvrlab.vps.extentions.rotate
import ru.arvrlab.vps.extentions.toGrayScaledByteBuffer

class NeuroModel(
    private val context: Context,
    private val filename: String = TF_MODEL_NAME
) {

    private var interpreter: Interpreter? = null

    private var batchSize = 0
    private var inputImageWidth = 0
    private var inputImageHeight = 0
    private var inputPixelSize = 0

    fun close() {
        interpreter?.close()
        interpreter = null
    }

    fun run(bitmap: Bitmap): NeuroResult? {
        initInterpreter()
        val resizedImage = Bitmap.createScaledBitmap(
            bitmap,
            inputImageWidth,
            inputImageHeight,
            true
        ).rotate(90f)

        val byteBuffer = resizedImage.toGrayScaledByteBuffer(batchSize)

        val outputMap = interpreter?.let { initOutputMap(it) }

        interpreter?.runForMultipleInputsOutputs(arrayOf(byteBuffer), outputMap ?: return null)

        val resultMap = outputMap as HashMap<Int, Array<Array<Array<FloatArray>>>>

        val globalDescriptor: FloatArray = resultMap[0]?.get(0)?.get(0)?.get(0) ?: floatArrayOf()
        val keyPoints: FloatArray = resultMap[1]?.get(0)?.get(0)?.get(0) ?: floatArrayOf()
        val localDescriptors: FloatArray = resultMap[2]?.get(0)?.get(0)?.get(0) ?: floatArrayOf()
        val scores: FloatArray = resultMap[3]?.get(0)?.get(0)?.get(0) ?: floatArrayOf()

        return NeuroResult(globalDescriptor, keyPoints, localDescriptors, scores)
    }

    private fun initInterpreter() {
        if (interpreter == null) {
            val interpreterOptions = Interpreter.Options().apply {
                setNumThreads(4)
            }

            interpreter = Interpreter(
                FileUtil.loadMappedFile(context, filename),
                interpreterOptions
            )

            initProperties()
        }
    }

    private fun initProperties() {
        val inputShape: IntArray? = interpreter?.getInputTensor(0)?.shape()

        batchSize = inputShape?.get(0) ?: 0
        inputImageWidth = inputShape?.get(1) ?: 0
        inputImageHeight = inputShape?.get(2) ?: 0
        inputPixelSize = inputShape?.get(3) ?: 0
    }

    private fun initOutputMap(interpreter: Interpreter): HashMap<Int, Any> {
        val outputMap = HashMap<Int, Any>()

        // 1 * 9 * 9 * 32
        val globalDescriptorShape = interpreter.getOutputTensor(3).shape()
        outputMap[0] = Array(globalDescriptorShape[0]) {
            Array(globalDescriptorShape[1]) {
                Array(globalDescriptorShape[2]) { FloatArray(globalDescriptorShape[3]) }
            }
        }

        // 1 * 9 * 9 * 17
        val keyPointsShape = interpreter.getOutputTensor(0).shape()
        outputMap[1] = Array(keyPointsShape[0]) {
            Array(keyPointsShape[1]) {
                Array(keyPointsShape[2]) { FloatArray(keyPointsShape[3]) }
            }
        }

        // 1 * 9 * 9 * 32
        val localDescriptorsShape = interpreter.getOutputTensor(2).shape()
        outputMap[2] = Array(localDescriptorsShape[0]) {
            Array(localDescriptorsShape[1]) {
                Array(localDescriptorsShape[2]) { FloatArray(localDescriptorsShape[3]) }
            }
        }

        // 1 * 9 * 9 * 34
        val scoresShape = interpreter.getOutputTensor(1).shape()
        outputMap[3] = Array(scoresShape[0]) {
            Array(scoresShape[1]) { Array(scoresShape[2]) { FloatArray(scoresShape[3]) } }
        }

        return outputMap
    }

    companion object{
        private const val TF_MODEL_NAME = "hfnet_i8_960.tflite"
    }
}