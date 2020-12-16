package ru.arvrlab.vps.neuro

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import ru.arvrlab.vps.extentions.convertBitmapToBuffer

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

//    fun run(bitmap: Bitmap): NeuroResult? {
//        initInterpreter()
//
//        val byteBuffer = convertBitmapToBuffer(bitmap, inputImageWidth, inputImageHeight)
//
//        val outputMap = interpreter?.let { initOutputMap(it) }
//
//        interpreter?.runForMultipleInputsOutputs(arrayOf(byteBuffer), outputMap ?: return null)
//
//        val globalDescriptor: FloatArray = outputMap?.get(0) as FloatArray
//        val keyPoints: FloatArray = (outputMap[1] as Array<FloatArray>)[0]
//        val localDescriptors: FloatArray = (outputMap[2] as Array<FloatArray>)[0]
//        val scores: FloatArray = outputMap[3] as FloatArray
//        val result = NeuroResult(globalDescriptor, keyPoints, localDescriptors, scores)
//        Log.i("Vps", "result = $result")
//
//        return result
//    }

    fun run(bitmap: Bitmap): NeuroResult? {
        initInterpreter()

        val byteBuffer = convertBitmapToBuffer(bitmap, inputImageWidth, inputImageHeight)

        val outputMap = interpreter?.let { initOutputMap(it) }

        interpreter?.runForMultipleInputsOutputs(arrayOf(byteBuffer), outputMap ?: return null)

        val globalDescriptor: FloatArray = outputMap?.get(0) as FloatArray
        val keyPoints: FloatArray = (outputMap[1] as Array<FloatArray>).flatten()
        val localDescriptors: FloatArray = (outputMap[2] as Array<FloatArray>).flatten()
        val scores: FloatArray = outputMap[3] as FloatArray

        return NeuroResult(globalDescriptor, keyPoints, localDescriptors, scores)
    }

    private fun Array<FloatArray>.flatten() : FloatArray{
        val list = mutableListOf<Float>()
        this.forEach { floats ->
            floats.forEach { f ->
                list.add(f)
            }
        }
        return list.toFloatArray()
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
        val outputMap = hashMapOf<Int, Any>()

        val keyPointsShape = interpreter.getOutputTensor(0).shape()
        outputMap[0] =  FloatArray(keyPointsShape[0])

        val scoresShape = interpreter.getOutputTensor(1).shape()
         outputMap[1] = Array(scoresShape[0])  { FloatArray(scoresShape[1]) }

        val localDescriptorsShape = interpreter.getOutputTensor(2).shape()
        outputMap[2] = Array(localDescriptorsShape[0]) { FloatArray(localDescriptorsShape[1]) }

        val globalDescriptorShape = interpreter.getOutputTensor(3).shape()
         outputMap[3] =  FloatArray(globalDescriptorShape[0])

        return outputMap
    }

    companion object{
        private const val TF_MODEL_NAME = "hfnet_i8_960.tflite"
    }
}