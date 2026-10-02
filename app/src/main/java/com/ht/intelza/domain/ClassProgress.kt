package com.ht.intelza.domain

import com.ht.intelza.data.db.SessionOverview
import com.ht.intelza.data.db.StudentEntity
import com.ht.intelza.data.db.StudentSessionRow
import com.ht.intelza.data.db.StudentSessionScore

/** One student's scores across a class's sessions. */
data class StudentProgress(
    val student: StudentEntity,
    /** Score (0–1) by session id; missing when absent or nothing was scored. */
    val scores: Map<Long, Float>,
    val absentSessions: Set<Long>,
) {
    val average: Float? get() = scores.values.takeIf { it.isNotEmpty() }?.average()?.toFloat()
}

data class SubjectAverage(val subject: String, val average: Float, val sessions: Int)

object ClassProgress {

    fun compute(
        sessions: List<SessionOverview>,
        scores: List<StudentSessionScore>,
        students: List<StudentEntity>,
    ): List<StudentProgress> {
        val scoredBySession = sessions.associate { it.id to it.scoredQuestions }
        val byStudent = scores.groupBy { it.studentId }
        return students
            .filter { it.active || byStudent.containsKey(it.id) }
            .map { student ->
                val rows = byStudent[student.id].orEmpty()
                StudentProgress(
                    student = student,
                    scores = rows.mapNotNull { row ->
                        val scored = scoredBySession[row.sessionId] ?: 0
                        if (row.present && scored > 0) row.sessionId to row.correctAnswers.toFloat() / scored else null
                    }.toMap(),
                    absentSessions = rows.filterNot { it.present }.map { it.sessionId }.toSet(),
                )
            }
    }

    /** Average score per subject over the sessions a student attended (requirement F3). */
    fun subjectAverages(history: List<StudentSessionRow>, quickLabel: String): List<SubjectAverage> =
        history
            .filter { it.present && it.scoredQuestions > 0 }
            .groupBy { it.subjectName.ifBlank { quickLabel } }
            .map { (subject, rows) ->
                SubjectAverage(
                    subject = subject,
                    average = rows.map { it.correctAnswers.toFloat() / it.scoredQuestions }.average().toFloat(),
                    sessions = rows.size,
                )
            }
            .sortedBy { it.subject.lowercase() }

    fun overallAverage(history: List<StudentSessionRow>): Float? =
        history.filter { it.present && it.scoredQuestions > 0 }
            .map { it.correctAnswers.toFloat() / it.scoredQuestions }
            .takeIf { it.isNotEmpty() }
            ?.average()?.toFloat()
}
