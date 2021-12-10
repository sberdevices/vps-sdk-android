package com.arvrlab.vps_sdk.domain.model

internal data class LocalizationModel(
    val nodePoseModel: NodePoseModel,
    val gpsPoseModel: GpsPoseModel
) {
    companion object {
        val EMPTY = LocalizationModel(NodePoseModel.EMPTY, GpsPoseModel.EMPTY)
    }
}
