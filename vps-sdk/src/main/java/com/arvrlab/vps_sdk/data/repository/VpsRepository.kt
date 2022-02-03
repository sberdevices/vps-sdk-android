package com.arvrlab.vps_sdk.data.repository

import com.arvrlab.vps_sdk.data.MobileVps
import com.arvrlab.vps_sdk.data.Photo
import com.arvrlab.vps_sdk.data.api.IVpsApiManager
import com.arvrlab.vps_sdk.data.model.request.*
import com.arvrlab.vps_sdk.data.model.response.ResponseLocationModel
import com.arvrlab.vps_sdk.data.model.response.ResponseRelativeModel
import com.arvrlab.vps_sdk.domain.model.*
import com.squareup.moshi.JsonAdapter
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

internal class VpsRepository(
    private val vpsApiManager: IVpsApiManager,
    private val requestVpsAdapter: JsonAdapter<RequestVpsModel>,
) : IVpsRepository {

    private companion object {
        const val STATUS_DONE = "done"

        const val EMBEDDING = "embedding"
        const val IMAGE = "image"
        const val JSON = "json"

        const val MES = "mes"
        const val EMBD = "embd"

        val ANY_MEDIA_TYPE = "*/*".toMediaTypeOrNull()
        val IMAGE_MEDIA_TYPE = "image/jpeg".toMediaTypeOrNull()
        val JSON_MEDIA_TYPE = "application/json".toMediaTypeOrNull()
    }

    override suspend fun requestLocalizationBySingleImage(
        url: String,
        vpsLocationModel: VpsLocationModel
    ): LocalizationModel? {
        val vpsApi = vpsApiManager.getVpsApi(url)

        val jsonBody = vpsLocationModel.toRequestVpsModel()
            .toBodyPart(JSON)
        val contentBody = when (vpsLocationModel.localizationType) {
            is Photo -> vpsLocationModel.toBodyPart(IMAGE_MEDIA_TYPE, IMAGE)
            is MobileVps -> vpsLocationModel.toBodyPart(ANY_MEDIA_TYPE, EMBEDDING)
        }
        val response = vpsApi.requestLocalizationBySingleImage(jsonBody, contentBody)

        val attributesModel = response.data?.attributes
        if (attributesModel?.status == STATUS_DONE) {
            val nodePoseModel = attributesModel.location
                ?.relative
                .toNodePoseModel()
            val gpsPoseModel = attributesModel.location
                .toGpsPoseModel()
            return LocalizationModel(nodePoseModel, gpsPoseModel)
        }
        return null
    }

    override suspend fun requestLocalizationBySerialImages(
        url: String,
        vararg vpsLocationModel: VpsLocationModel
    ): LocalizationBySerialImagesModel? {
        val vpsApi = vpsApiManager.getVpsApi(url)

        val parts = arrayListOf<MultipartBody.Part>()
        vpsLocationModel.forEachIndexed { index, model ->
            parts.add(
                model.toRequestVpsModel()
                    .toBodyPart("$MES$index")
            )
            parts.add(
                when (model.localizationType) {
                    is Photo -> model.toBodyPart(IMAGE_MEDIA_TYPE, "$MES$index")
                    is MobileVps -> model.toBodyPart(ANY_MEDIA_TYPE, "$EMBD$index")
                }
            )
        }
        val response = vpsApi.requestLocalizationBySerialImage(*parts.toTypedArray())

        val attributesModel = response.data?.attributes
        if (attributesModel?.status == STATUS_DONE) {
            val nodePoseModel = attributesModel.location
                ?.relative
                .toNodePoseModel()
            val gpsPoseModel = attributesModel.location
                .toGpsPoseModel()
            return LocalizationBySerialImagesModel(
                LocalizationModel(nodePoseModel, gpsPoseModel),
                response.data.id?.toInt() ?: 0
            )
        }
        return null
    }

    private fun VpsLocationModel.toRequestVpsModel(): RequestVpsModel =
        RequestVpsModel(
            data = RequestDataModel(
                attributes = RequestAttributesModel(
                    userId = this.userId,
                    timestamp = this.timestamp,
                    forcedLocalisation = this.force,
                    location = RequestLocationModel(
                        locationId = this.locationID,
                        localPos = RequestLocalPosModel(
                            x = this.nodePose.x,
                            y = this.nodePose.y,
                            z = this.nodePose.z,
                            roll = this.nodePose.roll,
                            pitch = this.nodePose.pitch,
                            yaw = this.nodePose.yaw,
                        ),
                        gps = this.gpsLocation?.let {
                            RequestGpsModel(
                                accuracy = it.accuracy,
                                altitude = it.altitude,
                                latitude = it.latitude,
                                longitude = it.longitude,
                                timestamp = it.elapsedTimestampSec
                            )
                        },
                        compass = RequestCompassModel(
                            accuracy = this.compass.accuracy,
                            heading = this.compass.heading,
                            timestamp = this.compass.timestamp
                        )
                    ),
                    intrinsics = RequestIntrinsicsModel(
                        cx = cameraIntrinsics.cx,
                        cy = cameraIntrinsics.cy,
                        fx = cameraIntrinsics.fx,
                        fy = cameraIntrinsics.fy,
                    )
                )
            )
        )

    private fun RequestVpsModel.toBodyPart(name: String): MultipartBody.Part {
        val requestBody = requestVpsAdapter.toJson(this).toRequestBody(JSON_MEDIA_TYPE)
        return MultipartBody.Part.createFormData(name, null, requestBody)
    }

    private fun VpsLocationModel.toBodyPart(
        contentType: MediaType?,
        name: String,
        fileName: String? = name
    ): MultipartBody.Part {
        val requestBody = byteArray.toRequestBody(contentType, 0, byteArray.size)
        return MultipartBody.Part.createFormData(name, fileName, requestBody)
    }

    private fun ResponseRelativeModel?.toNodePoseModel(): NodePoseModel =
        if (this == null)
            NodePoseModel.DEFAULT
        else
            NodePoseModel(
                x = this.x ?: 0f,
                y = this.y ?: 0f,
                z = this.z ?: 0f,
                roll = this.roll ?: 0f,
                pitch = this.pitch ?: 0f,
                yaw = this.yaw ?: 0f,
            )

    private fun ResponseLocationModel?.toGpsPoseModel(): GpsPoseModel =
        if (this == null)
            GpsPoseModel.EMPTY
        else
            GpsPoseModel(
                altitude = gps?.altitude ?: 0f,
                latitude = gps?.latitude ?: 0f,
                longitude = gps?.longitude ?: 0f,
                heading = compass?.heading ?: 0f
            )

}