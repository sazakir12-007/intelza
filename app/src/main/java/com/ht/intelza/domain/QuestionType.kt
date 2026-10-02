package com.ht.intelza.domain

enum class QuestionType {
    /** 2–4 options with one correct answer. */
    MULTIPLE_CHOICE,

    /** A = True, B = False. */
    TRUE_FALSE,

    /** Opinion or self-check: no correct answer, never scored. */
    POLL;

    val isScored: Boolean get() = this != POLL

    /** Number of answer options for this type, given the option count chosen for the question. */
    fun optionCount(chosen: Int): Int = if (this == TRUE_FALSE) 2 else chosen.coerceIn(2, 4)
}

/** Progress of a question within an evaluation session. */
enum class QuestionStatus { PENDING, ASKED, SKIPPED }
