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

        val mvnOutputMap: Map<Int, Any>
        val mspOutputMap: Map<Int, Any>
        withContext(Dispatchers.Default) {
            val mvnDeferred = async { mvnInterpreter.runForMultipleInputsOutputs(byteBuffer) }
            val mspDeferred = async { mspInterpreter.runForMultipleInputsOutputs(byteBuffer) }

            mvnOutputMap = mvnDeferred.await()
            mspOutputMap = mspDeferred.await()
        }

        return NeuroModel(
            keyPoints = mspOutputMap[0] ?: NeuroModel.EMPTY,
            scores = mspOutputMap[2] ?: NeuroModel.EMPTY,
            descriptors = mspOutputMap[1] ?: NeuroModel.EMPTY,
            globalDescriptor = mvnOutputMap[0] ?: NeuroModel.EMPTY
        )
    }

    private fun convertToByteArray(neuroModel: NeuroModel): ByteArray =
        ByteArrayOutputStream().use { fileData ->
            val version: Byte = 0x1
            val id: Byte = 0x2
            fileData.write(byteArrayOf(version, id))

            val keyPoints = neuroModel.keyPoints.toByteArray()
            fileData.write(keyPoints.size.toByteArray())
            fileData.write(keyPoints)

            val scores = neuroModel.scores.toByteArray()
            fileData.write(scores.size.toByteArray())
            fileData.write(scores)

            val descriptors = neuroModel.descriptors.toByteArray()
            fileData.write(descriptors.size.toByteArray())
            fileData.write(descriptors)

            val globalDescriptor = neuroModel.globalDescriptor.toByteArray()
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

    private fun Interpreter?.runForMultipleInputsOutputs(byteBuffer: ByteBuffer): Map<Int, Any> {
        val outputMap = this?.getOutputMap() ?: return mapOf()

        this.runForMultipleInputsOutputs(arrayOf(byteBuffer), outputMap)

        return outputMap
    }

    private fun Interpreter.getOutputMap(): Map<Int, Any> {
        val outputMap = mutableMapOf<Int, Any>()

        val outputTensorCount = this.outputTensorCount
        repeat(outputTensorCount) { index ->
            val shape = this.getOutputTensor(index).shape()
            outputMap[index] = shape.createFloatArray(0)
        }
        return outputMap
    }

    private fun IntArray.createFloatArray(index: Int): Any =
        if (this.size - 1 == index) {
            FloatArray(this[index])
        } else {
            when (val array = this.createFloatArray(index + 1)) {
                is FloatArray -> Array(this[index]) { array.copyOf() }
                is Array<*> -> Array(this[index]) { array.copyOf() }
                else -> throw IllegalStateException("Will never happen")
            }
        }

    private fun fillBuffer(bitmap: Bitmap, imgData: ByteBuffer) {
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val pixel = Color.green(bitmap.getPixel(x, y))
                imgData.putFloat(pixel.toFloat())
            }
        }
    }

    private fun Any.toByteArray(): ByteArray =
        ByteArrayOutputStream().use { out ->
            when (this) {
                is FloatArray -> {
                    out.write(this.toByteArray())
                }
                is Array<*> -> {
                    this.filterNotNull()
                        .forEach {
                            out.write(it.toByteArray())
                        }
                }
            }
            out.toByteArray()
        }

    private fun FloatArray.toByteArray(): ByteArray =
        ByteBuffer.allocate(2 * this.size)
            .order(ByteOrder.LITTLE_ENDIAN)
            .also { buff ->
                this.forEach { float ->
                    buff.putShort(float.toHalf())
                }
            }
            .array()

    private fun Int.toByteArray(): ByteArray =
        byteArrayOf(
            (this ushr 24).toByte(),
            (this ushr 16).toByte(),
            (this ushr 8).toByte(),
            this.toByte()
        )

}