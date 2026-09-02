package com.masc.chesspuzzlewidget.network

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.masc.chesspuzzlewidget.auth.TokenStore
import com.masc.chesspuzzlewidget.engine.FenParser
import com.masc.chesspuzzlewidget.engine.PuzzleBoardState
import com.masc.chesspuzzlewidget.engine.replayPgnWithLastMove
import com.masc.chesspuzzlewidget.state.WidgetPuzzlePrefs
import com.masc.chesspuzzlewidget.state.WidgetStatus
import com.masc.chesspuzzlewidget.state.parseAngleSelection
import com.masc.chesspuzzlewidget.widget.ChessPuzzleWidgetProvider
import com.masc.chesspuzzlewidget.widget.WidgetUpdater
import kotlinx.coroutines.suspendCancellableCoroutine
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationService
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Fetches the user's next personalized puzzle from Lichess and persists it for one widget.
 * Runs via WorkManager because [android.appwidget.AppWidgetProvider] callbacks can't do
 * network I/O synchronously and aren't guaranteed a live process.
 */
class PuzzleFetchWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val appWidgetId = inputData.getInt(KEY_APPWIDGET_ID, -1)
        if (appWidgetId == -1) return Result.failure()
        val stageOnly = inputData.getBoolean(KEY_STAGE_ONLY, false)
        val confirmPuzzleId = inputData.getString(KEY_CONFIRM_PUZZLE_ID)
        val confirmWin = inputData.getBoolean(KEY_CONFIRM_WIN, false)
        val confirmAngle = inputData.getString(KEY_CONFIRM_ANGLE)
        val silent = stageOnly || confirmPuzzleId != null

        val prefs = WidgetPuzzlePrefs(applicationContext, appWidgetId)
        val tokenStore = TokenStore(applicationContext)
        val authState = tokenStore.loadAuthState()

        if (authState == null || !authState.isAuthorized) {
            if (!silent) {
                prefs.setStatus(WidgetStatus.NEEDS_LOGIN)
                WidgetUpdater.render(applicationContext, appWidgetId)
            }
            return Result.success()
        }

        val authService = AuthorizationService(applicationContext)
        try {
            val accessToken = try {
                freshAccessToken(authState, authService)
            } catch (e: Exception) {
                Log.e(TAG, "Token refresh failed: ${e.javaClass.simpleName}: ${e.message}", e)
                tokenStore.saveAuthState(authState)
                if (!silent) {
                    prefs.setStatus(WidgetStatus.NEEDS_LOGIN)
                    WidgetUpdater.render(applicationContext, appWidgetId)
                }
                return Result.success()
            }
            tokenStore.saveAuthState(authState)

            if (confirmPuzzleId != null) {
                try {
                    val angleForConfirm = confirmAngle ?: WidgetPuzzlePrefs.DEFAULT_ANGLE
                    LichessApiClient().confirmSolved(accessToken, angleForConfirm, confirmPuzzleId, confirmWin)
                } catch (e: Exception) {
                    Log.e(TAG, "Confirming puzzle result failed: ${e.javaClass.simpleName}: ${e.message}", e)
                }
                return Result.success()
            }

            val angle = prefs.selectedAngles().random()
            val angleSelection = parseAngleSelection(angle)
            val difficulty = prefs.difficulty()
            // When prefetching (silent, background), also explicitly avoid re-serving whatever
            // puzzle is currently on screen — Lichess doesn't advance its queue until a puzzle is
            // confirmed solved, so a prefetch that runs before that confirm can otherwise hand
            // back the very puzzle the user is still looking at.
            val recentIds = prefs.recentPuzzleIds() + listOfNotNull(if (stageOnly) prefs.puzzleId() else null)
            // A bit more patient than a foreground fetch since nothing is waiting on this — but not
            // too patient, or the prefetch won't be ready by the time the user wants it (the caller
            // falls back to a normal fetch anyway if a duplicate slips through).
            val maxRetries = if (stageOnly) 6 else MAX_DUPLICATE_RETRIES
            val client = LichessApiClient()
            val puzzle = try {
                var candidate = client.getNextPuzzle(
                    accessToken, angle = angleSelection.angle, difficulty = difficulty, color = angleSelection.color
                )
                var attempt = 1
                while (candidate.puzzle.id in recentIds && attempt < maxRetries) {
                    candidate = client.getNextPuzzle(
                        accessToken, angle = angleSelection.angle, difficulty = difficulty, color = angleSelection.color
                    )
                    attempt++
                }
                candidate
            } catch (e: Exception) {
                val detail = "${e.javaClass.simpleName}: ${e.message}"
                Log.e(TAG, "Fetching next puzzle failed: $detail", e)
                if (!silent) {
                    prefs.setLastError(detail)
                    prefs.setStatus(WidgetStatus.ERROR)
                    WidgetUpdater.render(applicationContext, appWidgetId)
                }
                return Result.success()
            }
            prefs.addRecentPuzzleId(puzzle.puzzle.id)

            try {
                val hasDirectFen = puzzle.puzzle.fen != null
                val replay = if (!hasDirectFen) replayPgnWithLastMove(puzzle.game.pgn) else null
                val setupMove = replay?.lastMove
                val fen = puzzle.puzzle.fen ?: FenParser.toFen(replay!!.position)
                Log.d(TAG, "puzzle=${puzzle.puzzle.id} hasDirectFen=$hasDirectFen setupMove=$setupMove stageOnly=$stageOnly")

                if (stageOnly) {
                    prefs.saveStagedPuzzle(
                        puzzle.puzzle.id, fen, puzzle.puzzle.solution, puzzle.puzzle.themes, angle,
                        rating = puzzle.puzzle.rating,
                        setupMoveFrom = setupMove?.from, setupMoveTo = setupMove?.to
                    )
                } else {
                    val boardState = PuzzleBoardState.fromFen(fen, puzzle.puzzle.solution)
                    prefs.setThemes(puzzle.puzzle.themes)
                    prefs.setPuzzleId(puzzle.puzzle.id)
                    prefs.setPuzzleAngle(angle)
                    prefs.setRating(puzzle.puzzle.rating)
                    prefs.setOriginalFen(fen)
                    prefs.setFlipped(!boardState.position.whiteToMove)
                    prefs.clearReveals()
                    prefs.setTainted(false)
                    prefs.setCountedSolve(false)
                    prefs.setHistoryLogged(false)
                    prefs.resetHistory(fen)
                    prefs.setSetupMove(setupMove?.from, setupMove?.to)
                    if (setupMove != null) prefs.setLastMove(setupMove.from, setupMove.to) else prefs.clearLastMove()
                    prefs.saveBoardState(boardState)
                    WidgetUpdater.render(applicationContext, appWidgetId)
                    ChessPuzzleWidgetProvider.enqueuePrefetch(applicationContext, appWidgetId)
                }
            } catch (e: Exception) {
                val detail = "${e.javaClass.simpleName}: ${e.message}"
                Log.e(TAG, "Processing puzzle ${puzzle.puzzle.id} failed: $detail", e)
                if (!silent) {
                    prefs.setLastError(detail)
                    prefs.setStatus(WidgetStatus.ERROR)
                    WidgetUpdater.render(applicationContext, appWidgetId)
                }
            }
            return Result.success()
        } finally {
            authService.dispose()
        }
    }

    private suspend fun freshAccessToken(authState: AuthState, service: AuthorizationService): String =
        suspendCancellableCoroutine { cont ->
            authState.performActionWithFreshTokens(service) { accessToken, _, ex ->
                when {
                    ex != null -> cont.resumeWithException(ex)
                    accessToken != null -> cont.resume(accessToken)
                    else -> cont.resumeWithException(IllegalStateException("Lichess did not return an access token"))
                }
            }
        }

    companion object {
        const val KEY_APPWIDGET_ID = "appWidgetId"
        const val KEY_STAGE_ONLY = "stageOnly"
        const val KEY_CONFIRM_PUZZLE_ID = "confirmPuzzleId"
        const val KEY_CONFIRM_WIN = "confirmWin"
        const val KEY_CONFIRM_ANGLE = "confirmAngle"
        private const val TAG = "PuzzleFetchWorker"
        private const val MAX_DUPLICATE_RETRIES = 4
    }
}
