package com.arvrlab.vps_sdk.domain.interactor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.arvrlab.vps_sdk.data.LocalizationType
import com.arvrlab.vps_sdk.data.MobileVps
import com.arvrlab.vps_sdk.data.Photo
import com.arvrlab.vps_sdk.data.model.CameraIntrinsics
import com.arvrlab.vps_sdk.data.repository.IVpsRepository
import com.arvrlab.vps_sdk.domain.model.*
import com.arvrlab.vps_sdk.util.Constant.BITMAP_HEIGHT
import com.arvrlab.vps_sdk.util.Constant.BITMAP_WIDTH
import com.arvrlab.vps_sdk.util.Constant.QUALITY
import com.arvrlab.vps_sdk.util.toGrayscale
import java.io.ByteArrayOutputStream

internal class VpsInteractor(
    private val vpsRepository: IVpsRepository,
    private val neuroInteractor: INeuroInteractor
) : IVpsInteractor {

    override suspend fun calculateNodePose(
        url: String,
        locationID: String,
        source: ByteArray,
        localizationType: LocalizationType,
        nodePose: NodePoseModel,
        force: Boolean,
        gpsLocation: GpsLocationModel?,
        cameraIntrinsics: CameraIntrinsics
    ): LocalizationModel? {
        val byteArray = convertByteArray(source, localizationType)

        val vpsLocationModel = VpsLocationModel(
            locationID = locationID,
            gpsLocation = gpsLocation,
            nodePose = nodePose,
            force = force,
            localizationType = localizationType,
            byteArray = byteArray,
            cameraIntrinsics = cameraIntrinsics
        )
        return vpsRepository.requestLocalizationBySingleImage(url, vpsLocationModel)
    }

    override suspend fun calculateNodePose(
        url: String,
        locationID: String,
        sources: List<ByteArray>,
        localizationType: LocalizationType,
        nodePoses: List<NodePoseModel>,
        gpsLocations: List<GpsLocationModel?>,
        cameraIntrinsics: List<CameraIntrinsics>
    ): LocalizationBySerialImagesModel? {
        if (sources.size != nodePoses.size) {
            throw IllegalStateException()
        }

        val vpsLocationArray = arrayListOf<VpsLocationModel>()
        sources.forEachIndexed { index, source ->
            val byteArray = convertByteArray(source, localizationType)

            vpsLocationArray.add(
                VpsLocationModel(
                    locationID = locationID,
                    gpsLocation = gpsLocations[index],
                    nodePose = nodePoses[0],
                    force = true,
                    localizationType = localizationType,
                    byteArray = byteArray,
                    cameraIntrinsics = cameraIntrinsics[index]
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

    private suspend fun convertByteArray(
        source: ByteArray,
        localizationType: LocalizationType
    ): ByteArray =
        when (localizationType) {
            is Photo -> createJpgByteArray(source)
            is MobileVps -> {
                neuroInteractor.loadNeuroModel(localizationType)
                createNeuroByteArray(source)
            }
        }

    private suspend fun createNeuroByteArray(byteArray: ByteArray): ByteArray {
        val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        return neuroInteractor.codingBitmap(bitmap)
    }

    private fun createJpgByteArray(byteArray: ByteArray): ByteArray {
        val bitmap = Bitmap.createScaledBitmap(
            BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size),
            BITMAP_WIDTH,
            BITMAP_HEIGHT,
            false
        ).toGrayscale()

        return ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, stream)
            stream.toByteArray()
        }
    }

}