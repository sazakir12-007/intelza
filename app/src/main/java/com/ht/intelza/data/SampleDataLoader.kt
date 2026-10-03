package com.ht.intelza.data

import androidx.room.withTransaction
import com.ht.intelza.data.db.ClassEntity
import com.ht.intelza.data.db.IntelzaDatabase
import com.ht.intelza.data.db.QuestionEntity
import com.ht.intelza.data.db.StudentEntity
import com.ht.intelza.data.db.SubjectEntity
import com.ht.intelza.data.db.TopicEntity
import com.ht.intelza.domain.QuestionType

/** Adds or removes [SampleData]. Adding twice does nothing the second time. */
class SampleDataLoader(private val db: IntelzaDatabase) {

    data class Summary(val classes: Int, val students: Int, val topics: Int, val questions: Int) {
        val isEmpty: Boolean get() = classes == 0 && topics == 0
    }

    suspend fun add(): Summary = db.withTransaction {
        var classes = 0
        var students = 0
        var topics = 0
        var questions = 0

        val existingClasses = db.classDao().getAll().map { it.name }.toSet()
        for (sample in SampleData.classes) {
            if (sample.name in existingClasses) continue
            val classId = db.classDao().insert(ClassEntity(name = sample.name))
            sample.students.forEachIndexed { index, name ->
                db.studentDao().insert(
                    StudentEntity(
                        classId = classId,
                        name = name,
                        rollNumber = (index + 1).toString(),
                        cardNumber = index + 1,
                    ),
                )
            }
            classes++
            students += sample.students.size
        }

        val subjectIds = db.subjectDao().getAll().associate { it.name.lowercase() to it.id }.toMutableMap()
        for (sample in SampleData.topics) {
            val subjectId = subjectIds.getOrPut(sample.subject.lowercase()) {
                db.subjectDao().insert(SubjectEntity(name = sample.subject))
            }
            val exists = db.topicDao().getForSubject(subjectId).any { it.name == sample.name && it.grade == sample.grade }
            if (exists) continue
            val topicId = db.topicDao().insert(TopicEntity(subjectId = subjectId, name = sample.name, grade = sample.grade))
            sample.questions.forEachIndexed { index, question ->
                db.questionDao().insert(
                    QuestionEntity(
                        topicId = topicId,
                        position = index,
                        type = QuestionType.MULTIPLE_CHOICE,
                        text = question.text,
                        optionCount = question.options.size,
                        optionA = question.options[0],
                        optionB = question.options[1],
                        optionC = question.options[2],
                        optionD = question.options[3],
                        correct = question.correct,
                    ),
                )
            }
            topics++
            questions += sample.questions.size
        }
        Summary(classes, students, topics, questions)
    }

    /** Deletes the sample classes (with any results recorded for them) and the sample topics. */
    suspend fun remove(): Int = db.withTransaction {
        var removed = 0
        val classNames = SampleData.classes.map { it.name }.toSet()
        for (schoolClass in db.classDao().getAll().filter { it.name in classNames }) {
            db.classDao().delete(schoolClass.id)
            removed++
        }
        for (subject in db.subjectDao().getAll()) {
            val samples = SampleData.topics.filter { it.subject.equals(subject.name, ignoreCase = true) }
            if (samples.isEmpty()) continue
            for (topic in db.topicDao().getForSubject(subject.id)) {
                if (samples.any { it.name == topic.name && it.grade == topic.grade }) {
                    db.topicDao().delete(topic.id)
                    removed++
                }
            }
        }
        removed
    }
}
