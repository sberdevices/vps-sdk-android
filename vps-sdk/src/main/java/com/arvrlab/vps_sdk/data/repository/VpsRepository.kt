package com.arvrlab.vps_sdk.data.repository

import com.arvrlab.vps_sdk.data.api.IVpsApiManager
import com.arvrlab.vps_sdk.data.model.request.*
import com.arvrlab.vps_sdk.domain.model.LocalPositionModel
import com.arvrlab.vps_sdk.domain.model.VpsLocationModel
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

internal class VpsRepository(private val vpsApiManager: IVpsApiManager) : IVpsRepository {

    private companion object {
        const val STATUS_DONE = "done"

        const val IMAGE = "image"
        const val EMBEDDING = "embedding"

        val ANY_MEDIA_TYPE = "*/*".toMediaTypeOrNull()
        val IMAGE_MEDIA_TYPE = "image/jpeg".toMediaTypeOrNull()
    }

    override suspend fun getLocation(url: String, vpsLocationModel: VpsLocationModel): LocalPositionModel? {
        val requestVpsModel = vpsLocationModel.toRequestVpsModel()
        val bodyPart = if (vpsLocationModel.isNeuro) {
            vpsLocationModel.toBodyPart(ANY_MEDIA_TYPE, EMBEDDING)
        } else {
            vpsLocationModel.toBodyPart(IMAGE_MEDIA_TYPE, IMAGE)
        }

        val response = vpsApiManager.getVpsApi(url).requestLocation(requestVpsModel, bodyPart)

        if (response.data?.attributes?.status == STATUS_DONE) {
            val relative = response.data.attributes.location?.relative

            return LocalPositionModel(
                x = relative?.x ?: 0f,
                y = relative?.y ?: 0f,
                z = relative?.z ?: 0f,
                roll = relative?.roll ?: 0f,
                pitch = relative?.pitch ?: 0f,
                yaw = relative?.yaw ?: 0f,
            )
        }
        return null
    }

    private fun VpsLocationModel.toRequestVpsModel(): RequestVpsModel {
        val localPos = if (!this.force) {
            RequestLocalPosModel(
                x = this.localPosition.x,
                y = this.localPosition.y,
                z = this.localPosition.z,
                roll = this.localPosition.roll,
                pitch = this.localPosition.pitch,
                yaw = this.localPosition.yaw,
            )
        } else {
            RequestLocalPosModel()
        }

        val gpsModel: RequestGpsModel? = this.location?.run {
            RequestGpsModel(
                accuracy.toDouble(),
                altitude,
                latitude,
                longitude,
                elapsedRealtimeNanos.toDouble()
            )
        }

        return RequestVpsModel(
            data = RequestDataModel(
                attributes = RequestAttributesModel(
                    forcedLocalisation = this.force,
                    location = RequestLocationModel(
                        locationId = this.locationID,
                        localPos = localPos,
                        gps = gpsModel
                    )
                )
            )
        )
    }

    private fun VpsLocationModel.toBodyPart(
        contentType: MediaType?,
        name: String,
        fileName: String = name
    ): MultipartBody.Part {
        val requestBody = byteArray.toRequestBody(contentType, 0, byteArray.size)
        return MultipartBody.Part.createFormData(name, fileName, requestBody)
    }
}