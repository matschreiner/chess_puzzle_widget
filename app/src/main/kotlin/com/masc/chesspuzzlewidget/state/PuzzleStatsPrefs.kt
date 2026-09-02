package com.masc.chesspuzzlewidget.state

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.time.LocalDate

/** One puzzle's full solve record — everything worth remembering about how it went. */
@Serializable
data class PuzzleSolveRecord(
    val puzzleId: String,
    val date: String,
    val timestampMillis: Long,
    val rating: Int,
    val themes: List<String>,
    val usedHint: Boolean,
    val usedSolution: Boolean
) {
    val perfect: Boolean get() = !usedHint && !usedSolution
}

/**
 * Global (not per-widget) daily puzzle-solving stats, shared across every widget instance.
 * Each day gets its own counter key, so the history of past days is kept indefinitely just by
 * writing new keys — there's no separate "archive" step. Alongside the simple perfect-solve
 * counter (used for the widget's "Daily: N" header), this also keeps a full per-puzzle history
 * log (rating, themes, whether a hint/solution was used) for every puzzle solved, perfect or not.
 */
class PuzzleStatsPrefs(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val recordListSerializer = ListSerializer(PuzzleSolveRecord.serializer())

    /** Call when a puzzle was solved with no hint/solution reveal and no wrong move attempted. */
    fun recordPerfectSolve() {
        val key = countKey(LocalDate.now())
        prefs.edit().putInt(key, prefs.getInt(key, 0) + 1).apply()
    }

    fun todayCount(): Int = countFor(LocalDate.now())

    fun countFor(date: LocalDate): Int = prefs.getInt(countKey(date), 0)

    private fun countKey(date: LocalDate) = "$KEY_PREFIX$date"

    /** Appends one solved puzzle's full record to that day's history log. */
    fun recordSolve(record: PuzzleSolveRecord) {
        val date = LocalDate.parse(record.date)
        val updated = historyFor(date) + record
        prefs.edit()
            .putString(historyKey(date), json.encodeToString(recordListSerializer, updated))
            .apply()
    }

    fun historyFor(date: LocalDate): List<PuzzleSolveRecord> {
        val raw = prefs.getString(historyKey(date), null) ?: return emptyList()
        return try {
            json.decodeFromString(recordListSerializer, raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Every date that has at least one recorded solve, most recent first. */
    fun historyDates(): List<LocalDate> =
        prefs.all.keys
            .filter { it.startsWith(HISTORY_KEY_PREFIX) }
            .mapNotNull { key -> runCatching { LocalDate.parse(key.removePrefix(HISTORY_KEY_PREFIX)) }.getOrNull() }
            .sortedDescending()

    private fun historyKey(date: LocalDate) = "$HISTORY_KEY_PREFIX$date"

    companion object {
        private const val PREFS_NAME = "puzzle_stats"
        private const val KEY_PREFIX = "solved_"
        private const val HISTORY_KEY_PREFIX = "history_"
    }
}
