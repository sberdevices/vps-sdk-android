package com.arvrlab.vps_sdk.domain.model

import com.arvrlab.vps_sdk.data.LocalizationType
import com.arvrlab.vps_sdk.data.model.CameraIntrinsics

internal data class VpsLocationModel(
    val userId: String,
    val timestamp: Double,
    val locationID: String,
    val gpsLocation: GpsLocationModel?,
    val compass: CompassModel,
    val nodePose: NodePoseModel,
    val force: Boolean,
    val localizationType: LocalizationType,
    val byteArray: ByteArray,
    val cameraIntrinsics: CameraIntrinsics
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VpsLocationModel

        if (userId != other.userId) return false
        if (timestamp != other.timestamp) return false
        if (locationID != other.locationID) return false
        if (gpsLocation != other.gpsLocation) return false
        if (compass != other.compass) return false
        if (nodePose != other.nodePose) return false
        if (force != other.force) return false
        if (localizationType != other.localizationType) return false
        if (!byteArray.contentEquals(other.byteArray)) return false
        if (cameraIntrinsics != other.cameraIntrinsics) return false

        return true
    }

    override fun hashCode(): Int {
        var result = userId.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + locationID.hashCode()
        result = 31 * result + (gpsLocation?.hashCode() ?: 0)
        result = 31 * result + compass.hashCode()
        result = 31 * result + nodePose.hashCode()
        result = 31 * result + force.hashCode()
        result = 31 * result + localizationType.hashCode()
        result = 31 * result + byteArray.contentHashCode()
        result = 31 * result + cameraIntrinsics.hashCode()
        return result
    }
}
