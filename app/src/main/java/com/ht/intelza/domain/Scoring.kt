package com.ht.intelza.domain

import com.ht.intelza.data.db.ResponseEntity
import com.ht.intelza.data.db.SessionQuestionEntity
import com.ht.intelza.data.db.SessionStudentRow

/** Results of one asked question among the students present. */
data class QuestionResult(
    val question: SessionQuestionEntity,
    val counts: Map<AnswerOption, Int>,
    val answered: Int,
    val present: Int,
    /** Null for polls and questions without a correct answer. */
    val fractionCorrect: Float?,
    val studentsByAnswer: Map<AnswerOption, List<SessionStudentRow>>,
    val noAnswer: List<SessionStudentRow>,
) {
    val isScored: Boolean get() = fractionCorrect != null
    val correctCount: Int get() = question.correct?.let { counts[it] } ?: 0
}

/** One present student's results in a session. */
data class StudentResult(
    val student: SessionStudentRow,
    val answers: Map<Long, AnswerOption>,
    val correct: Int,
    val scoredQuestions: Int,
) {
    /** From 0 to 1, or null when no scored question was asked. */
    val score: Float? get() = if (scoredQuestions > 0) correct.toFloat() / scoredQuestions else null
}

data class SessionReport(
    /** Asked questions, in order. */
    val questions: List<QuestionResult>,
    /** Present students, by card number. */
    val students: List<StudentResult>,
    val absent: List<SessionStudentRow>,
) {
    val scoredQuestionCount: Int get() = questions.count { it.isScored }

    /** Mean student score from 0 to 1, or null when nothing was scored. */
    val classAverage: Float?
        get() = students.mapNotNull { it.score }.takeIf { it.isNotEmpty() }?.average()?.toFloat()

    fun studentsNeedingHelp(thresholdPercent: Int): List<StudentResult> =
        students.filter { result -> result.score?.let { it * 100 < thresholdPercent } == true }

    fun questionsToReteach(thresholdPercent: Int): List<QuestionResult> =
        questions.filter { result -> result.fractionCorrect?.let { it * 100 < thresholdPercent } == true }
}

/**
 * Scoring rules (requirement F6): an unanswered question counts as not correct, absent
 * students are left out, and polls are never scored.
 */
object Scoring {

    fun isScored(question: SessionQuestionEntity): Boolean =
        question.status == QuestionStatus.ASKED && question.type.isScored && question.correct != null

    fun questionResult(
        question: SessionQuestionEntity,
        students: List<SessionStudentRow>,
        responses: List<ResponseEntity>,
    ): QuestionResult {
        val present = students.filter { it.present }
        val answers = responses
            .filter { it.sessionQuestionId == question.id }
            .associate { it.studentId to it.answer }
        val options = AnswerOption.firstN(question.type.optionCount(question.optionCount))
        val byAnswer = options.associateWith { option ->
            present.filter { answers[it.studentId] == option }
        }
        val answered = present.count { it.studentId in answers }
        val correct = question.correct
        val fraction = if (question.type.isScored && correct != null && present.isNotEmpty()) {
            present.count { answers[it.studentId] == correct }.toFloat() / present.size
        } else {
            null
        }
        return QuestionResult(
            question = question,
            counts = byAnswer.mapValues { it.value.size },
            answered = answered,
            present = present.size,
            fractionCorrect = fraction,
            studentsByAnswer = byAnswer,
            noAnswer = present.filter { it.studentId !in answers },
        )
    }

    fun sessionReport(
        questions: List<SessionQuestionEntity>,
        students: List<SessionStudentRow>,
        responses: List<ResponseEntity>,
    ): SessionReport {
        val asked = questions.filter { it.status == QuestionStatus.ASKED }.sortedBy { it.position }
        val scored = asked.filter(::isScored)
        val answersByStudent = responses.groupBy { it.studentId }
            .mapValues { (_, list) -> list.associate { it.sessionQuestionId to it.answer } }
        val present = students.filter { it.present }.sortedBy { it.cardNumber }
        return SessionReport(
            questions = asked.map { questionResult(it, students, responses) },
            students = present.map { student ->
                val answers = answersByStudent[student.studentId].orEmpty()
                StudentResult(
                    student = student,
                    answers = answers.filterKeys { id -> asked.any { it.id == id } },
                    correct = scored.count { answers[it.id] == it.correct },
                    scoredQuestions = scored.size,
                )
            },
            absent = students.filterNot { it.present }.sortedBy { it.cardNumber },
        )
    }
}
