package com.masc.chesspuzzlewidget.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.masc.chesspuzzlewidget.engine.PuzzleBoardState
import com.masc.chesspuzzlewidget.engine.PuzzleStatus
import com.masc.chesspuzzlewidget.network.PuzzleFetchWorker
import com.masc.chesspuzzlewidget.state.WidgetPuzzlePrefs
import com.masc.chesspuzzlewidget.state.WidgetStatus

class ChessPuzzleWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { appWidgetId -> refreshWidget(context, appWidgetId) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        WidgetUpdater.render(context, appWidgetId)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { appWidgetId -> WidgetPuzzlePrefs(context, appWidgetId).clear() }
    }

    companion object {

        /** Renders from cache if we already have a puzzle (or already know we need login); fetches otherwise. */
        fun refreshWidget(context: Context, appWidgetId: Int) {
            val prefs = WidgetPuzzlePrefs(context, appWidgetId)
            val status = prefs.status()

            if (status == WidgetStatus.READY && prefs.loadBoardState() != null) {
                WidgetUpdater.render(context, appWidgetId)
                return
            }
            if (status == WidgetStatus.NEEDS_LOGIN) {
                WidgetUpdater.render(context, appWidgetId)
                return
            }

            requestNextPuzzle(context, appWidgetId)
        }

        /**
         * Called when the user wants a new puzzle (skip, solved-continue, first load): swaps in the
         * already-prefetched puzzle instantly if one is ready, otherwise falls back to [forceFetch].
         */
        fun requestNextPuzzle(context: Context, appWidgetId: Int) {
            val prefs = WidgetPuzzlePrefs(context, appWidgetId)

            val current = prefs.loadBoardState()
            val currentId = prefs.puzzleId()
            if (current != null && current.status != PuzzleStatus.SOLVED && currentId != null) {
                enqueueConfirm(context, appWidgetId, currentId, prefs.puzzleAngle(), win = false)
            }

            val staged = prefs.loadStagedPuzzle()
            if (staged == null) {
                forceFetch(context, appWidgetId)
                return
            }

            val boardState = PuzzleBoardState.fromFen(staged.fen, staged.solution)
            prefs.setThemes(staged.themes)
            prefs.setPuzzleId(staged.id)
            prefs.setPuzzleAngle(staged.angle)
            prefs.setOriginalFen(staged.fen)
            prefs.setFlipped(!boardState.position.whiteToMove)
            prefs.clearReveals()
            prefs.setSetupMove(staged.setupMoveFrom, staged.setupMoveTo)
            if (staged.setupMoveFrom != null && staged.setupMoveTo != null) {
                prefs.setLastMove(staged.setupMoveFrom, staged.setupMoveTo)
            } else {
                prefs.clearLastMove()
            }
            prefs.clearStagedPuzzle()
            prefs.saveBoardState(boardState)
            WidgetUpdater.render(context, appWidgetId)
            enqueuePrefetch(context, appWidgetId)
        }

        /** Unconditionally shows a loading state and fetches a fresh puzzle over the network. */
        fun forceFetch(context: Context, appWidgetId: Int) {
            val prefs = WidgetPuzzlePrefs(context, appWidgetId)
            prefs.setStatus(WidgetStatus.LOADING)
            WidgetUpdater.render(context, appWidgetId)

            val request = OneTimeWorkRequestBuilder<PuzzleFetchWorker>()
                .setInputData(Data.Builder().putInt(PuzzleFetchWorker.KEY_APPWIDGET_ID, appWidgetId).build())
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }

        /** Quietly fetches the puzzle after next in the background, without touching the active puzzle. */
        fun enqueuePrefetch(context: Context, appWidgetId: Int) {
            val request = OneTimeWorkRequestBuilder<PuzzleFetchWorker>()
                .setInputData(
                    Data.Builder()
                        .putInt(PuzzleFetchWorker.KEY_APPWIDGET_ID, appWidgetId)
                        .putBoolean(PuzzleFetchWorker.KEY_STAGE_ONLY, true)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }

        /** Tells Lichess a puzzle was solved or given up on, so its personalized queue advances past it. */
        fun enqueueConfirm(context: Context, appWidgetId: Int, puzzleId: String, angle: String, win: Boolean) {
            val request = OneTimeWorkRequestBuilder<PuzzleFetchWorker>()
                .setInputData(
                    Data.Builder()
                        .putInt(PuzzleFetchWorker.KEY_APPWIDGET_ID, appWidgetId)
                        .putString(PuzzleFetchWorker.KEY_CONFIRM_PUZZLE_ID, puzzleId)
                        .putString(PuzzleFetchWorker.KEY_CONFIRM_ANGLE, angle)
                        .putBoolean(PuzzleFetchWorker.KEY_CONFIRM_WIN, win)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }

        /** Called after a successful login so every already-placed widget picks up a puzzle immediately. */
        fun forceFetchAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, ChessPuzzleWidgetProvider::class.java))
            ids.forEach { appWidgetId -> forceFetch(context, appWidgetId) }
        }
    }
}
