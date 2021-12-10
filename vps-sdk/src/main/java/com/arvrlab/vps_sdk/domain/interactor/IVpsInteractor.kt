package com.arvrlab.vps_sdk.domain.interactor

import com.arvrlab.vps_sdk.data.LocalizationType
import com.arvrlab.vps_sdk.data.model.CameraIntrinsics
import com.arvrlab.vps_sdk.domain.model.GpsLocationModel
import com.arvrlab.vps_sdk.domain.model.LocalizationBySerialImagesModel
import com.arvrlab.vps_sdk.domain.model.NodePoseModel
import com.arvrlab.vps_sdk.domain.model.LocalizationModel

internal interface IVpsInteractor {

    suspend fun calculateNodePose(
        url: String,
        locationID: String,
        source: ByteArray,
        localizationType: LocalizationType,
        nodePose: NodePoseModel,
        force: Boolean = false,
        gpsLocation: GpsLocationModel? = null,
        cameraIntrinsics: CameraIntrinsics
    ): LocalizationModel?

    suspend fun calculateNodePose(
        url: String,
        locationID: String,
        sources: List<ByteArray>,
        localizationType: LocalizationType,
        nodePoses: List<NodePoseModel>,
        gpsLocations: List<GpsLocationModel?>,
        cameraIntrinsics: List<CameraIntrinsics>
    ): LocalizationBySerialImagesModel?

    fun destroy()

}