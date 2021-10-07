package com.arvrlab.vps_sdk.domain.interactor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import com.arvrlab.vps_sdk.data.LocalizationType
import com.arvrlab.vps_sdk.data.MobileVps
import com.arvrlab.vps_sdk.data.Photo
import com.arvrlab.vps_sdk.data.repository.IVpsRepository
import com.arvrlab.vps_sdk.domain.model.GpsLocationModel
import com.arvrlab.vps_sdk.domain.model.LocalizationBySerialImages
import com.arvrlab.vps_sdk.domain.model.NodePositionModel
import com.arvrlab.vps_sdk.domain.model.VpsLocationModel
import com.arvrlab.vps_sdk.util.Constant.BITMAP_HEIGHT
import com.arvrlab.vps_sdk.util.Constant.BITMAP_WIDTH
import com.arvrlab.vps_sdk.util.Constant.QUALITY
import java.io.ByteArrayOutputStream

internal class VpsInteractor(
    private val vpsRepository: IVpsRepository,
    private val neuroInteractor: INeuroInteractor
) : IVpsInteractor {

    override suspend fun calculateNodePosition(
        url: String,
        locationID: String,
        source: ByteArray,
        localizationType: LocalizationType,
        nodePosition: NodePositionModel,
        force: Boolean,
        gpsLocation: GpsLocationModel?
    ): NodePositionModel? {
        val byteArray = convertByteArray(source, localizationType)

        val vpsLocationModel = VpsLocationModel(
            locationID = locationID,
            gpsLocation = gpsLocation,
            nodePosition = nodePosition,
            force = force,
            localizationType = localizationType,
            byteArray = byteArray
        )
        return vpsRepository.requestLocalizationBySingleImage(url, vpsLocationModel)
    }

    override suspend fun calculateNodePosition(
        url: String,
        locationID: String,
        sources: List<ByteArray>,
        localizationType: LocalizationType,
        nodePositions: List<NodePositionModel>,
        gpsLocations: List<GpsLocationModel>?
    ): LocalizationBySerialImages? {
        if (sources.size != nodePositions.size) {
            throw IllegalStateException()
        }

        val vpsLocationArray = arrayListOf<VpsLocationModel>()
        sources.forEachIndexed { index, source ->
            val byteArray = convertByteArray(source, localizationType)

            vpsLocationArray.add(
                VpsLocationModel(
                    locationID = locationID,
                    gpsLocation = gpsLocations?.getOrNull(index),
                    nodePosition = nodePositions[0],
                    force = true,
                    localizationType = localizationType,
                    byteArray = byteArray
                )
            )
        }

        return vpsRepository.requestLocalizationBySerialImages(
            url,
            *vpsLocationArray.toTypedArray()
        )
    }

    override fun destroy() {
        neuroInteractor.close()
    }

    private fun convertByteArray(source: ByteArray, localizationType: LocalizationType): ByteArray =
        when (localizationType) {
            is Photo -> createJpgByteArray(source)
            is MobileVps -> createNeuroByteArray(localizationType.neuroModelUrl, source)
        }

    private fun createNeuroByteArray(neuroModelUrl: String, byteArray: ByteArray): ByteArray {
        neuroInteractor.loadNeuroModel(neuroModelUrl)
        val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        return neuroInteractor.codingBitmap(bitmap, BITMAP_WIDTH, BITMAP_HEIGHT)
    }

    private fun createJpgByteArray(byteArray: ByteArray): ByteArray {
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