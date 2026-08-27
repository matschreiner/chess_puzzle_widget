package com.masc.chesspuzzlewidget.state

import android.content.Context
import java.time.LocalDate

/**
 * Global (not per-widget) daily puzzle-solving stats, shared across every widget instance.
 * Each day gets its own counter key, so the history of past days is kept indefinitely just by
 * writing new keys — there's no separate "archive" step.
 */
class PuzzleStatsPrefs(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Call when a puzzle was solved with no hint/solution reveal and no wrong move attempted. */
    fun recordPerfectSolve() {
        val key = countKey(LocalDate.now())
        prefs.edit().putInt(key, prefs.getInt(key, 0) + 1).apply()
    }

    fun todayCount(): Int = countFor(LocalDate.now())

    fun countFor(date: LocalDate): Int = prefs.getInt(countKey(date), 0)

    private fun countKey(date: LocalDate) = "$KEY_PREFIX$date"

    companion object {
        private const val PREFS_NAME = "puzzle_stats"
        private const val KEY_PREFIX = "solved_"
    }
}
