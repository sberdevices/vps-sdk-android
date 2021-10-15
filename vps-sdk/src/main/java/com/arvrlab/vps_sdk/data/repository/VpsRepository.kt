package com.arvrlab.vps_sdk.data.repository

import com.arvrlab.vps_sdk.data.MobileVps
import com.arvrlab.vps_sdk.data.Photo
import com.arvrlab.vps_sdk.data.api.IVpsApiManager
import com.arvrlab.vps_sdk.data.model.request.*
import com.arvrlab.vps_sdk.data.model.response.ResponseRelativeModel
import com.arvrlab.vps_sdk.domain.model.LocalizationBySerialImages
import com.arvrlab.vps_sdk.domain.model.NodePositionModel
import com.arvrlab.vps_sdk.domain.model.VpsLocationModel
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
    ): NodePositionModel? {
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
            return attributesModel.location
                ?.relative
                .toNodePositionModel()
        }
        return null
    }

    override suspend fun requestLocalizationBySerialImages(
        url: String,
        vararg vpsLocationModel: VpsLocationModel
    ): LocalizationBySerialImages? {
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
            val nodePositionModel = attributesModel.location
                ?.relative
                .toNodePositionModel()
            return LocalizationBySerialImages(nodePositionModel, response.data.id?.toInt() ?: 0)
        }
        return null
    }

    private fun VpsLocationModel.toRequestVpsModel(): RequestVpsModel =
        RequestVpsModel(
            data = RequestDataModel(
                attributes = RequestAttributesModel(
                    forcedLocalisation = this.force,
                    location = RequestLocationModel(
                        locationId = this.locationID,
                        localPos = RequestLocalPosModel(
                            x = this.nodePosition.x,
                            y = this.nodePosition.y,
                            z = this.nodePosition.z,
                            roll = this.nodePosition.roll,
                            pitch = this.nodePosition.pitch,
                            yaw = this.nodePosition.yaw,
                        ),
                        gps = this.gpsLocation?.let {
                            RequestGpsModel(
                                accuracy = it.accuracy,
                                altitude = it.altitude,
                                latitude = it.longitude,
                                longitude = it.longitude,
                                timestamp = it.elapsedRealtimeNanos
                            )
                        }
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

    private fun ResponseRelativeModel?.toNodePositionModel(): NodePositionModel =
        if (this == null)
            NodePositionModel.EMPTY
        else
            NodePositionModel(
                x = this.x ?: 0f,
                y = this.y ?: 0f,
                z = this.z ?: 0f,
                roll = this.roll ?: 0f,
                pitch = this.pitch ?: 0f,
                yaw = this.yaw ?: 0f,
            )

}