package com.ht.intelza.scan

import com.ht.intelza.domain.AnswerOption
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToInt

/**
 * Works out which answer a student is showing from how their card is turned.
 *
 * The card's A edge is its printed top; B, C and D follow clockwise. Whichever edge
 * points up in the real world is the answer, so the card direction is compared with the
 * direction of gravity rather than with the phone screen. That keeps answers correct
 * however the teacher holds the phone.
 */
object CardOrientation {

    /** Readings further than this from a clean quarter turn are ignored as ambiguous. */
    const val DEFAULT_TOLERANCE_DEGREES = 30f

    /**
     * @param cardUp direction of the card's printed top edge.
     * @param worldUp the real "up" direction, in the same coordinate system as [cardUp].
     * @return the answer shown, or null when the card is held too close to diagonal.
     */
    fun decode(
        cardUp: Vec2,
        worldUp: Vec2,
        toleranceDegrees: Float = DEFAULT_TOLERANCE_DEGREES,
    ): AnswerOption? {
        if (cardUp.length == 0f || worldUp.length == 0f) return null
        val up = worldUp.normalized()
        // Right-hand direction of the world as seen on screen (y points down).
        val right = Vec2(-up.y, up.x)
        val upness = cardUp dot up
        val rightness = cardUp dot right
        // 0 => A edge on top, -90 => B on top, +-180 => C on top, +90 => D on top.
        val angle = Math.toDegrees(atan2(rightness, upness).toDouble()).toFloat()
        val quarterTurns = (angle / 90f).roundToInt()
        if (abs(angle - quarterTurns * 90f) > toleranceDegrees) return null
        return when (((quarterTurns % 4) + 4) % 4) {
            0 -> AnswerOption.A
            3 -> AnswerOption.B
            2 -> AnswerOption.C
            else -> AnswerOption.D
        }
    }

    /**
     * Decodes a detection made in a raw camera buffer.
     *
     * @param sensorRotationDegrees clockwise rotation that turns the camera buffer upright
     *   in the device's natural (portrait) orientation.
     * @param worldUpNatural real "up" in natural-orientation screen coordinates.
     */
    fun decode(
        detection: TagDetection,
        sensorRotationDegrees: Int,
        worldUpNatural: Vec2,
        toleranceDegrees: Float = DEFAULT_TOLERANCE_DEGREES,
    ): AnswerOption? = decode(
        cardUp = detection.cardUp.rotateClockwise(sensorRotationDegrees),
        worldUp = worldUpNatural,
        toleranceDegrees = toleranceDegrees,
    )
}
