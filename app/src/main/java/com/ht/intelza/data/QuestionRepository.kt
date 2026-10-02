package com.ht.intelza.data

import androidx.room.withTransaction
import com.ht.intelza.data.db.IntelzaDatabase
import com.ht.intelza.data.db.QuestionEntity
import com.ht.intelza.data.db.SubjectEntity
import com.ht.intelza.data.db.TopicEntity

class QuestionRepository(
    private val db: IntelzaDatabase,
    private val images: ImageStore,
) {
    private val subjects = db.subjectDao()
    private val topics = db.topicDao()
    private val questions = db.questionDao()

    // Subjects

    fun observeSubjects() = subjects.observeSummaries()

    fun observeSubject(id: Long) = subjects.observe(id)

    suspend fun addSubject(name: String): Long = subjects.insert(SubjectEntity(name = name.trim()))

    suspend fun renameSubject(id: Long, name: String) {
        subjects.get(id)?.let { subjects.update(it.copy(name = name.trim())) }
    }

    suspend fun deleteSubject(id: Long) {
        subjects.delete(id)
        cleanUpImages()
    }

    // Topics

    fun observeTopics(subjectId: Long) = topics.observeSummaries(subjectId)

    fun observeAllTopics() = topics.observeAllWithSubject()

    fun observeTopic(id: Long) = topics.observeWithSubject(id)

    suspend fun addTopic(subjectId: Long, name: String, grade: String): Long =
        topics.insert(TopicEntity(subjectId = subjectId, name = name.trim(), grade = grade.trim()))

    suspend fun updateTopic(id: Long, name: String, grade: String) {
        topics.get(id)?.let { topics.update(it.copy(name = name.trim(), grade = grade.trim())) }
    }

    suspend fun deleteTopic(id: Long) {
        topics.delete(id)
        cleanUpImages()
    }

    // Questions

    fun observeQuestions(topicId: Long) = questions.observeForTopic(topicId)

    suspend fun getQuestion(id: Long) = questions.get(id)

    /** Inserts a new question at the end of its topic, or updates an existing one. */
    suspend fun saveQuestion(question: QuestionEntity): Long {
        val id = if (question.id == 0L) {
            db.withTransaction {
                questions.insert(question.copy(position = questions.maxPosition(question.topicId) + 1))
            }
        } else {
            questions.update(question)
            question.id
        }
        cleanUpImages()
        return id
    }

    suspend fun deleteQuestion(id: Long) {
        val question = questions.get(id) ?: return
        db.withTransaction {
            questions.delete(id)
            renumber(question.topicId)
        }
        cleanUpImages()
    }

    /** Inserts a copy right after the original. */
    suspend fun duplicateQuestion(id: Long): Long? = db.withTransaction {
        val original = questions.get(id) ?: return@withTransaction null
        val all = questions.getForTopic(original.topicId)
        val copyId = questions.insert(original.copy(id = 0, position = all.size))
        val copy = questions.get(copyId)!!
        val ordered = all.toMutableList().apply { add(indexOfFirst { it.id == id } + 1, copy) }
        questions.updateAll(ordered.mapIndexed { index, q -> q.copy(position = index) })
        copyId
    }

    suspend fun moveQuestion(topicId: Long, from: Int, to: Int) = db.withTransaction {
        val ordered = questions.getForTopic(topicId).toMutableList()
        if (from !in ordered.indices || to !in ordered.indices) return@withTransaction
        ordered.add(to, ordered.removeAt(from))
        questions.updateAll(ordered.mapIndexed { index, q -> q.copy(position = index) })
    }

    private suspend fun renumber(topicId: Long) {
        val ordered = questions.getForTopic(topicId)
        questions.updateAll(ordered.mapIndexed { index, q -> q.copy(position = index) })
    }

    private suspend fun cleanUpImages() {
        images.deleteUnreferenced(questions.referencedImages())
    }
}
