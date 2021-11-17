package com.arvrlab.vps_sdk.domain.interactor

import com.arvrlab.vps_sdk.data.LocalizationType
import com.arvrlab.vps_sdk.data.model.CameraIntrinsics
import com.arvrlab.vps_sdk.domain.model.GpsLocationModel
import com.arvrlab.vps_sdk.domain.model.LocalizationBySerialImages
import com.arvrlab.vps_sdk.domain.model.NodePoseModel

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
    ): NodePoseModel?

    suspend fun calculateNodePose(
        url: String,
        locationID: String,
        sources: List<ByteArray>,
        localizationType: LocalizationType,
        nodePoses: List<NodePoseModel>,
        gpsLocations: List<GpsLocationModel?>,
        cameraIntrinsics: List<CameraIntrinsics>
    ): LocalizationBySerialImages?

    fun destroy()

}