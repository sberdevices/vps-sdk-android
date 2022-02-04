package com.arvrlab.vps_sdk.domain.interactor

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Looper
import com.arvrlab.vps_sdk.data.MobileVps
import com.arvrlab.vps_sdk.data.repository.INeuroRepository
import com.arvrlab.vps_sdk.domain.model.NeuroModel
import com.arvrlab.vps_sdk.util.ColorUtil
import com.arvrlab.vps_sdk.util.Constant.URL_DELIMITER
import com.arvrlab.vps_sdk.util.cropTo9x16
import com.arvrlab.vps_sdk.util.toHalf
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import org.tensorflow.lite.Interpreter
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.collections.set

internal class NeuroInteractor(
    private val neuroRepository: INeuroRepository
) : INeuroInteractor {

    private companion object {
        const val MATRIX_ROTATE = 90f

        const val CLOSE_DELAY = 100L
    }

    override var scaleFactorImage: Float = 1f
        private set

    private val coroutineScope: CoroutineScope =
        CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val mutex: Mutex = Mutex()

    private var mvnInterpreter: Interpreter? = null
    private var mspInterpreter: Interpreter? = null
    private var mvnNeuroFile: File? = null
    private var mspNeuroFile: File? = null

    override suspend fun loadNeuroModel(mobileVps: MobileVps) {
        if (Looper.myLooper() == Looper.getMainLooper())
            throw IllegalThreadStateException("Must be called from non UI thread.")

        @Suppress("DeferredResultUnused")
        coroutineScope.launch(Dispatchers.IO) {
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
        }.join()
    }

    override suspend fun codingBitmap(bitmap: Bitmap): ByteArray {
        val neuroModel = convertToNeuroModel(bitmap.cropTo9x16())
        return convertToByteArray(neuroModel)
    }

    override fun close() {
        coroutineScope.launch {
            while (mutex.isLocked) delay(CLOSE_DELAY)

            mvnInterpreter?.close()
            mvnInterpreter = null
            mspInterpreter?.close()
            mspInterpreter = null

            coroutineScope.cancel()
        }
    }

    private suspend fun convertToNeuroModel(
        bitmap: Bitmap
    ): NeuroModel {
        initInterpreterIfNeed()

        val mspInputModel = mspInterpreter?.getInputModel() ?: return NeuroModel.EMPTY
        val mvnInputModel = mvnInterpreter?.getInputModel()

        val mspByteBuffer = convertBitmapToBuffer(bitmap, mspInputModel)
        val mvnByteBuffer = if (mvnInputModel == null || mspInputModel == mvnInputModel) {
            mspByteBuffer
        } else {
            convertBitmapToBuffer(bitmap, mvnInputModel)
        }

        lateinit var mspOutputMap: Map<Int, Any>
        lateinit var mvnOutputMap: Map<Int, Any>
        coroutineScope.launch(Dispatchers.Default) {
            mutex.lock()

            val mspDeferred = async { mspInterpreter.runForMultipleInputsOutputs(mspByteBuffer) }
            val mvnDeferred = async { mvnInterpreter.runForMultipleInputsOutputs(mvnByteBuffer) }

            mspOutputMap = mspDeferred.await()
            mvnOutputMap = mvnDeferred.await()

            mutex.unlock()
        }.join()

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
            .apply {
                setNumThreads(4)
                setCancellable(true)
            }

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
        inputModel: InputModel
    ): ByteBuffer =
        bitmap.prepareBitmap(inputModel.imageWidth, inputModel.imageHeight)
            .getByteBuffer(inputModel.grayImage)

    private fun Bitmap.prepareBitmap(newWidth: Int, newHeight: Int): Bitmap {
        val matrix = Matrix()

        if (width != newWidth || height != newHeight) {
            scaleFactorImage = newWidth / width.toFloat()
            matrix.setScale(scaleFactorImage, scaleFactorImage)
        } else {
            scaleFactorImage = 1f
        }
        matrix.postRotate(MATRIX_ROTATE)

        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    private fun Interpreter?.runForMultipleInputsOutputs(byteBuffer: ByteBuffer): Map<Int, Any> {
        val outputMap = this?.getOutputMap() ?: return mapOf()

        this.runForMultipleInputsOutputs(arrayOf(byteBuffer), outputMap)

        return outputMap
    }

    private fun Interpreter.getInputModel(): InputModel {
        val shape = this.getInputTensor(0).shape()
        return InputModel(
            imageWidth = shape[1],
            imageHeight = shape[2],
            grayImage = shape[3] == 1
        )
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

    private fun Bitmap.getByteBuffer(useGrayScale: Boolean): ByteBuffer {
        val imageByteBuffer = ByteBuffer
            .allocateDirect(allocationByteCount)
            .order(ByteOrder.nativeOrder())

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = getPixel(x, y)
                val color = if (useGrayScale) ColorUtil.gray(pixel) else pixel
                imageByteBuffer.putFloat(color.toFloat())
            }
        }
        return imageByteBuffer
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

    private data class InputModel(
        val imageWidth: Int,
        val imageHeight: Int,
        val grayImage: Boolean,
    )

}