package com.arvrlab.vps_sdk.domain.model

import android.location.Location

internal data class VpsLocationModel(
    val locationID: String,
    val location: Location?,
    val localPosition: LocalPositionModel,
    val force: Boolean,
    val isNeuro: Boolean,
    val byteArray: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VpsLocationModel

        if (locationID != other.locationID) return false
        if (location != other.location) return false
        if (localPosition != other.localPosition) return false
        if (force != other.force) return false
        if (isNeuro != other.isNeuro) return false
        if (!byteArray.contentEquals(other.byteArray)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = locationID.hashCode()
        result = 31 * result + (location?.hashCode() ?: 0)
        result = 31 * result + localPosition.hashCode()
        result = 31 * result + force.hashCode()
        result = 31 * result + isNeuro.hashCode()
        result = 31 * result + byteArray.contentHashCode()
        return result
    }
}
