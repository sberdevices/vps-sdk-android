package com.arvrlab.vps_sdk.neuro

data class NeuroResult(
    var globalDescriptor: FloatArray = floatArrayOf(),
    var keyPoints: Array<FloatArray> = arrayOf(),
    var localDescriptors: Array<FloatArray> = arrayOf(),
    var scores: FloatArray = floatArrayOf()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NeuroResult

        if (!globalDescriptor.contentEquals(other.globalDescriptor)) return false
        if (!keyPoints.contentEquals(other.keyPoints)) return false
        if (!localDescriptors.contentEquals(other.localDescriptors)) return false
        if (!scores.contentEquals(other.scores)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = globalDescriptor.contentHashCode()
        result = 31 * result + keyPoints.contentHashCode()
        result = 31 * result + localDescriptors.contentHashCode()
        result = 31 * result + scores.contentHashCode()
        return result
    }
}