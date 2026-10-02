package com.ht.intelza.scan

import com.ht.intelza.domain.AnswerOption

/**
 * Turns noisy per-frame readings into confirmed answers.
 *
 * A card's answer is confirmed only after the same reading is seen in
 * [requiredFrames] frames in a row, so a card that is still being turned is not
 * recorded. When a student changes their answer the newer one replaces the old one
 * once it is stable too (the latest answer wins).
 */
class AnswerStabilizer(
    private val requiredFrames: Int = 3,
    /** A streak is broken if the card disappears for longer than this. */
    private val maxGapMillis: Long = 800,
) {
    private class Track(var candidate: AnswerOption, var streak: Int, var lastSeen: Long) {
        var confirmed: AnswerOption? = null
    }

    private val tracks = HashMap<Int, Track>()

    /**
     * Starts from answers recorded earlier (e.g. when scanning the same question again),
     * so unchanged cards are not reported as new.
     */
    fun seed(confirmed: Map<Int, AnswerOption>) {
        tracks.clear()
        for ((id, answer) in confirmed) {
            tracks[id] = Track(answer, 0, Long.MIN_VALUE).also { it.confirmed = answer }
        }
    }

    /**
     * Adds one frame's readings (card id to answer seen).
     * @return the cards whose confirmed answer changed because of this frame.
     */
    fun update(readings: Map<Int, AnswerOption>, nowMillis: Long): Map<Int, AnswerOption> {
        val changes = HashMap<Int, AnswerOption>()
        for ((id, answer) in readings) {
            val track = tracks.getOrPut(id) { Track(answer, 0, nowMillis) }
            if (track.lastSeen != Long.MIN_VALUE && nowMillis - track.lastSeen > maxGapMillis) {
                track.streak = 0
            }
            if (answer == track.candidate) {
                track.streak++
            } else {
                track.candidate = answer
                track.streak = 1
            }
            track.lastSeen = nowMillis
            if (track.streak >= requiredFrames && track.confirmed != answer) {
                track.confirmed = answer
                changes[id] = answer
            }
        }
        return changes
    }

    fun confirmed(): Map<Int, AnswerOption> =
        tracks.mapNotNull { (id, track) -> track.confirmed?.let { id to it } }.toMap()

    fun clear() = tracks.clear()
}
