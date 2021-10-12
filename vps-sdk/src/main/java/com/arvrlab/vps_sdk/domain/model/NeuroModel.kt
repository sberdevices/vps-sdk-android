package com.arvrlab.vps_sdk.domain.model

internal data class NeuroModel(
    val keyPoints: Any,
    val scores: Any,
    val descriptors: Any,
    val globalDescriptor: Any
) {
    companion object {

        val EMPTY = NeuroModel(
            keyPoints = Any(),
            scores = Any(),
            descriptors = Any(),
            globalDescriptor = Any()
        )

    }
}