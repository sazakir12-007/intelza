package com.ht.intelza.data

import com.ht.intelza.data.db.IntelzaDatabase
import com.ht.intelza.domain.Scoring
import com.ht.intelza.domain.SessionReport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class ReportRepository(
    db: IntelzaDatabase,
    private val sessions: SessionRepository,
) {
    private val reports = db.reportDao()

    fun observeSessions() = reports.observeSessions()

    fun observeUnfinishedSessions() = reports.observeUnfinishedSessions()

    fun observeSessionsForClass(classId: Long) = reports.observeSessionsForClass(classId)

    suspend fun getSessionsForClass(classId: Long) = reports.getSessionsForClass(classId)

    fun observeSessionsForTopic(topicId: Long) = reports.observeSessionsForTopic(topicId)

    fun observeSessionOverview(sessionId: Long) = reports.observeSession(sessionId)

    suspend fun getSessionOverview(sessionId: Long) = reports.observeSession(sessionId).first()

    fun observeStudentHistory(studentId: Long) = reports.observeStudentHistory(studentId)

    fun observeTopicQuestionStats(topicId: Long) = reports.observeTopicQuestionStats(topicId)

    suspend fun getClassScores(classId: Long) = reports.getClassScores(classId)

    fun observeSessionReport(sessionId: Long): Flow<SessionReport> = combine(
        sessions.observeQuestions(sessionId),
        sessions.observeStudents(sessionId),
        sessions.observeResponses(sessionId),
        Scoring::sessionReport,
    )

    suspend fun getSessionReport(sessionId: Long): SessionReport = Scoring.sessionReport(
        sessions.getQuestions(sessionId),
        sessions.getStudents(sessionId),
        sessions.getResponses(sessionId),
    )
}
