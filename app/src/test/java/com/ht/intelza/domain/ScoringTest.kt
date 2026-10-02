package com.ht.intelza.domain

import com.ht.intelza.data.db.ResponseEntity
import com.ht.intelza.data.db.SessionQuestionEntity
import com.ht.intelza.data.db.SessionStudentRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScoringTest {

    private val asha = student(1, "Asha")
    private val ben = student(2, "Ben")
    private val chen = student(3, "Chen")
    private val dev = student(4, "Dev", present = false)
    private val students = listOf(asha, ben, chen, dev)

    private val q1 = question(10, position = 0, correct = AnswerOption.B)
    private val q2 = question(11, position = 1, correct = AnswerOption.A)
    private val poll = question(12, position = 2, type = QuestionType.POLL, correct = null)
    private val pending = question(13, position = 3, correct = AnswerOption.C, status = QuestionStatus.PENDING)
    private val skipped = question(14, position = 4, correct = AnswerOption.C, status = QuestionStatus.SKIPPED)

    private val responses = listOf(
        answer(q1, asha, AnswerOption.B),
        answer(q1, ben, AnswerOption.B),
        answer(q1, chen, AnswerOption.D),
        answer(q1, dev, AnswerOption.B), // absent students' answers are ignored
        answer(q2, asha, AnswerOption.A),
        // Ben did not answer q2.
        answer(q2, chen, AnswerOption.C),
        answer(poll, asha, AnswerOption.A),
        answer(poll, ben, AnswerOption.C),
    )

    private val report = Scoring.sessionReport(
        listOf(q1, q2, poll, pending, skipped),
        students,
        responses,
    )

    @Test
    fun `only asked questions are reported`() {
        assertEquals(listOf(10L, 11L, 12L), report.questions.map { it.question.id })
        assertEquals(2, report.scoredQuestionCount)
    }

    @Test
    fun `absent students are excluded`() {
        assertEquals(listOf("Asha", "Ben", "Chen"), report.students.map { it.student.name })
        assertEquals(listOf("Dev"), report.absent.map { it.name })
    }

    @Test
    fun `unanswered questions count as wrong`() {
        val scores = report.students.associate { it.student.name to it.score }
        assertEquals(1f, scores["Asha"]!!, 0.001f)
        assertEquals(0.5f, scores["Ben"]!!, 0.001f)
        assertEquals(0f, scores["Chen"]!!, 0.001f)
        assertEquals(0.5f, report.classAverage!!, 0.001f)
    }

    @Test
    fun `question results count present students only`() {
        val first = report.questions[0]
        assertEquals(mapOf(AnswerOption.A to 0, AnswerOption.B to 2, AnswerOption.C to 0, AnswerOption.D to 1), first.counts)
        assertEquals(2f / 3f, first.fractionCorrect!!, 0.001f)
        val second = report.questions[1]
        assertEquals(2, second.answered)
        assertEquals(listOf("Ben"), second.noAnswer.map { it.name })
    }

    @Test
    fun `polls are never scored`() {
        val pollResult = report.questions[2]
        assertNull(pollResult.fractionCorrect)
        assertEquals(1, pollResult.counts[AnswerOption.A])
        assertEquals(1, pollResult.counts[AnswerOption.C])
    }

    @Test
    fun `flags use the thresholds`() {
        assertEquals(listOf("Chen"), report.studentsNeedingHelp(50).map { it.student.name })
        assertEquals(listOf("Ben", "Chen"), report.studentsNeedingHelp(51).map { it.student.name })
        // q1 is 67% correct, q2 is 33% correct.
        assertEquals(listOf(11L), report.questionsToReteach(60).map { it.question.id })
        assertEquals(listOf(10L, 11L), report.questionsToReteach(70).map { it.question.id })
    }

    @Test
    fun `true false questions have two options`() {
        val tf = question(20, position = 0, type = QuestionType.TRUE_FALSE, correct = AnswerOption.A, optionCount = 4)
        val result = Scoring.questionResult(tf, students, listOf(answer(tf, asha, AnswerOption.A)))
        assertEquals(setOf(AnswerOption.A, AnswerOption.B), result.counts.keys)
        assertEquals(1f / 3f, result.fractionCorrect!!, 0.001f)
    }

    @Test
    fun `nothing scored gives no average`() {
        val onlyPoll = Scoring.sessionReport(listOf(poll), students, responses)
        assertNull(onlyPoll.classAverage)
        assertNull(onlyPoll.students.first().score)
    }

    private fun student(card: Int, name: String, present: Boolean = true) =
        SessionStudentRow(studentId = card.toLong(), name = name, rollNumber = "", cardNumber = card, present = present)

    private fun question(
        id: Long,
        position: Int,
        type: QuestionType = QuestionType.MULTIPLE_CHOICE,
        correct: AnswerOption?,
        status: QuestionStatus = QuestionStatus.ASKED,
        optionCount: Int = 4,
    ) = SessionQuestionEntity(
        id = id,
        sessionId = 1,
        position = position,
        sourceQuestionId = null,
        type = type,
        text = "Question $id",
        optionCount = optionCount,
        correct = correct,
        status = status,
    )

    private fun answer(question: SessionQuestionEntity, student: SessionStudentRow, option: AnswerOption) =
        ResponseEntity(sessionQuestionId = question.id, studentId = student.studentId, answer = option)
}
