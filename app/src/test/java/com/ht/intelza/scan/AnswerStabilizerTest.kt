package com.ht.intelza.scan

import com.ht.intelza.domain.AnswerOption.A
import com.ht.intelza.domain.AnswerOption.B
import com.ht.intelza.domain.AnswerOption.C
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerStabilizerTest {

    @Test
    fun `answer is confirmed after three matching frames`() {
        val stabilizer = AnswerStabilizer(requiredFrames = 3)
        assertTrue(stabilizer.update(mapOf(5 to A), 0).isEmpty())
        assertTrue(stabilizer.update(mapOf(5 to A), 100).isEmpty())
        assertEquals(mapOf(5 to A), stabilizer.update(mapOf(5 to A), 200))
        // Not reported again while it stays the same.
        assertTrue(stabilizer.update(mapOf(5 to A), 300).isEmpty())
        assertEquals(mapOf(5 to A), stabilizer.confirmed())
    }

    @Test
    fun `a card being turned is not confirmed`() {
        val stabilizer = AnswerStabilizer(requiredFrames = 3)
        stabilizer.update(mapOf(5 to A), 0)
        stabilizer.update(mapOf(5 to B), 100)
        stabilizer.update(mapOf(5 to C), 200)
        stabilizer.update(mapOf(5 to B), 300)
        assertTrue(stabilizer.confirmed().isEmpty())
    }

    @Test
    fun `latest stable answer wins`() {
        val stabilizer = AnswerStabilizer(requiredFrames = 2)
        stabilizer.update(mapOf(5 to A), 0)
        stabilizer.update(mapOf(5 to A), 100)
        stabilizer.update(mapOf(5 to C), 200)
        assertEquals(mapOf(5 to C), stabilizer.update(mapOf(5 to C), 300))
        assertEquals(mapOf(5 to C), stabilizer.confirmed())
    }

    @Test
    fun `streak restarts when a card disappears for too long`() {
        val stabilizer = AnswerStabilizer(requiredFrames = 2, maxGapMillis = 500)
        stabilizer.update(mapOf(5 to A), 0)
        assertTrue(stabilizer.update(mapOf(5 to A), 2_000).isEmpty())
        assertEquals(mapOf(5 to A), stabilizer.update(mapOf(5 to A), 2_100))
    }

    @Test
    fun `seeded answers are not reported again`() {
        val stabilizer = AnswerStabilizer(requiredFrames = 2)
        stabilizer.seed(mapOf(5 to A, 6 to B))
        assertTrue(stabilizer.update(mapOf(5 to A), 0).isEmpty())
        assertTrue(stabilizer.update(mapOf(5 to A), 100).isEmpty())
        stabilizer.update(mapOf(6 to C), 100)
        assertEquals(mapOf(6 to C), stabilizer.update(mapOf(6 to C), 200))
    }

    @Test
    fun `cards are tracked independently`() {
        val stabilizer = AnswerStabilizer(requiredFrames = 2)
        stabilizer.update(mapOf(1 to A, 2 to B), 0)
        assertEquals(mapOf(1 to A, 2 to B), stabilizer.update(mapOf(1 to A, 2 to B), 100))
    }
}
