package com.ht.intelza.domain

import com.ht.intelza.data.db.SessionOverview
import com.ht.intelza.data.db.StudentEntity
import com.ht.intelza.data.db.StudentSessionRow
import com.ht.intelza.data.db.StudentSessionScore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClassProgressTest {

    private val asha = StudentEntity(id = 1, classId = 1, name = "Asha", cardNumber = 1)
    private val ben = StudentEntity(id = 2, classId = 1, name = "Ben", cardNumber = 2)
    private val left = StudentEntity(id = 3, classId = 1, name = "Left", cardNumber = null, active = false)
    private val neverHere = StudentEntity(id = 4, classId = 1, name = "Gone", cardNumber = null, active = false)

    private val sessions = listOf(overview(10, scored = 4), overview(11, scored = 2), overview(12, scored = 0))
    private val scores = listOf(
        StudentSessionScore(10, 1, present = true, correctAnswers = 3),
        StudentSessionScore(11, 1, present = true, correctAnswers = 1),
        StudentSessionScore(12, 1, present = true, correctAnswers = 0),
        StudentSessionScore(10, 2, present = false, correctAnswers = 0),
        StudentSessionScore(11, 2, present = true, correctAnswers = 2),
        StudentSessionScore(10, 3, present = true, correctAnswers = 4),
    )

    @Test
    fun `averages skip absences and unscored sessions`() {
        val progress = ClassProgress.compute(sessions, scores, listOf(asha, ben, left, neverHere))
        val byName = progress.associateBy { it.student.name }
        assertEquals(setOf("Asha", "Ben", "Left"), byName.keys)
        assertEquals(mapOf(10L to 0.75f, 11L to 0.5f), byName["Asha"]!!.scores)
        assertEquals(0.625f, byName["Asha"]!!.average!!, 0.001f)
        assertEquals(setOf(10L), byName["Ben"]!!.absentSessions)
        assertEquals(1f, byName["Ben"]!!.average!!, 0.001f)
        assertEquals(1f, byName["Left"]!!.average!!, 0.001f)
    }

    @Test
    fun `subject averages group sessions`() {
        val history = listOf(
            row("Maths", present = true, scored = 4, correct = 2),
            row("Maths", present = true, scored = 2, correct = 2),
            row("Science", present = false, scored = 4, correct = 0),
            row("", present = true, scored = 5, correct = 1),
        )
        val averages = ClassProgress.subjectAverages(history, quickLabel = "Quick")
        assertEquals(listOf("Maths", "Quick"), averages.map { it.subject })
        assertEquals(0.75f, averages[0].average, 0.001f)
        assertEquals(2, averages[0].sessions)
        assertEquals(0.2f, averages[1].average, 0.001f)
        assertEquals((0.5f + 1f + 0.2f) / 3, ClassProgress.overallAverage(history)!!, 0.001f)
        assertNull(ClassProgress.overallAverage(emptyList()))
    }

    private fun overview(id: Long, scored: Int) = SessionOverview(
        id = id, classId = 1, className = "3B", topicId = null, title = "T$id", subjectName = "Maths",
        startedAt = id, finishedAt = id, totalQuestions = scored, askedQuestions = scored,
        scoredQuestions = scored, presentStudents = 2, correctAnswers = 0,
    )

    private fun row(subject: String, present: Boolean, scored: Int, correct: Int) = StudentSessionRow(
        sessionId = 1, title = "t", subjectName = subject, startedAt = 0, present = present,
        scoredQuestions = scored, correctAnswers = correct,
    )
}
