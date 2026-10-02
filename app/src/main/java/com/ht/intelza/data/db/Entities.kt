package com.ht.intelza.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ht.intelza.domain.AnswerOption
import com.ht.intelza.domain.QuestionStatus
import com.ht.intelza.domain.QuestionType

@Entity(tableName = "classes")
data class ClassEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * A student in a class. Removed students are kept (inactive, without a card) so their
 * past results stay in reports.
 */
@Entity(
    tableName = "students",
    foreignKeys = [
        ForeignKey(
            entity = ClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("classId"), Index(value = ["classId", "cardNumber"], unique = true)],
)
data class StudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val classId: Long,
    val name: String,
    val rollNumber: String = "",
    val cardNumber: Int?,
    val active: Boolean = true,
)

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

@Entity(
    tableName = "topics",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("subjectId")],
)
data class TopicEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val name: String,
    val grade: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "questions",
    foreignKeys = [
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("topicId")],
)
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val topicId: Long,
    val position: Int,
    val type: QuestionType,
    val text: String,
    val optionCount: Int,
    val optionA: String = "",
    val optionB: String = "",
    val optionC: String = "",
    val optionD: String = "",
    /** Null for polls. */
    val correct: AnswerOption?,
    /** File name inside the app's image folder, if the question has a picture. */
    val imageName: String? = null,
)

/**
 * One evaluation of a class. Topic and subject names are copied in so the history stays
 * readable after the question bank changes.
 */
@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = ClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("classId"), Index("topicId")],
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val classId: Long,
    val topicId: Long?,
    val title: String,
    val subjectName: String,
    val startedAt: Long = System.currentTimeMillis(),
    val finishedAt: Long? = null,
    val currentPosition: Int = 0,
)

/** The class roster as it was when the session started, with attendance. */
@Entity(
    tableName = "session_students",
    primaryKeys = ["sessionId", "studentId"],
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("studentId")],
)
data class SessionStudentEntity(
    val sessionId: Long,
    val studentId: Long,
    val cardNumber: Int,
    val present: Boolean = true,
)

/** A question as asked in a session (a copy, so later edits don't change past results). */
@Entity(
    tableName = "session_questions",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId")],
)
data class SessionQuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val position: Int,
    val sourceQuestionId: Long?,
    val type: QuestionType,
    val text: String,
    val optionCount: Int,
    val optionA: String = "",
    val optionB: String = "",
    val optionC: String = "",
    val optionD: String = "",
    val correct: AnswerOption?,
    val imageName: String? = null,
    val status: QuestionStatus = QuestionStatus.PENDING,
    val askedAt: Long? = null,
)

@Entity(
    tableName = "responses",
    primaryKeys = ["sessionQuestionId", "studentId"],
    foreignKeys = [
        ForeignKey(
            entity = SessionQuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionQuestionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("studentId")],
)
data class ResponseEntity(
    val sessionQuestionId: Long,
    val studentId: Long,
    val answer: AnswerOption,
    /** True when the teacher entered or corrected the answer by hand. */
    val manual: Boolean = false,
    val recordedAt: Long = System.currentTimeMillis(),
)
