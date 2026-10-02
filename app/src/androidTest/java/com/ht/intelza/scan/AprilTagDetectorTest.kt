package com.ht.intelza.scan

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ht.intelza.domain.AnswerOption
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer

/**
 * Runs the native detector on cards rendered in software, checking card numbers and
 * that each quarter turn reads as the right answer.
 */
@RunWith(AndroidJUnit4::class)
class AprilTagDetectorTest {

    private val detector = AprilTagDetector(AprilTagDetector.Config(decimate = 1f))

    @After
    fun tearDown() = detector.close()

    @Test
    fun familyHasExpectedNumberOfCodes() {
        assertEquals(587, AprilTagDetector.codeCount)
        assertEquals(10, AprilTagDetector.tagCells(1).size)
    }

    @Test
    fun readsCardNumberAndAnswerForEveryQuarterTurn() {
        // Turning the printed card clockwise brings the D, C and B edges to the top in turn.
        val expected = mapOf(0 to AnswerOption.A, 1 to AnswerOption.D, 2 to AnswerOption.C, 3 to AnswerOption.B)
        for (id in listOf(1, 7, 42, 586)) {
            for ((quarterTurns, answer) in expected) {
                val cells = rotateClockwise(AprilTagDetector.tagCells(id), quarterTurns)
                val image = render(cells)
                val detections = detector.detect(image, IMAGE_SIZE, IMAGE_SIZE, IMAGE_SIZE)
                assertEquals("card $id turned $quarterTurns", 1, detections.size)
                val detection = detections.single()
                assertEquals(id, detection.id)
                assertEquals(
                    "card $id turned $quarterTurns",
                    answer,
                    CardOrientation.decode(detection, sensorRotationDegrees = 0, worldUpNatural = Vec2.UP),
                )
            }
        }
    }

    @Test
    fun cornersStartBottomLeftForAnUprightCard() {
        val detection = detector.detect(render(AprilTagDetector.tagCells(3)), IMAGE_SIZE, IMAGE_SIZE, IMAGE_SIZE).single()
        val (bottomLeft, bottomRight, topRight, topLeft) = detection.corners
        assertTrue(topLeft.y < bottomLeft.y)
        assertTrue(topRight.y < bottomRight.y)
        assertTrue(topLeft.x < topRight.x)
        assertTrue(bottomLeft.x < bottomRight.x)
    }

    private fun rotateClockwise(cells: Array<BooleanArray>, quarterTurns: Int): Array<BooleanArray> {
        var result = cells
        repeat(quarterTurns) {
            val n = result.size
            val source = result
            result = Array(n) { y -> BooleanArray(n) { x -> source[n - 1 - x][y] } }
        }
        return result
    }

    /** Draws the cells, 20 px each, in the middle of a white square image. */
    private fun render(cells: Array<BooleanArray>): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(IMAGE_SIZE * IMAGE_SIZE)
        val cellPx = 20
        val offset = (IMAGE_SIZE - cells.size * cellPx) / 2
        for (y in 0 until IMAGE_SIZE) {
            for (x in 0 until IMAGE_SIZE) {
                val cx = (x - offset).floorDiv(cellPx)
                val cy = (y - offset).floorDiv(cellPx)
                val black = cy in cells.indices && cx in cells.indices && cells[cy][cx]
                buffer.put(y * IMAGE_SIZE + x, if (black) BLACK else WHITE)
            }
        }
        return buffer
    }

    private companion object {
        const val IMAGE_SIZE = 400
        const val BLACK: Byte = 0
        const val WHITE: Byte = -1 // 255 as an unsigned pixel
    }
}
