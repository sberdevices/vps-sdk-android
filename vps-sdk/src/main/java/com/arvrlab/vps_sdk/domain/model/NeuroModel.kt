package com.arvrlab.vps_sdk.domain.model

internal data class NeuroModel(
    val keyPoints: Array<FloatArray> = arrayOf(),
    val scores: FloatArray = floatArrayOf(),
    val descriptors: Array<FloatArray> = arrayOf(),
    val globalDescriptor: Array<FloatArray> = arrayOf()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NeuroModel

        if (!keyPoints.contentDeepEquals(other.keyPoints)) return false
        if (!scores.contentEquals(other.scores)) return false
        if (!descriptors.contentDeepEquals(other.descriptors)) return false
        if (!globalDescriptor.contentEquals(other.globalDescriptor)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = keyPoints.contentDeepHashCode()
        result = 31 * result + scores.contentHashCode()
        result = 31 * result + descriptors.contentDeepHashCode()
        result = 31 * result + globalDescriptor.contentHashCode()
        return result
    }
}