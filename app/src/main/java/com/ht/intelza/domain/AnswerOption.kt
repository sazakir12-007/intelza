package com.ht.intelza.domain

/**
 * One of the four answers a student can show by turning their card.
 * The letters are printed clockwise around the card: A top, B right, C bottom, D left.
 */
enum class AnswerOption {
    A, B, C, D;

    val letter: Char get() = name[0]

    companion object {
        fun fromIndex(index: Int): AnswerOption = entries[index]

        fun fromLetter(letter: String?): AnswerOption? =
            entries.firstOrNull { it.name.equals(letter?.trim(), ignoreCase = true) }

        /** The first [count] options, e.g. A–C for a three-option question. */
        fun firstN(count: Int): List<AnswerOption> = entries.take(count.coerceIn(2, 4))
    }
}
