package com.arvrlab.vps_sdk.util

import kotlin.math.PI

private const val FP32_SIGN_SHIFT = 31
private const val FP32_EXPONENT_SHIFT = 23
private const val FP32_SHIFTED_EXPONENT_MASK = 0xff
private const val FP32_SIGNIFICAND_MASK = 0x7fffff
private const val FP32_EXPONENT_BIAS = 127
private const val EXPONENT_BIAS = 15
private const val SIGN_SHIFT = 15
private const val EXPONENT_SHIFT = 10

private const val RAD_2_DEG = 180.0 / PI
private const val DEG_2_RAD = PI / 180.0

/**
 * <p>Converts the specified single-precision float value into a
 * half-precision float value. The following special cases are handled:</p>
 * <ul>
 * <li>If the input is NaN (see {@link Float#isNaN(float)}), the returned
 * value is {@link #NaN}</li>
 * <li>If the input is {@link Float#POSITIVE_INFINITY} or
 * {@link Float#NEGATIVE_INFINITY}, the returned value is respectively
 * {@link #POSITIVE_INFINITY} or {@link #NEGATIVE_INFINITY}</li>
 * <li>If the input is 0 (positive or negative), the returned value is
 * {@link #POSITIVE_ZERO} or {@link #NEGATIVE_ZERO}</li>
 * <li>If the input is a less than {@link #MIN_VALUE}, the returned value
 * is flushed to {@link #POSITIVE_ZERO} or {@link #NEGATIVE_ZERO}</li>
 * <li>If the input is a less than {@link #MIN_NORMAL}, the returned value
 * is a denorm half-precision float</li>
 * <li>Otherwise, the returned value is rounded to the nearest
 * representable half-precision float value</li>
 * </ul>
 *
 * @param f The single-precision float value to convert to half-precision
 * @return A half-precision float value
 *
 * source: libcore/util/FP16.java
 */
fun Float.toHalf(): Short {
    val bits = this.toBits()
    val s = bits ushr FP32_SIGN_SHIFT
    var e = bits ushr FP32_EXPONENT_SHIFT and FP32_SHIFTED_EXPONENT_MASK
    var m = bits and FP32_SIGNIFICAND_MASK
    var outE = 0
    var outM = 0
    if (e == 0xff) { // Infinite or NaN
        outE = 0x1f
        outM = if (m != 0) 0x200 else 0
    } else {
        e = e - FP32_EXPONENT_BIAS + EXPONENT_BIAS
        if (e >= 0x1f) { // Overflow
            outE = 0x1f
        } else if (e <= 0) { // Underflow
            if (e < -10) {
                // The absolute fp32 value is less than MIN_VALUE, flush to +/-0
            } else {
                // The fp32 value is a normalized float less than MIN_NORMAL,
                // we convert to a denorm fp16
                m = m or 0x800000
                val shift = 14 - e
                outM = m shr shift
                val lowm = m and (1 shl shift) - 1
                val hway = 1 shl shift - 1
                // if above halfway or exactly halfway and outM is odd
                if (lowm + (outM and 1) > hway) {
                    // Round to nearest even
                    // Can overflow into exponent bit, which surprisingly is OK.
                    // This increment relies on the +outM in the return statement below
                    outM++
                }
            }
        } else {
            outE = e
            outM = m shr 13
            // if above halfway or exactly halfway and outM is odd
            if ((m and 0x1fff) + (outM and 0x1) > 0x1000) {
                // Round to nearest even
                // Can overflow into exponent bit, which surprisingly is OK.
                // This increment relies on the +outM in the return statement below
                outM++
            }
        }
    }
    // The outM is added here as the +1 increments for outM above can
    // cause an overflow in the exponent bit which is OK.
    return (s shl SIGN_SHIFT or (outE shl EXPONENT_SHIFT) + outM).toShort()
}

fun Float.toDegrees(): Float =
    (this * RAD_2_DEG).toFloat()

fun Float.toRadians(): Float =
    (this * DEG_2_RAD).toFloat()