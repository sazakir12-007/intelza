package com.ht.intelza.data.db

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

data class ClassSummary(
    @Embedded val schoolClass: ClassEntity,
    val studentCount: Int,
    val lastSessionAt: Long?,
)

@Dao
interface ClassDao {
    @Query(
        """
        SELECT c.*,
            (SELECT COUNT(*) FROM students s WHERE s.classId = c.id AND s.active = 1) AS studentCount,
            (SELECT MAX(se.startedAt) FROM sessions se WHERE se.classId = c.id) AS lastSessionAt
        FROM classes c
        ORDER BY c.archived, c.name COLLATE NOCASE
        """,
    )
    fun observeSummaries(): Flow<List<ClassSummary>>

    @Query("SELECT * FROM classes WHERE id = :id")
    fun observe(id: Long): Flow<ClassEntity?>

    @Query("SELECT * FROM classes WHERE id = :id")
    suspend fun get(id: Long): ClassEntity?

    @Insert
    suspend fun insert(schoolClass: ClassEntity): Long

    @Update
    suspend fun update(schoolClass: ClassEntity)

    @Query("DELETE FROM classes WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface StudentDao {
    @Query("SELECT * FROM students WHERE classId = :classId AND active = 1 ORDER BY cardNumber")
    fun observeActive(classId: Long): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE classId = :classId AND active = 1 ORDER BY cardNumber")
    suspend fun getActive(classId: Long): List<StudentEntity>

    /** Every student who was ever in the class, current students first. */
    @Query("SELECT * FROM students WHERE classId = :classId ORDER BY active DESC, cardNumber, name")
    suspend fun getAll(classId: Long): List<StudentEntity>

    @Query("SELECT * FROM students WHERE id = :id")
    fun observe(id: Long): Flow<StudentEntity?>

    @Query("SELECT * FROM students WHERE id = :id")
    suspend fun get(id: Long): StudentEntity?

    @Insert
    suspend fun insert(student: StudentEntity): Long

    @Update
    suspend fun update(student: StudentEntity)

    /** Removes a student from the class but keeps their past results. */
    @Query("UPDATE students SET active = 0, cardNumber = NULL WHERE id = :id")
    suspend fun deactivate(id: Long)
}

data class SubjectSummary(
    @Embedded val subject: SubjectEntity,
    val topicCount: Int,
    val questionCount: Int,
)

@Dao
interface SubjectDao {
    @Query(
        """
        SELECT s.*,
            (SELECT COUNT(*) FROM topics t WHERE t.subjectId = s.id) AS topicCount,
            (SELECT COUNT(*) FROM questions q JOIN topics t ON t.id = q.topicId
                WHERE t.subjectId = s.id) AS questionCount
        FROM subjects s
        ORDER BY s.name COLLATE NOCASE
        """,
    )
    fun observeSummaries(): Flow<List<SubjectSummary>>

    @Query("SELECT * FROM subjects WHERE id = :id")
    fun observe(id: Long): Flow<SubjectEntity?>

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun get(id: Long): SubjectEntity?

    @Insert
    suspend fun insert(subject: SubjectEntity): Long

    @Update
    suspend fun update(subject: SubjectEntity)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun delete(id: Long)
}

data class TopicSummary(
    @Embedded val topic: TopicEntity,
    val questionCount: Int,
    val lastEvaluatedAt: Long?,
)

data class TopicWithSubject(
    @Embedded val topic: TopicEntity,
    val subjectName: String,
    val questionCount: Int,
)

@Dao
interface TopicDao {
    @Query(
        """
        SELECT t.*,
            (SELECT COUNT(*) FROM questions q WHERE q.topicId = t.id) AS questionCount,
            (SELECT MAX(s.startedAt) FROM sessions s WHERE s.topicId = t.id) AS lastEvaluatedAt
        FROM topics t
        WHERE t.subjectId = :subjectId
        ORDER BY t.createdAt DESC
        """,
    )
    fun observeSummaries(subjectId: Long): Flow<List<TopicSummary>>

    @Query(
        """
        SELECT t.*, s.name AS subjectName,
            (SELECT COUNT(*) FROM questions q WHERE q.topicId = t.id) AS questionCount
        FROM topics t JOIN subjects s ON s.id = t.subjectId
        ORDER BY s.name COLLATE NOCASE, t.createdAt DESC
        """,
    )
    fun observeAllWithSubject(): Flow<List<TopicWithSubject>>

    @Query(
        """
        SELECT t.*, s.name AS subjectName,
            (SELECT COUNT(*) FROM questions q WHERE q.topicId = t.id) AS questionCount
        FROM topics t JOIN subjects s ON s.id = t.subjectId
        WHERE t.id = :id
        """,
    )
    fun observeWithSubject(id: Long): Flow<TopicWithSubject?>

    @Query("SELECT * FROM topics WHERE id = :id")
    suspend fun get(id: Long): TopicEntity?

    @Insert
    suspend fun insert(topic: TopicEntity): Long

    @Update
    suspend fun update(topic: TopicEntity)

    @Query("DELETE FROM topics WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions WHERE topicId = :topicId ORDER BY position")
    fun observeForTopic(topicId: Long): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE topicId = :topicId ORDER BY position")
    suspend fun getForTopic(topicId: Long): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE id = :id")
    suspend fun get(id: Long): QuestionEntity?

    @Query("SELECT COALESCE(MAX(position), -1) FROM questions WHERE topicId = :topicId")
    suspend fun maxPosition(topicId: Long): Int

    @Insert
    suspend fun insert(question: QuestionEntity): Long

    @Update
    suspend fun update(question: QuestionEntity)

    @Update
    suspend fun updateAll(questions: List<QuestionEntity>)

    @Query("DELETE FROM questions WHERE id = :id")
    suspend fun delete(id: Long)

    /** Picture files still used by any question, past or present. */
    @Query(
        """
        SELECT imageName FROM questions WHERE imageName IS NOT NULL
        UNION
        SELECT imageName FROM session_questions WHERE imageName IS NOT NULL
        """,
    )
    suspend fun referencedImages(): List<String>
}

data class SessionStudentRow(
    val studentId: Long,
    val name: String,
    val rollNumber: String,
    val cardNumber: Int,
    val present: Boolean,
)

@Dao
interface SessionDao {
    @Insert
    suspend fun insertSession(session: SessionEntity): Long

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE id = :id")
    fun observeSession(id: Long): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSession(id: Long): SessionEntity?

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Insert
    suspend fun insertSessionStudents(students: List<SessionStudentEntity>)

    @Query(
        """
        SELECT ss.studentId, st.name, st.rollNumber, ss.cardNumber, ss.present
        FROM session_students ss JOIN students st ON st.id = ss.studentId
        WHERE ss.sessionId = :sessionId
        ORDER BY ss.cardNumber
        """,
    )
    fun observeStudents(sessionId: Long): Flow<List<SessionStudentRow>>

    @Query(
        """
        SELECT ss.studentId, st.name, st.rollNumber, ss.cardNumber, ss.present
        FROM session_students ss JOIN students st ON st.id = ss.studentId
        WHERE ss.sessionId = :sessionId
        ORDER BY ss.cardNumber
        """,
    )
    suspend fun getStudents(sessionId: Long): List<SessionStudentRow>

    @Query(
        "UPDATE session_students SET present = :present " +
            "WHERE sessionId = :sessionId AND studentId = :studentId",
    )
    suspend fun setPresent(sessionId: Long, studentId: Long, present: Boolean)

    @Insert
    suspend fun insertQuestions(questions: List<SessionQuestionEntity>)

    @Insert
    suspend fun insertQuestion(question: SessionQuestionEntity): Long

    @Update
    suspend fun updateQuestion(question: SessionQuestionEntity)

    @Query("DELETE FROM session_questions WHERE id = :id")
    suspend fun deleteQuestion(id: Long)

    @Query("SELECT * FROM session_questions WHERE sessionId = :sessionId ORDER BY position")
    fun observeQuestions(sessionId: Long): Flow<List<SessionQuestionEntity>>

    @Query("SELECT * FROM session_questions WHERE sessionId = :sessionId ORDER BY position")
    suspend fun getQuestions(sessionId: Long): List<SessionQuestionEntity>

    @Query("SELECT * FROM session_questions WHERE id = :id")
    suspend fun getQuestion(id: Long): SessionQuestionEntity?

    @Query(
        """
        SELECT r.* FROM responses r JOIN session_questions q ON q.id = r.sessionQuestionId
        WHERE q.sessionId = :sessionId
        """,
    )
    fun observeResponses(sessionId: Long): Flow<List<ResponseEntity>>

    @Query(
        """
        SELECT r.* FROM responses r JOIN session_questions q ON q.id = r.sessionQuestionId
        WHERE q.sessionId = :sessionId
        """,
    )
    suspend fun getResponses(sessionId: Long): List<ResponseEntity>

    @Upsert
    suspend fun upsertResponses(responses: List<ResponseEntity>)

    @Query("DELETE FROM responses WHERE sessionQuestionId = :sessionQuestionId AND studentId = :studentId")
    suspend fun deleteResponse(sessionQuestionId: Long, studentId: Long)

    @Query("DELETE FROM responses WHERE sessionQuestionId = :sessionQuestionId")
    suspend fun clearResponses(sessionQuestionId: Long)
}

/** One session with the counts needed for its class average. */
data class SessionOverview(
    val id: Long,
    val classId: Long,
    val className: String,
    val topicId: Long?,
    val title: String,
    val subjectName: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val totalQuestions: Int,
    val askedQuestions: Int,
    val scoredQuestions: Int,
    val presentStudents: Int,
    val correctAnswers: Int,
) {
    /** Class average from 0 to 1, or null when nothing was scored. */
    val average: Float?
        get() = if (scoredQuestions > 0 && presentStudents > 0) {
            correctAnswers.toFloat() / (scoredQuestions * presentStudents)
        } else {
            null
        }
}

/** One session from a student's point of view. */
data class StudentSessionRow(
    val sessionId: Long,
    val title: String,
    val subjectName: String,
    val startedAt: Long,
    val present: Boolean,
    val scoredQuestions: Int,
    val correctAnswers: Int,
)

/** A student's correct answers in one session, for class-wide tables. */
data class StudentSessionScore(
    val sessionId: Long,
    val studentId: Long,
    val present: Boolean,
    val correctAnswers: Int,
)

/** How one bank question has gone across every session that asked it. */
data class TopicQuestionStat(
    val questionKey: Long,
    val text: String,
    val timesAsked: Int,
    val correctAnswers: Int,
    val possibleAnswers: Int,
)

private const val SCORED = "q.status = 'ASKED' AND q.type != 'POLL' AND q.correct IS NOT NULL"

private const val SESSION_OVERVIEW = """
    SELECT s.id, s.classId, c.name AS className, s.topicId, s.title, s.subjectName,
        s.startedAt, s.finishedAt,
        (SELECT COUNT(*) FROM session_questions q WHERE q.sessionId = s.id) AS totalQuestions,
        (SELECT COUNT(*) FROM session_questions q
            WHERE q.sessionId = s.id AND q.status = 'ASKED') AS askedQuestions,
        (SELECT COUNT(*) FROM session_questions q WHERE q.sessionId = s.id AND $SCORED) AS scoredQuestions,
        (SELECT COUNT(*) FROM session_students ss
            WHERE ss.sessionId = s.id AND ss.present = 1) AS presentStudents,
        (SELECT COUNT(*) FROM responses r
            JOIN session_questions q ON q.id = r.sessionQuestionId
            JOIN session_students ss ON ss.sessionId = q.sessionId AND ss.studentId = r.studentId
            WHERE q.sessionId = s.id AND $SCORED AND r.answer = q.correct AND ss.present = 1
        ) AS correctAnswers
    FROM sessions s JOIN classes c ON c.id = s.classId
"""

@Dao
interface ReportDao {
    @Query("$SESSION_OVERVIEW ORDER BY s.startedAt DESC")
    fun observeSessions(): Flow<List<SessionOverview>>

    @Query("$SESSION_OVERVIEW WHERE s.finishedAt IS NULL ORDER BY s.startedAt DESC")
    fun observeUnfinishedSessions(): Flow<List<SessionOverview>>

    @Query("$SESSION_OVERVIEW WHERE s.classId = :classId ORDER BY s.startedAt DESC")
    fun observeSessionsForClass(classId: Long): Flow<List<SessionOverview>>

    @Query("$SESSION_OVERVIEW WHERE s.classId = :classId ORDER BY s.startedAt")
    suspend fun getSessionsForClass(classId: Long): List<SessionOverview>

    @Query("$SESSION_OVERVIEW WHERE s.topicId = :topicId ORDER BY s.startedAt DESC")
    fun observeSessionsForTopic(topicId: Long): Flow<List<SessionOverview>>

    @Query("$SESSION_OVERVIEW WHERE s.id = :sessionId")
    fun observeSession(sessionId: Long): Flow<SessionOverview?>

    @Query(
        """
        SELECT s.id AS sessionId, s.title, s.subjectName, s.startedAt, ss.present,
            (SELECT COUNT(*) FROM session_questions q WHERE q.sessionId = s.id AND $SCORED) AS scoredQuestions,
            (SELECT COUNT(*) FROM responses r JOIN session_questions q ON q.id = r.sessionQuestionId
                WHERE q.sessionId = s.id AND r.studentId = :studentId AND $SCORED
                    AND r.answer = q.correct) AS correctAnswers
        FROM session_students ss JOIN sessions s ON s.id = ss.sessionId
        WHERE ss.studentId = :studentId
        ORDER BY s.startedAt
        """,
    )
    fun observeStudentHistory(studentId: Long): Flow<List<StudentSessionRow>>

    @Query(
        """
        SELECT ss.sessionId, ss.studentId, ss.present,
            (SELECT COUNT(*) FROM responses r JOIN session_questions q ON q.id = r.sessionQuestionId
                WHERE q.sessionId = ss.sessionId AND r.studentId = ss.studentId AND $SCORED
                    AND r.answer = q.correct) AS correctAnswers
        FROM session_students ss JOIN sessions s ON s.id = ss.sessionId
        WHERE s.classId = :classId
        """,
    )
    suspend fun getClassScores(classId: Long): List<StudentSessionScore>

    @Query(
        """
        SELECT COALESCE(q.sourceQuestionId, -q.id) AS questionKey, q.text,
            COUNT(*) AS timesAsked,
            SUM((SELECT COUNT(*) FROM responses r
                JOIN session_students ss ON ss.sessionId = q.sessionId AND ss.studentId = r.studentId
                WHERE r.sessionQuestionId = q.id AND r.answer = q.correct AND ss.present = 1)) AS correctAnswers,
            SUM((SELECT COUNT(*) FROM session_students ss
                WHERE ss.sessionId = q.sessionId AND ss.present = 1)) AS possibleAnswers
        FROM session_questions q JOIN sessions s ON s.id = q.sessionId
        WHERE s.topicId = :topicId AND $SCORED
        GROUP BY COALESCE(q.sourceQuestionId, -q.id)
        ORDER BY MIN(q.position)
        """,
    )
    fun observeTopicQuestionStats(topicId: Long): Flow<List<TopicQuestionStat>>
}
