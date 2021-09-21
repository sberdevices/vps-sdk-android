package com.arvrlab.vps_sdk.data.repository

import com.arvrlab.vps_sdk.data.api.IVpsApiManager
import com.arvrlab.vps_sdk.data.model.request.*
import com.arvrlab.vps_sdk.domain.model.NodePositionModel
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

    override suspend fun calculateNodePosition(url: String, vpsLocationModel: VpsLocationModel): NodePositionModel? {
        val requestVpsModel = vpsLocationModel.toRequestVpsModel()
        val bodyPart = if (vpsLocationModel.isNeuro) {
            vpsLocationModel.toBodyPart(ANY_MEDIA_TYPE, EMBEDDING)
        } else {
            vpsLocationModel.toBodyPart(IMAGE_MEDIA_TYPE, IMAGE)
        }

        val response = vpsApiManager.getVpsApi(url).calculateNodePosition(requestVpsModel, bodyPart)

        if (response.data?.attributes?.status == STATUS_DONE) {
            val relative = response.data.attributes.location?.relative

            return NodePositionModel(
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
                x = this.nodePosition.x,
                y = this.nodePosition.y,
                z = this.nodePosition.z,
                roll = this.nodePosition.roll,
                pitch = this.nodePosition.pitch,
                yaw = this.nodePosition.yaw,
            )
        } else {
            RequestLocalPosModel()
        }

        return RequestVpsModel(
            data = RequestDataModel(
                attributes = RequestAttributesModel(
                    forcedLocalisation = this.force,
                    location = RequestLocationModel(
                        locationId = this.locationID,
                        localPos = localPos,
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