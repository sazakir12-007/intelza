package com.ht.intelza.data

import androidx.room.withTransaction
import com.ht.intelza.data.csv.StudentImportRow
import com.ht.intelza.data.db.ClassEntity
import com.ht.intelza.data.db.IntelzaDatabase
import com.ht.intelza.data.db.StudentEntity
import com.ht.intelza.domain.CardNumbers

sealed interface StudentSaveResult {
    data class Saved(val studentId: Long) : StudentSaveResult
    data class CardTaken(val takenBy: String) : StudentSaveResult
    data object InvalidCard : StudentSaveResult
    data object NoFreeCard : StudentSaveResult
}

data class ImportResult(val added: Int, val skipped: List<String>)

class ClassRepository(private val db: IntelzaDatabase) {
    private val classes = db.classDao()
    private val students = db.studentDao()

    fun observeSummaries() = classes.observeSummaries()

    fun observeClass(id: Long) = classes.observe(id)

    suspend fun getClass(id: Long) = classes.get(id)

    fun observeStudents(classId: Long) = students.observeActive(classId)

    suspend fun getStudents(classId: Long) = students.getActive(classId)

    /** Includes students removed from the class, for reports on past sessions. */
    suspend fun getAllStudents(classId: Long) = students.getAll(classId)

    suspend fun getStudent(id: Long) = students.get(id)

    fun observeStudent(id: Long) = students.observe(id)

    suspend fun createClass(name: String): Long = classes.insert(ClassEntity(name = name.trim()))

    suspend fun renameClass(id: Long, name: String) {
        classes.get(id)?.let { classes.update(it.copy(name = name.trim())) }
    }

    suspend fun setArchived(id: Long, archived: Boolean) {
        classes.get(id)?.let { classes.update(it.copy(archived = archived)) }
    }

    /** Deletes the class with its students and all their results. */
    suspend fun deleteClass(id: Long) = classes.delete(id)

    /**
     * Adds a student. With no [cardNumber] the lowest free card is used.
     */
    suspend fun addStudent(
        classId: Long,
        name: String,
        rollNumber: String,
        cardNumber: Int?,
    ): StudentSaveResult = db.withTransaction {
        val existing = students.getActive(classId)
        val card = cardNumber ?: CardNumbers.nextFree(existing.mapNotNull { it.cardNumber })
            ?: return@withTransaction StudentSaveResult.NoFreeCard
        validateCard(card, existing, ignoreStudentId = null)?.let { return@withTransaction it }
        val id = students.insert(
            StudentEntity(classId = classId, name = name.trim(), rollNumber = rollNumber.trim(), cardNumber = card),
        )
        StudentSaveResult.Saved(id)
    }

    suspend fun updateStudent(
        studentId: Long,
        name: String,
        rollNumber: String,
        cardNumber: Int,
    ): StudentSaveResult = db.withTransaction {
        val student = students.get(studentId) ?: return@withTransaction StudentSaveResult.InvalidCard
        validateCard(cardNumber, students.getActive(student.classId), ignoreStudentId = studentId)
            ?.let { return@withTransaction it }
        students.update(student.copy(name = name.trim(), rollNumber = rollNumber.trim(), cardNumber = cardNumber))
        StudentSaveResult.Saved(studentId)
    }

    /** Removes the student from the class; their past results are kept. */
    suspend fun removeStudent(studentId: Long) = students.deactivate(studentId)

    /**
     * Adds imported students. Requested card numbers are used when free; otherwise the
     * lowest free card is assigned.
     */
    suspend fun importStudents(classId: Long, rows: List<StudentImportRow>): ImportResult =
        db.withTransaction {
            val used = students.getActive(classId).mapNotNull { it.cardNumber }.toHashSet()
            var added = 0
            val skipped = mutableListOf<String>()
            for (row in rows) {
                val requested = row.cardNumber?.takeIf { CardNumbers.isValid(it) && it !in used }
                val card = requested ?: CardNumbers.nextFree(used)
                if (card == null) {
                    skipped += row.name
                    continue
                }
                students.insert(
                    StudentEntity(classId = classId, name = row.name, rollNumber = row.rollNumber, cardNumber = card),
                )
                used += card
                added++
            }
            ImportResult(added, skipped)
        }

    private fun validateCard(
        card: Int,
        classStudents: List<StudentEntity>,
        ignoreStudentId: Long?,
    ): StudentSaveResult? {
        if (!CardNumbers.isValid(card)) return StudentSaveResult.InvalidCard
        val owner = classStudents.firstOrNull { it.cardNumber == card && it.id != ignoreStudentId }
        return owner?.let { StudentSaveResult.CardTaken(it.name) }
    }
}
