package com.arvrlab.vps_sdk.domain.interactor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.arvrlab.vps_sdk.data.LocalizationType
import com.arvrlab.vps_sdk.data.MobileVps
import com.arvrlab.vps_sdk.data.Photo
import com.arvrlab.vps_sdk.data.model.CameraIntrinsics
import com.arvrlab.vps_sdk.data.repository.IPrefsRepository
import com.arvrlab.vps_sdk.data.repository.IVpsRepository
import com.arvrlab.vps_sdk.domain.model.*
import com.arvrlab.vps_sdk.util.Constant.BITMAP_HEIGHT
import com.arvrlab.vps_sdk.util.Constant.BITMAP_WIDTH
import com.arvrlab.vps_sdk.util.Constant.QUALITY
import com.arvrlab.vps_sdk.util.TimestampUtil
import com.arvrlab.vps_sdk.util.cropTo16x9
import com.arvrlab.vps_sdk.util.toGrayscale
import java.io.ByteArrayOutputStream

internal class VpsInteractor(
    private val vpsRepository: IVpsRepository,
    private val neuroInteractor: INeuroInteractor,
    private val prefsRepository: IPrefsRepository
) : IVpsInteractor {

    private var scaleFactorPhoto: Float = 1f

    override suspend fun calculateNodePose(
        url: String,
        locationID: String,
        source: ByteArray,
        localizationType: LocalizationType,
        nodePose: NodePoseModel,
        force: Boolean,
        gpsLocation: GpsLocationModel?,
        compass: CompassModel,
        cameraIntrinsics: CameraIntrinsics
    ): LocalizationModel? {
        val byteArray = convertByteArray(source, localizationType)
        val newCameraIntrinsics = cameraIntrinsics.scaleCameraIntrinsics(localizationType)

        val vpsLocationModel = VpsLocationModel(
            userId = prefsRepository.getUserId(),
            timestamp = TimestampUtil.getTimestampInSec(),
            locationID = locationID,
            gpsLocation = gpsLocation,
            compass = compass,
            nodePose = nodePose,
            force = force,
            localizationType = localizationType,
            byteArray = byteArray,
            cameraIntrinsics = newCameraIntrinsics
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
        compasses: List<CompassModel>,
        cameraIntrinsics: List<CameraIntrinsics>
    ): LocalizationBySerialImagesModel? {
        if (sources.size != nodePoses.size) {
            throw IllegalStateException()
        }

        val vpsLocationArray = arrayListOf<VpsLocationModel>()
        sources.forEachIndexed { index, source ->
            val byteArray = convertByteArray(source, localizationType)
            val newCameraIntrinsics = cameraIntrinsics[index]
                .scaleCameraIntrinsics(localizationType)

            vpsLocationArray.add(
                VpsLocationModel(
                    userId = prefsRepository.getUserId(),
                    timestamp = TimestampUtil.getTimestampInSec(),
                    locationID = locationID,
                    gpsLocation = gpsLocations[index],
                    compass = compasses[index],
                    nodePose = nodePoses[0],
                    force = true,
                    localizationType = localizationType,
                    byteArray = byteArray,
                    cameraIntrinsics = newCameraIntrinsics
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
        val source = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            .cropTo16x9()
        scaleFactorPhoto = BITMAP_WIDTH.toFloat() / source.width

        val bitmap = Bitmap.createScaledBitmap(
            source,
            BITMAP_WIDTH,
            BITMAP_HEIGHT,
            false
        ).toGrayscale()

        return ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, stream)
            stream.toByteArray()
        }
    }

    private fun CameraIntrinsics.scaleCameraIntrinsics(localizationType: LocalizationType): CameraIntrinsics {
        val scale = when (localizationType) {
            is Photo -> scaleFactorPhoto
            is MobileVps -> neuroInteractor.scaleFactorImage
        }
        return this.copy(
            fx = fx * scale,
            fy = fy * scale,
            cx = cx * scale,
            cy = cy * scale
        )
    }
}