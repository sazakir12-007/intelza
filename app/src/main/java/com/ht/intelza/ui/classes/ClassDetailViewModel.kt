package com.ht.intelza.ui.classes

import android.app.Application
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ht.intelza.R
import com.ht.intelza.data.ClassRepository
import com.ht.intelza.data.StudentSaveResult
import com.ht.intelza.data.csv.StudentImport
import com.ht.intelza.data.csv.StudentImportRow
import com.ht.intelza.data.db.ClassEntity
import com.ht.intelza.data.db.StudentEntity
import com.ht.intelza.domain.CardNumbers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ClassDetailState(
    val schoolClass: ClassEntity?,
    val students: List<StudentEntity>,
    val loaded: Boolean,
)

/** Result of saving the student dialog: null when saved, otherwise an error to show. */
sealed interface StudentFormError {
    data class CardTaken(val takenBy: String) : StudentFormError
    data object InvalidCard : StudentFormError
    data object NoFreeCard : StudentFormError
}

class ClassDetailViewModel(
    private val app: Application,
    private val classes: ClassRepository,
    savedState: SavedStateHandle,
) : ViewModel() {
    val classId: Long = checkNotNull(savedState["classId"])

    val state: StateFlow<ClassDetailState> = combine(
        classes.observeClass(classId),
        classes.observeStudents(classId),
    ) { schoolClass, students -> ClassDetailState(schoolClass, students, loaded = true) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ClassDetailState(null, emptyList(), false))

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /** Students read from a file, waiting for the teacher to confirm the import. */
    private val _pendingImport = MutableStateFlow<List<StudentImportRow>?>(null)
    val pendingImport: StateFlow<List<StudentImportRow>?> = _pendingImport.asStateFlow()

    fun nextFreeCard(): Int? = CardNumbers.nextFree(state.value.students.mapNotNull { it.cardNumber })

    fun rename(name: String) = viewModelScope.launch { classes.renameClass(classId, name) }

    fun setArchived(archived: Boolean) = viewModelScope.launch { classes.setArchived(classId, archived) }

    fun delete(onDeleted: () -> Unit) = viewModelScope.launch {
        classes.deleteClass(classId)
        onDeleted()
    }

    fun saveStudent(
        studentId: Long?,
        name: String,
        rollNumber: String,
        cardNumber: Int?,
        onResult: (StudentFormError?) -> Unit,
    ) = viewModelScope.launch {
        val result = if (studentId == null) {
            classes.addStudent(classId, name, rollNumber, cardNumber)
        } else if (cardNumber == null) {
            StudentSaveResult.InvalidCard
        } else {
            classes.updateStudent(studentId, name, rollNumber, cardNumber)
        }
        onResult(
            when (result) {
                is StudentSaveResult.Saved -> null
                is StudentSaveResult.CardTaken -> StudentFormError.CardTaken(result.takenBy)
                StudentSaveResult.InvalidCard -> StudentFormError.InvalidCard
                StudentSaveResult.NoFreeCard -> StudentFormError.NoFreeCard
            },
        )
    }

    fun removeStudent(studentId: Long) = viewModelScope.launch { classes.removeStudent(studentId) }

    fun readImportFile(uri: Uri) = viewModelScope.launch {
        val rows = withContext(Dispatchers.IO) {
            runCatching {
                app.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            }.getOrNull()?.let(StudentImport::parse)
        }
        if (rows.isNullOrEmpty()) {
            _message.value = app.getString(R.string.import_nothing_found)
        } else {
            _pendingImport.value = rows
        }
    }

    fun confirmImport() = viewModelScope.launch {
        val rows = _pendingImport.value ?: return@launch
        _pendingImport.value = null
        val result = classes.importStudents(classId, rows)
        _message.value = buildString {
            append(app.resources.getQuantityString(R.plurals.import_added, result.added, result.added))
            if (result.skipped.isNotEmpty()) {
                append(" ")
                append(app.getString(R.string.import_skipped, result.skipped.size))
            }
        }
    }

    fun cancelImport() {
        _pendingImport.value = null
    }

    fun messageShown() {
        _message.value = null
    }
}
