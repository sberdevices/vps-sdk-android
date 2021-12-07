package com.arvrlab.vps_sdk.domain.model

internal data class PoseModel(
    val nodePoseModel: NodePoseModel,
    val gpsPoseModel: GpsPoseModel
) {
    companion object {
        val EMPTY = PoseModel(NodePoseModel.EMPTY, GpsPoseModel.EMPTY)
    }
}
