package com.ht.intelza.data

import com.ht.intelza.data.db.IntelzaDatabase
import com.ht.intelza.domain.AnswerOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleDataTest {

    @Test
    fun `two test grades with ten students each`() {
        assertEquals(listOf("Grade 2 – Test", "Grade 3 – Test"), SampleData.classes.map { it.name })
        for (sample in SampleData.classes) {
            assertEquals(10, sample.students.size)
            assertEquals(sample.students.size, sample.students.toSet().size)
        }
    }

    @Test
    fun `two banks of ten questions per subject and grade`() {
        val bySubjectAndGrade = SampleData.topics.groupBy { it.subject to it.grade }
        assertEquals(8, bySubjectAndGrade.size)
        for ((key, topics) in bySubjectAndGrade) {
            assertEquals("$key", 2, topics.size)
            for (topic in topics) assertEquals(topic.name, 10, topic.questions.size)
        }
        assertEquals(IntelzaDatabase.DEFAULT_SUBJECTS.toSet(), SampleData.topics.map { it.subject }.toSet())
        assertEquals(setOf("2", "3"), SampleData.topics.map { it.grade }.toSet())
        assertEquals(160, SampleData.topics.sumOf { it.questions.size })
    }

    @Test
    fun `every question has four distinct options and uses all answer letters`() {
        for (topic in SampleData.topics) {
            for (question in topic.questions) {
                assertEquals(question.text, 4, question.options.size)
                assertEquals(question.text, 4, question.options.toSet().size)
                assertTrue(question.text, question.options.all { it.isNotBlank() })
            }
            // Correct answers are spread out so scanning tests exercise every card turn.
            assertEquals(topic.name, AnswerOption.entries.toSet(), topic.questions.map { it.correct }.toSet())
        }
    }
}
