package com.ht.intelza.ui.session

import com.ht.intelza.domain.AnswerOption
import com.ht.intelza.domain.QuestionType

enum class PresenterStage {
    /** Before the first question or after the last: shows the session title. */
    WAITING,

    /** The question and its options. */
    QUESTION,

    /** Scanning, or scanned but not revealed: who has answered, never what. */
    COLLECTING,

    /** The answer chart and correct answer. */
    RESULTS,
}

data class PresenterState(
    val title: String,
    val className: String,
    val questionNumber: Int,
    val questionCount: Int,
    val stage: PresenterStage,
    val questionText: String,
    val imageName: String?,
    val type: QuestionType,
    val options: List<Pair<AnswerOption, String>>,
    val answeredCount: Int,
    val presentCount: Int,
    /** Present students by card number, with whether each has answered yet. */
    val roster: List<Pair<String, Boolean>>,
    val counts: Map<AnswerOption, Int>,
    val correct: AnswerOption?,
    val fractionCorrect: Float?,
)

internal fun SessionUiState.toPresenterState(): PresenterState {
    val question = current
    val result = currentResult
    val answers = currentAnswers
    val present = presentStudents
    val stage = when (phase) {
        SessionPhase.WRAP_UP -> PresenterStage.WAITING
        SessionPhase.QUESTION -> PresenterStage.QUESTION
        SessionPhase.SCANNING -> PresenterStage.COLLECTING
        SessionPhase.RESULTS -> if (revealed) PresenterStage.RESULTS else PresenterStage.COLLECTING
    }
    val options = question?.let { q ->
        AnswerOption.firstN(q.type.optionCount(q.optionCount)).map { option ->
            option to when (option) {
                AnswerOption.A -> q.optionA
                AnswerOption.B -> q.optionB
                AnswerOption.C -> q.optionC
                AnswerOption.D -> q.optionD
            }
        }
    }.orEmpty()
    return PresenterState(
        title = session.title,
        className = className,
        questionNumber = if (question != null) currentIndex + 1 else 0,
        questionCount = questions.size,
        stage = stage,
        questionText = question?.text.orEmpty(),
        imageName = question?.imageName,
        type = question?.type ?: QuestionType.MULTIPLE_CHOICE,
        options = options,
        answeredCount = present.count { it.studentId in answers },
        presentCount = present.size,
        roster = present.map { it.name to (it.studentId in answers) },
        counts = result?.counts.orEmpty(),
        correct = if (stage == PresenterStage.RESULTS) question?.correct else null,
        fractionCorrect = if (stage == PresenterStage.RESULTS) result?.fractionCorrect else null,
    )
}
