package com.ht.intelza.data

import androidx.room.withTransaction
import com.ht.intelza.data.db.IntelzaDatabase
import com.ht.intelza.data.db.QuestionEntity
import com.ht.intelza.data.db.ResponseEntity
import com.ht.intelza.data.db.SessionEntity
import com.ht.intelza.data.db.SessionQuestionEntity
import com.ht.intelza.data.db.SessionStudentEntity
import com.ht.intelza.domain.AnswerOption
import com.ht.intelza.domain.QuestionStatus
import com.ht.intelza.domain.QuestionType

class SessionRepository(private val db: IntelzaDatabase) {
    private val sessions = db.sessionDao()

    /**
     * Starts an evaluation of [classId]. With a [topicId] the topic's questions are copied
     * into the session; without one it is a quick session and questions are added as asked.
     */
    suspend fun startSession(classId: Long, topicId: Long?): Long = db.withTransaction {
        val topic = topicId?.let { db.topicDao().get(it) }
        val subject = topic?.let { db.subjectDao().get(it.subjectId) }
        val sessionId = sessions.insertSession(
            SessionEntity(
                classId = classId,
                topicId = topic?.id,
                title = topic?.name.orEmpty(),
                subjectName = subject?.name.orEmpty(),
            ),
        )
        val roster = db.studentDao().getActive(classId).mapNotNull { student ->
            student.cardNumber?.let { SessionStudentEntity(sessionId, student.id, it) }
        }
        sessions.insertSessionStudents(roster)
        if (topic != null) {
            val questions = db.questionDao().getForTopic(topic.id)
            sessions.insertQuestions(questions.mapIndexed { index, q -> q.toSessionQuestion(sessionId, index) })
        }
        sessionId
    }

    fun observeSession(id: Long) = sessions.observeSession(id)

    fun observeQuestions(sessionId: Long) = sessions.observeQuestions(sessionId)

    fun observeStudents(sessionId: Long) = sessions.observeStudents(sessionId)

    fun observeResponses(sessionId: Long) = sessions.observeResponses(sessionId)

    suspend fun getSession(id: Long) = sessions.getSession(id)

    suspend fun getQuestions(sessionId: Long) = sessions.getQuestions(sessionId)

    suspend fun getStudents(sessionId: Long) = sessions.getStudents(sessionId)

    suspend fun getResponses(sessionId: Long) = sessions.getResponses(sessionId)

    suspend fun setCurrentPosition(sessionId: Long, position: Int) {
        sessions.getSession(sessionId)?.let { sessions.updateSession(it.copy(currentPosition = position)) }
    }

    suspend fun finish(sessionId: Long) {
        sessions.getSession(sessionId)?.let {
            sessions.updateSession(it.copy(finishedAt = it.finishedAt ?: System.currentTimeMillis()))
        }
    }

    /** Lets the teacher continue a session that was already finished. */
    suspend fun reopen(sessionId: Long) {
        sessions.getSession(sessionId)?.let { sessions.updateSession(it.copy(finishedAt = null)) }
    }

    suspend fun deleteSession(sessionId: Long) = sessions.deleteSession(sessionId)

    suspend fun markAsked(sessionQuestionId: Long) {
        sessions.getQuestion(sessionQuestionId)?.let {
            sessions.updateQuestion(
                it.copy(status = QuestionStatus.ASKED, askedAt = it.askedAt ?: System.currentTimeMillis()),
            )
        }
    }

    suspend fun markSkipped(sessionQuestionId: Long) = db.withTransaction {
        sessions.getQuestion(sessionQuestionId)?.let {
            sessions.clearResponses(it.id)
            sessions.updateQuestion(it.copy(status = QuestionStatus.SKIPPED))
        }
    }

    suspend fun markPending(sessionQuestionId: Long) {
        sessions.getQuestion(sessionQuestionId)?.let {
            sessions.updateQuestion(it.copy(status = QuestionStatus.PENDING))
        }
    }

    /** Saves answers read by the scanner (by student id). */
    suspend fun recordScannedAnswers(sessionQuestionId: Long, answers: Map<Long, AnswerOption>) {
        if (answers.isEmpty()) return
        val now = System.currentTimeMillis()
        sessions.upsertResponses(
            answers.map { (studentId, answer) ->
                ResponseEntity(sessionQuestionId, studentId, answer, manual = false, recordedAt = now)
            },
        )
    }

    /** Sets or (with a null [answer]) removes one student's answer by hand. */
    suspend fun setAnswer(sessionQuestionId: Long, studentId: Long, answer: AnswerOption?) {
        if (answer == null) {
            sessions.deleteResponse(sessionQuestionId, studentId)
        } else {
            sessions.upsertResponses(listOf(ResponseEntity(sessionQuestionId, studentId, answer, manual = true)))
        }
    }

    suspend fun clearAnswers(sessionQuestionId: Long) = sessions.clearResponses(sessionQuestionId)

    /** Adds an unprepared question at the end of the session. */
    suspend fun addQuickQuestion(
        sessionId: Long,
        type: QuestionType,
        optionCount: Int,
        text: String,
    ): Long = db.withTransaction {
        val position = sessions.getQuestions(sessionId).maxOfOrNull { it.position + 1 } ?: 0
        sessions.insertQuestion(
            SessionQuestionEntity(
                sessionId = sessionId,
                position = position,
                sourceQuestionId = null,
                type = type,
                text = text.trim(),
                optionCount = type.optionCount(optionCount),
                optionA = if (type == QuestionType.TRUE_FALSE) TRUE_TEXT else "",
                optionB = if (type == QuestionType.TRUE_FALSE) FALSE_TEXT else "",
                correct = null,
            ),
        )
    }

    /** Sets the correct answer after the fact (quick questions); null makes it a poll. */
    suspend fun setCorrectAnswer(sessionQuestionId: Long, correct: AnswerOption?) {
        val question = sessions.getQuestion(sessionQuestionId) ?: return
        val type = when {
            correct == null -> QuestionType.POLL
            question.type == QuestionType.POLL -> QuestionType.MULTIPLE_CHOICE
            else -> question.type
        }
        sessions.updateQuestion(question.copy(correct = correct, type = type))
    }

    suspend fun removeQuestion(sessionQuestionId: Long) = sessions.deleteQuestion(sessionQuestionId)

    suspend fun setPresent(sessionId: Long, studentId: Long, present: Boolean) =
        sessions.setPresent(sessionId, studentId, present)

    private fun QuestionEntity.toSessionQuestion(sessionId: Long, position: Int) = SessionQuestionEntity(
        sessionId = sessionId,
        position = position,
        sourceQuestionId = id,
        type = type,
        text = text,
        optionCount = type.optionCount(optionCount),
        optionA = optionA,
        optionB = optionB,
        optionC = optionC,
        optionD = optionD,
        correct = if (type.isScored) correct else null,
        imageName = imageName,
    )

    companion object {
        // Stored option labels for True/False questions; the UI shows localised text instead.
        const val TRUE_TEXT = "True"
        const val FALSE_TEXT = "False"
    }
}
