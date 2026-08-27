package com.masc.chesspuzzlewidget.state

import android.content.Context
import com.masc.chesspuzzlewidget.engine.FenParser
import com.masc.chesspuzzlewidget.engine.PuzzleBoardState
import com.masc.chesspuzzlewidget.engine.PuzzleStatus
import com.masc.chesspuzzlewidget.engine.UciMove

enum class WidgetStatus { NEEDS_LOGIN, LOADING, READY, ERROR }

/** Per-appWidgetId persisted puzzle state, since the widget process can be killed and recreated at any time. */
class WidgetPuzzlePrefs(context: Context, appWidgetId: Int) {

    private val prefs = context.getSharedPreferences(prefsName(appWidgetId), Context.MODE_PRIVATE)

    fun status(): WidgetStatus =
        prefs.getString(KEY_STATUS, null)?.let { runCatching { WidgetStatus.valueOf(it) }.getOrNull() }
            ?: WidgetStatus.NEEDS_LOGIN

    fun setStatus(status: WidgetStatus) {
        prefs.edit().putString(KEY_STATUS, status.name).apply()
    }

    fun loadBoardState(): PuzzleBoardState? {
        val fen = prefs.getString(KEY_FEN, null) ?: return null
        val solutionCsv = prefs.getString(KEY_SOLUTION, null) ?: return null
        val solution = if (solutionCsv.isEmpty()) emptyList() else solutionCsv.split(",")
        val solutionIndex = prefs.getInt(KEY_SOLUTION_INDEX, 0)
        val selectedSquare = prefs.getInt(KEY_SELECTED_SQUARE, -1).takeIf { it >= 0 }
        val status = if (solutionIndex >= solution.size) PuzzleStatus.SOLVED else PuzzleStatus.AWAITING_MOVE

        return PuzzleBoardState(
            position = FenParser.parse(fen),
            solution = solution.map { UciMove.parse(it) },
            solutionIndex = solutionIndex,
            selectedSquare = selectedSquare,
            status = status
        )
    }

    fun saveBoardState(boardState: PuzzleBoardState) {
        prefs.edit()
            .putString(KEY_FEN, FenParser.toFen(boardState.position))
            .putString(KEY_SOLUTION, boardState.solution.joinToString(",") { it.toString() })
            .putInt(KEY_SOLUTION_INDEX, boardState.solutionIndex)
            .putInt(KEY_SELECTED_SQUARE, boardState.selectedSquare ?: -1)
            .apply()
        setStatus(WidgetStatus.READY)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun setLastError(message: String) {
        prefs.edit().putString(KEY_LAST_ERROR, message).apply()
    }

    fun lastError(): String? = prefs.getString(KEY_LAST_ERROR, null)

    fun setThemes(themes: List<String>) {
        prefs.edit().putString(KEY_THEMES, themes.joinToString(",")).apply()
    }

    fun themes(): List<String> =
        prefs.getString(KEY_THEMES, null)?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

    fun setPuzzleId(id: String) {
        prefs.edit().putString(KEY_PUZZLE_ID, id).apply()
    }

    fun puzzleId(): String? = prefs.getString(KEY_PUZZLE_ID, null)

    fun setRating(rating: Int) {
        prefs.edit().putInt(KEY_RATING, rating).apply()
    }

    fun rating(): Int = prefs.getInt(KEY_RATING, 0)

    /** The set of Lichess puzzle themes ("angles") the user wants — one is picked at random per fetch. */
    fun setSelectedAngles(angles: Set<String>) {
        prefs.edit().putString(KEY_ANGLES, angles.joinToString(",")).apply()
    }

    fun selectedAngles(): Set<String> {
        val stored = prefs.getString(KEY_ANGLES, null)?.split(",")?.filter { it.isNotBlank() }?.toSet()
        return if (stored.isNullOrEmpty()) setOf(DEFAULT_ANGLE) else stored
    }

    /** Puzzle difficulty relative to the user's own puzzle rating (Lichess's `difficulty` param). */
    fun setDifficulty(difficulty: String) {
        prefs.edit().putString(KEY_DIFFICULTY, difficulty).apply()
    }

    fun difficulty(): String = prefs.getString(KEY_DIFFICULTY, null) ?: PuzzleDifficulty.DEFAULT

    /** The angle the currently active puzzle was actually fetched with (for confirming its result later). */
    fun setPuzzleAngle(angle: String) {
        prefs.edit().putString(KEY_PUZZLE_ANGLE, angle).apply()
    }

    fun puzzleAngle(): String = prefs.getString(KEY_PUZZLE_ANGLE, null) ?: DEFAULT_ANGLE

    /** The puzzle's starting FEN, saved once when fetched — lets "restart" undo every move made since. */
    fun setOriginalFen(fen: String) {
        prefs.edit().putString(KEY_ORIGINAL_FEN, fen).apply()
    }

    fun originalFen(): String? = prefs.getString(KEY_ORIGINAL_FEN, null)

    /** The most recently played move's squares, highlighted like Lichess's last-move markers. */
    fun setLastMove(from: Int, to: Int) {
        prefs.edit().putInt(KEY_LAST_MOVE_FROM, from).putInt(KEY_LAST_MOVE_TO, to).apply()
    }

    fun lastMove(): Pair<Int, Int>? {
        val from = prefs.getInt(KEY_LAST_MOVE_FROM, -1)
        val to = prefs.getInt(KEY_LAST_MOVE_TO, -1)
        return if (from >= 0 && to >= 0) from to to else null
    }

    fun clearLastMove() {
        prefs.edit().remove(KEY_LAST_MOVE_FROM).remove(KEY_LAST_MOVE_TO).apply()
    }

    /**
     * The puzzle's "setup" move (the opponent's blunder that created the tactic) — persists for the
     * whole puzzle, unlike [setLastMove] which changes with every move played, so "restart" can
     * bring back this same initial highlight.
     */
    fun setSetupMove(from: Int?, to: Int?) {
        prefs.edit().putInt(KEY_SETUP_MOVE_FROM, from ?: -1).putInt(KEY_SETUP_MOVE_TO, to ?: -1).apply()
    }

    fun setupMove(): Pair<Int, Int>? {
        val from = prefs.getInt(KEY_SETUP_MOVE_FROM, -1)
        val to = prefs.getInt(KEY_SETUP_MOVE_TO, -1)
        return if (from >= 0 && to >= 0) from to to else null
    }

    /** Board orientation is decided once when a puzzle is fetched and stays fixed until the next fetch. */
    fun setFlipped(flipped: Boolean) {
        prefs.edit().putBoolean(KEY_FLIPPED, flipped).apply()
    }

    fun isFlipped(): Boolean = prefs.getBoolean(KEY_FLIPPED, false)

    fun setHintRequested(active: Boolean) {
        prefs.edit().putBoolean(KEY_HINT, active).apply()
    }

    fun isHintRequested(): Boolean = prefs.getBoolean(KEY_HINT, false)

    fun setSolutionRequested(active: Boolean) {
        prefs.edit().putBoolean(KEY_SOLUTION_REQUESTED, active).apply()
    }

    fun isSolutionRequested(): Boolean = prefs.getBoolean(KEY_SOLUTION_REQUESTED, false)

    fun clearReveals() {
        prefs.edit()
            .putBoolean(KEY_HINT, false)
            .putBoolean(KEY_SOLUTION_REQUESTED, false)
            .apply()
    }

    /**
     * Whether the current puzzle has had a hint/solution reveal or a wrong move attempt — once
     * true it stays true for the rest of this puzzle (even across Restart) so it can't be "solved
     * cleanly" after the fact. Only cleared when a genuinely new puzzle is fetched.
     */
    fun setTainted(tainted: Boolean) {
        prefs.edit().putBoolean(KEY_TAINTED, tainted).apply()
    }

    fun isTainted(): Boolean = prefs.getBoolean(KEY_TAINTED, false)

    /**
     * Whether this puzzle has already counted toward the daily solved-count — restarting and
     * solving the same puzzle again must not double-count it. Only cleared when a genuinely new
     * puzzle is fetched, just like [isTainted].
     */
    fun setCountedSolve(counted: Boolean) {
        prefs.edit().putBoolean(KEY_COUNTED_SOLVE, counted).apply()
    }

    fun hasCountedSolve(): Boolean = prefs.getBoolean(KEY_COUNTED_SOLVE, false)

    /** A puzzle fetched ahead of time in the background, ready to swap in instantly on skip/solve. */
    data class StagedPuzzle(
        val id: String,
        val fen: String,
        val solution: List<String>,
        val themes: List<String>,
        val angle: String,
        val rating: Int,
        val setupMoveFrom: Int?,
        val setupMoveTo: Int?
    )

    fun saveStagedPuzzle(
        id: String,
        fen: String,
        solution: List<String>,
        themes: List<String>,
        angle: String,
        rating: Int = 0,
        setupMoveFrom: Int? = null,
        setupMoveTo: Int? = null
    ) {
        prefs.edit()
            .putString(KEY_STAGED_ID, id)
            .putString(KEY_STAGED_FEN, fen)
            .putString(KEY_STAGED_SOLUTION, solution.joinToString(","))
            .putString(KEY_STAGED_THEMES, themes.joinToString(","))
            .putString(KEY_STAGED_ANGLE, angle)
            .putInt(KEY_STAGED_RATING, rating)
            .putInt(KEY_STAGED_SETUP_FROM, setupMoveFrom ?: -1)
            .putInt(KEY_STAGED_SETUP_TO, setupMoveTo ?: -1)
            .apply()
    }

    fun loadStagedPuzzle(): StagedPuzzle? {
        val id = prefs.getString(KEY_STAGED_ID, null) ?: return null
        val fen = prefs.getString(KEY_STAGED_FEN, null) ?: return null
        val angle = prefs.getString(KEY_STAGED_ANGLE, null) ?: DEFAULT_ANGLE
        val rating = prefs.getInt(KEY_STAGED_RATING, 0)
        val setupFrom = prefs.getInt(KEY_STAGED_SETUP_FROM, -1).takeIf { it >= 0 }
        val setupTo = prefs.getInt(KEY_STAGED_SETUP_TO, -1).takeIf { it >= 0 }
        val solution = prefs.getString(KEY_STAGED_SOLUTION, "")!!.split(",").filter { it.isNotBlank() }
        val themes = prefs.getString(KEY_STAGED_THEMES, "")!!.split(",").filter { it.isNotBlank() }
        return StagedPuzzle(id, fen, solution, themes, angle, rating, setupFrom, setupTo)
    }

    /** Tracks recently served puzzle ids so a same-again response from Lichess can be detected and retried. */
    fun recentPuzzleIds(): List<String> =
        prefs.getString(KEY_RECENT_IDS, null)?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

    fun addRecentPuzzleId(id: String) {
        val updated = (recentPuzzleIds() + id).takeLast(RECENT_IDS_LIMIT)
        prefs.edit().putString(KEY_RECENT_IDS, updated.joinToString(",")).apply()
    }

    fun clearStagedPuzzle() {
        prefs.edit()
            .remove(KEY_STAGED_ID)
            .remove(KEY_STAGED_FEN)
            .remove(KEY_STAGED_SOLUTION)
            .remove(KEY_STAGED_THEMES)
            .remove(KEY_STAGED_ANGLE)
            .remove(KEY_STAGED_RATING)
            .remove(KEY_STAGED_SETUP_FROM)
            .remove(KEY_STAGED_SETUP_TO)
            .apply()
    }

    companion object {
        private const val KEY_STATUS = "status"
        private const val KEY_FEN = "fen"
        private const val KEY_SOLUTION = "solution"
        private const val KEY_SOLUTION_INDEX = "solution_index"
        private const val KEY_SELECTED_SQUARE = "selected_square"
        private const val KEY_LAST_ERROR = "last_error"
        private const val KEY_THEMES = "themes"
        private const val KEY_PUZZLE_ID = "puzzle_id"
        private const val KEY_RATING = "rating"
        private const val KEY_ANGLES = "puzzle_angles"
        private const val KEY_DIFFICULTY = "puzzle_difficulty"
        private const val KEY_PUZZLE_ANGLE = "active_puzzle_angle"
        const val DEFAULT_ANGLE = "pin"
        private const val KEY_ORIGINAL_FEN = "original_fen"
        private const val KEY_LAST_MOVE_FROM = "last_move_from"
        private const val KEY_LAST_MOVE_TO = "last_move_to"
        private const val KEY_SETUP_MOVE_FROM = "setup_move_from"
        private const val KEY_SETUP_MOVE_TO = "setup_move_to"
        private const val KEY_FLIPPED = "flipped"
        private const val KEY_HINT = "hint_requested"
        private const val KEY_SOLUTION_REQUESTED = "solution_requested"
        private const val KEY_TAINTED = "tainted"
        private const val KEY_COUNTED_SOLVE = "counted_solve"
        private const val KEY_STAGED_ID = "staged_id"
        private const val KEY_STAGED_FEN = "staged_fen"
        private const val KEY_STAGED_SOLUTION = "staged_solution"
        private const val KEY_STAGED_THEMES = "staged_themes"
        private const val KEY_STAGED_ANGLE = "staged_angle"
        private const val KEY_STAGED_RATING = "staged_rating"
        private const val KEY_STAGED_SETUP_FROM = "staged_setup_from"
        private const val KEY_STAGED_SETUP_TO = "staged_setup_to"
        private const val KEY_RECENT_IDS = "recent_puzzle_ids"
        private const val RECENT_IDS_LIMIT = 15

        private fun prefsName(appWidgetId: Int) = "widget_puzzle_$appWidgetId"
    }
}
