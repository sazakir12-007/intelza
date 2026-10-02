package com.ht.intelza.scan

import kotlin.math.sqrt

/** A 2D point or direction in image coordinates (x to the right, y downwards). */
data class Vec2(val x: Float, val y: Float) {
    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2) = Vec2(x - other.x, y - other.y)
    operator fun times(factor: Float) = Vec2(x * factor, y * factor)

    infix fun dot(other: Vec2): Float = x * other.x + y * other.y

    val length: Float get() = sqrt(x * x + y * y)

    fun normalized(): Vec2 {
        val len = length
        return if (len == 0f) this else Vec2(x / len, y / len)
    }

    /**
     * Rotates this direction clockwise as it appears on screen (y points down) by a
     * multiple of 90 degrees.
     */
    fun rotateClockwise(degrees: Int): Vec2 = when (((degrees % 360) + 360) % 360) {
        0 -> this
        90 -> Vec2(-y, x)
        180 -> Vec2(-x, -y)
        270 -> Vec2(y, -x)
        else -> throw IllegalArgumentException("Rotation must be a multiple of 90: $degrees")
    }

    companion object {
        val UP = Vec2(0f, -1f)

        fun midpoint(a: Vec2, b: Vec2) = Vec2((a.x + b.x) / 2f, (a.y + b.y) / 2f)
    }
}
