package com.ht.intelza.ui.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ht.intelza.data.AppSettings
import com.ht.intelza.data.ClassRepository
import com.ht.intelza.data.SessionRepository
import com.ht.intelza.data.SettingsRepository
import com.ht.intelza.data.db.ResponseEntity
import com.ht.intelza.data.db.SessionEntity
import com.ht.intelza.data.db.SessionQuestionEntity
import com.ht.intelza.data.db.SessionStudentRow
import com.ht.intelza.domain.AnswerOption
import com.ht.intelza.domain.QuestionResult
import com.ht.intelza.domain.QuestionStatus
import com.ht.intelza.domain.QuestionType
import com.ht.intelza.domain.Scoring
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SessionPhase {
    /** Showing a question before scanning. */
    QUESTION,

    /** Camera open, reading cards. */
    SCANNING,

    /** Answers in; showing results. */
    RESULTS,

    /** Past the last question: add a quick question or finish. */
    WRAP_UP,
}

data class SessionUiState(
    val session: SessionEntity,
    val className: String,
    val questions: List<SessionQuestionEntity>,
    val students: List<SessionStudentRow>,
    val responses: List<ResponseEntity>,
    val currentIndex: Int,
    val scanning: Boolean,
    val revealed: Boolean,
    val settings: AppSettings,
) {
    val isQuickSession: Boolean get() = session.title.isEmpty()

    val current: SessionQuestionEntity? get() = questions.getOrNull(currentIndex)

    val phase: SessionPhase
        get() {
            val question = current ?: return SessionPhase.WRAP_UP
            return when {
                scanning -> SessionPhase.SCANNING
                question.status == QuestionStatus.ASKED -> SessionPhase.RESULTS
                else -> SessionPhase.QUESTION
            }
        }

    val presentStudents: List<SessionStudentRow> get() = students.filter { it.present }

    /** Answers to the current question, by student id. */
    val currentAnswers: Map<Long, AnswerOption>
        get() {
            val id = current?.id ?: return emptyMap()
            return responses.filter { it.sessionQuestionId == id }.associate { it.studentId to it.answer }
        }

    val currentResult: QuestionResult?
        get() = current?.let { Scoring.questionResult(it, students, responses) }

    /** Card numbers of every student in the session, used to ignore other cards. */
    val cardNumbers: Set<Int> get() = students.map { it.cardNumber }.toSet()
}

class SessionViewModel(
    private val sessions: SessionRepository,
    private val classes: ClassRepository,
    settings: SettingsRepository,
    savedState: SavedStateHandle,
) : ViewModel() {
    val sessionId: Long = checkNotNull(savedState["sessionId"])

    private val currentIndex = MutableStateFlow<Int?>(null)
    private val scanning = MutableStateFlow(false)
    private val revealed = MutableStateFlow(false)
    private val className = MutableStateFlow("")

    private val data = combine(
        sessions.observeSession(sessionId).filterNotNull(),
        sessions.observeQuestions(sessionId),
        sessions.observeStudents(sessionId),
        sessions.observeResponses(sessionId),
        settings.settings,
    ) { session, questions, students, responses, appSettings ->
        SessionData(session, questions, students, responses, appSettings)
    }

    val state: StateFlow<SessionUiState?> = combine(
        data,
        className,
        currentIndex,
        scanning,
        revealed,
    ) { d, name, index, isScanning, isRevealed ->
        val resolvedIndex = (index ?: d.session.currentPosition).coerceIn(0, d.questions.size)
        SessionUiState(
            session = d.session,
            className = name,
            questions = d.questions,
            students = d.students,
            responses = d.responses,
            currentIndex = resolvedIndex,
            scanning = isScanning && resolvedIndex < d.questions.size,
            revealed = isRevealed,
            settings = d.settings,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** What a TV or projector shows (requirement E1). */
    val presenter: StateFlow<PresenterState?> = state
        .map { it?.toPresenterState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            val session = sessions.observeSession(sessionId).filterNotNull().first()
            className.value = classes.getClass(session.classId)?.name.orEmpty()
        }
    }

    fun goTo(index: Int) {
        val current = state.value ?: return
        val target = index.coerceIn(0, current.questions.size)
        scanning.value = false
        revealed.value = false
        currentIndex.value = target
        viewModelScope.launch { sessions.setCurrentPosition(sessionId, target) }
    }

    fun next() = state.value?.let { goTo(it.currentIndex + 1) }

    fun previous() = state.value?.let { goTo(it.currentIndex - 1) }

    fun startScan() {
        val question = state.value?.current ?: return
        revealed.value = false
        scanning.value = true
        viewModelScope.launch { sessions.markAsked(question.id) }
    }

    fun stopScan() {
        scanning.value = false
    }

    fun skip() {
        val question = state.value?.current ?: return
        viewModelScope.launch {
            sessions.markSkipped(question.id)
            next()
        }
    }

    fun setRevealed(value: Boolean) {
        revealed.value = value
    }

    /** Answers already recorded for the current question, by card number (to seed the scanner). */
    fun recordedAnswersByCard(): Map<Int, AnswerOption> {
        val current = state.value ?: return emptyMap()
        val cards = current.students.associate { it.studentId to it.cardNumber }
        return current.currentAnswers.mapNotNull { (student, answer) -> cards[student]?.let { it to answer } }.toMap()
    }

    /** Stores answers confirmed by the scanner. Students marked absent who answer are marked present. */
    fun onCardsConfirmed(byCard: Map<Int, AnswerOption>) {
        val current = state.value ?: return
        val question = current.current ?: return
        val studentsByCard = current.students.associateBy { it.cardNumber }
        val answers = byCard.mapNotNull { (card, answer) ->
            studentsByCard[card]?.let { it.studentId to answer }
        }.toMap()
        if (answers.isEmpty()) return
        viewModelScope.launch {
            current.students.filter { !it.present && it.studentId in answers }.forEach {
                sessions.setPresent(sessionId, it.studentId, true)
            }
            sessions.recordScannedAnswers(question.id, answers)
        }
    }

    fun setAnswer(studentId: Long, answer: AnswerOption?) {
        val question = state.value?.current ?: return
        viewModelScope.launch { sessions.setAnswer(question.id, studentId, answer) }
    }

    fun clearAnswers() {
        val question = state.value?.current ?: return
        viewModelScope.launch { sessions.clearAnswers(question.id) }
    }

    fun setCorrectAnswer(answer: AnswerOption?) {
        val question = state.value?.current ?: return
        viewModelScope.launch { sessions.setCorrectAnswer(question.id, answer) }
    }

    fun setPresent(studentId: Long, present: Boolean) {
        viewModelScope.launch { sessions.setPresent(sessionId, studentId, present) }
    }

    /** Adds a quick question after the last one and starts scanning for it. */
    fun askQuickQuestion(type: QuestionType, optionCount: Int, text: String) {
        val current = state.value ?: return
        viewModelScope.launch {
            val id = sessions.addQuickQuestion(sessionId, type, optionCount, text)
            sessions.markAsked(id)
            currentIndex.value = current.questions.size
            sessions.setCurrentPosition(sessionId, current.questions.size)
            revealed.value = false
            scanning.value = true
        }
    }

    fun finish(onFinished: () -> Unit) {
        scanning.value = false
        viewModelScope.launch {
            sessions.finish(sessionId)
            onFinished()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            sessions.deleteSession(sessionId)
            onDeleted()
        }
    }

    private data class SessionData(
        val session: SessionEntity,
        val questions: List<SessionQuestionEntity>,
        val students: List<SessionStudentRow>,
        val responses: List<ResponseEntity>,
        val settings: AppSettings,
    )
}
