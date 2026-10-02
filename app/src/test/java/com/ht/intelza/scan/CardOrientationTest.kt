package com.ht.intelza.scan

import com.ht.intelza.domain.AnswerOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class CardOrientationTest {

    private val up = Vec2.UP
    private val down = Vec2(0f, 1f)
    private val left = Vec2(-1f, 0f)
    private val right = Vec2(1f, 0f)

    @Test
    fun `edge pointing up is the answer`() {
        assertEquals(AnswerOption.A, CardOrientation.decode(cardUp = up, worldUp = up))
        assertEquals(AnswerOption.B, CardOrientation.decode(cardUp = left, worldUp = up))
        assertEquals(AnswerOption.C, CardOrientation.decode(cardUp = down, worldUp = up))
        assertEquals(AnswerOption.D, CardOrientation.decode(cardUp = right, worldUp = up))
    }

    @Test
    fun `answer follows gravity not the phone screen`() {
        // Phone turned to landscape: the real "up" now points to the screen's right edge.
        val worldUp = right
        assertEquals(AnswerOption.A, CardOrientation.decode(cardUp = right, worldUp = worldUp))
        assertEquals(AnswerOption.B, CardOrientation.decode(cardUp = up, worldUp = worldUp))
        assertEquals(AnswerOption.C, CardOrientation.decode(cardUp = left, worldUp = worldUp))
        assertEquals(AnswerOption.D, CardOrientation.decode(cardUp = down, worldUp = worldUp))
    }

    @Test
    fun `slightly tilted cards are still read`() {
        assertEquals(AnswerOption.A, CardOrientation.decode(rotated(up, 25f), up))
        assertEquals(AnswerOption.A, CardOrientation.decode(rotated(up, -25f), up))
        assertEquals(AnswerOption.D, CardOrientation.decode(rotated(right, 20f), up))
    }

    @Test
    fun `diagonal cards are ignored`() {
        assertNull(CardOrientation.decode(rotated(up, 45f), up))
        assertNull(CardOrientation.decode(rotated(up, -40f), up))
    }

    @Test
    fun `buffer detections are rotated by the sensor orientation`() {
        // Typical phone: the back camera buffer must be turned 90 degrees clockwise to be
        // upright in portrait, so an upright card points left in the raw buffer.
        val detection = square(center = Vec2(500f, 300f), up = left, halfSize = 40f)
        assertEquals(
            AnswerOption.A,
            CardOrientation.decode(detection, sensorRotationDegrees = 90, worldUpNatural = up),
        )
        val turnedToB = square(center = Vec2(500f, 300f), up = down, halfSize = 40f)
        assertEquals(
            AnswerOption.B,
            CardOrientation.decode(turnedToB, sensorRotationDegrees = 90, worldUpNatural = up),
        )
    }

    @Test
    fun `card up uses the top edge of the detection`() {
        val detection = square(center = Vec2(10f, 10f), up = up, halfSize = 5f)
        assertEquals(Vec2(0f, -5f), detection.cardUp)
    }

    /** Rotates [v] clockwise on screen by [degrees]. */
    private fun rotated(v: Vec2, degrees: Float): Vec2 {
        val r = Math.toRadians(degrees.toDouble())
        val c = cos(r).toFloat()
        val s = sin(r).toFloat()
        return Vec2(v.x * c - v.y * s, v.x * s + v.y * c)
    }

    private fun square(center: Vec2, up: Vec2, halfSize: Float): TagDetection {
        val u = up * halfSize
        val r = Vec2(-up.y, up.x) * halfSize
        return TagDetection(
            id = 1,
            hamming = 0,
            decisionMargin = 100f,
            center = center,
            corners = listOf(
                center - u - r, // bottom-left
                center - u + r, // bottom-right
                center + u + r, // top-right
                center + u - r, // top-left
            ),
        )
    }
}
