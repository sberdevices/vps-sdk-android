package com.arvrlab.vps_sdk.util

import com.google.ar.sceneform.math.MathHelper
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import org.junit.Assert
import org.junit.Test
import kotlin.math.abs

class QuaternionExtensionTest {

    @Test
    fun checkGetEulerAnglesFromQuaternion() {
        val eulerList = listOf(
            Vector3(30f, 60f, 90f),
            Vector3(41f, -23f, 140f),
            Vector3(13f, 42f, 245f),
            Vector3(53f, 6f, 9f),
            Vector3(153f, 46f, 79f),
            Vector3(0f, 90f, 180f)
        )

        eulerList.forEach { euler ->
            val q1 = Quaternion.eulerAngles(euler)
            val eulerFromQuaternion = q1.getEulerAngles()
            val q2 = Quaternion.eulerAngles(eulerFromQuaternion)

            Assert.assertTrue("$q1 != $q2", equalQuaternion(q1, q2))
        }
    }

    /**
     * Compare two Quaternions
     *
     * Tests for equality by calculating the dot product of lhs and rhs.
     * lhs == -lhs
     *
     */
    private fun equalQuaternion(lhs: Quaternion, rhs: Quaternion): Boolean {
        val dot = abs(lhs.x * rhs.x + lhs.y * rhs.y + lhs.z * rhs.z + lhs.w * rhs.w)
        return MathHelper.almostEqualRelativeAndAbs(dot, 1.0f)
    }

}