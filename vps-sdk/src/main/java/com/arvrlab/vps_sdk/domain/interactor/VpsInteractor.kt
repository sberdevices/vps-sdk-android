package com.arvrlab.vps_sdk.domain.interactor

import android.graphics.*
import android.media.Image
import com.arvrlab.vps_sdk.data.repository.IVpsRepository
import com.arvrlab.vps_sdk.domain.model.GpsLocationModel
import com.arvrlab.vps_sdk.domain.model.NodePositionModel
import com.arvrlab.vps_sdk.domain.model.VpsLocationModel
import java.io.ByteArrayOutputStream

internal class VpsInteractor(
    private val vpsRepository: IVpsRepository,
    private val neuroInteractor: INeuroInteractor
) : IVpsInteractor {

    private companion object {
        const val BITMAP_WIDTH = 960
        const val BITMAP_HEIGHT = 540

        const val QUALITY = 90
    }

    override suspend fun calculateNodePosition(
        url: String,
        locationID: String,
        image: Image,
        isNeuro: Boolean,
        nodePosition: NodePositionModel,
        force: Boolean,
        gpsLocation: GpsLocationModel?
    ): NodePositionModel? {
        val byteArray = if (isNeuro) {
            createNeuroByteArray(image)
        } else {
            createJpgByteArray(image)
        }
        image.close()

        val vpsLocationModel = VpsLocationModel(
            locationID = locationID,
            gpsLocation = gpsLocation,
            nodePosition = nodePosition,
            force = force,
            isNeuro = isNeuro,
            byteArray = byteArray
        )
        return vpsRepository.calculateNodePosition(url, vpsLocationModel)
    }

    override fun destroy() {
        neuroInteractor.close()
    }

    private fun createNeuroByteArray(image: Image): ByteArray {
        val imageInByteArray = ByteArrayOutputStream().use { out ->
            val yBuffer = image.planes[0].buffer
            val ySize = yBuffer.remaining()
            val nv21 = ByteArray(ySize)

            yBuffer.get(nv21, 0, ySize)

            val yuv = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
            yuv.compressToJpeg(Rect(0, 0, image.width, image.height), QUALITY, out)
            out.toByteArray()
        }
        val bitmap = BitmapFactory.decodeByteArray(imageInByteArray, 0, imageInByteArray.size)

        return neuroInteractor.codingBitmap(bitmap, BITMAP_WIDTH, BITMAP_HEIGHT)
    }

    private fun createJpgByteArray(image: Image): ByteArray {
        val byteArray = image.toByteArray()

        val bitmap = Bitmap.createScaledBitmap(
            BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size),
            BITMAP_WIDTH,
            BITMAP_HEIGHT,
            false
        ).toBlackAndWhiteBitmap()

        return ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, stream)
            stream.toByteArray()
        }
    }

    private fun Image.toByteArray(): ByteArray {
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
        return ByteArrayOutputStream().use { out ->
            yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), QUALITY, out)
            out.toByteArray()
        }
    }

    private fun Bitmap.toBlackAndWhiteBitmap(): Bitmap {
        val blackAndWhiteBitmap = Bitmap.createBitmap(
            this.width, this.height, this.config
        )
        for (x in 0 until this.width) {
            for (y in 0 until this.height) {
                val pixelColor = this.getPixel(x, y)
                val pixelAlpha: Int = Color.alpha(pixelColor)
                val pixelRed: Int = Color.red(pixelColor)
                val pixelGreen: Int = Color.green(pixelColor)
                val pixelBlue: Int = Color.blue(pixelColor)
                val pixelBW = (pixelRed + pixelGreen + pixelBlue) / 3
                val newPixel: Int = Color.argb(pixelAlpha, pixelBW, pixelBW, pixelBW)
                blackAndWhiteBitmap.setPixel(x, y, newPixel)
            }
        }
        return blackAndWhiteBitmap
    }

}