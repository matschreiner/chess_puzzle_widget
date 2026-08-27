package com.masc.chesspuzzlewidget.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.masc.chesspuzzlewidget.engine.Position
import com.masc.chesspuzzlewidget.engine.PuzzleBoardState
import com.masc.chesspuzzlewidget.engine.PuzzleStatus
import com.masc.chesspuzzlewidget.state.PuzzleStatsPrefs
import com.masc.chesspuzzlewidget.state.WidgetPuzzlePrefs

/** Receives taps on the widget's 64 overlay cells and its "fetch puzzle" targets. */
class WidgetClickReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appWidgetId = intent.getIntExtra(EXTRA_APPWIDGET_ID, -1)
        if (appWidgetId == -1) return

        when (intent.action) {
            ACTION_SQUARE_TAP -> handleSquareTap(context, appWidgetId, intent)
            ACTION_FETCH_PUZZLE -> ChessPuzzleWidgetProvider.requestNextPuzzle(context, appWidgetId)
            ACTION_HINT -> {
                val prefs = WidgetPuzzlePrefs(context, appWidgetId)
                prefs.setSolutionRequested(false)
                prefs.setHintRequested(true)
                prefs.setTainted(true)
                WidgetUpdater.render(context, appWidgetId)
            }
            ACTION_SHOW_SOLUTION -> {
                val prefs = WidgetPuzzlePrefs(context, appWidgetId)
                prefs.setHintRequested(false)
                prefs.setSolutionRequested(true)
                prefs.setTainted(true)
                WidgetUpdater.render(context, appWidgetId)
            }
            ACTION_RESTART -> handleRestart(context, appWidgetId)
        }
    }

    private fun handleRestart(context: Context, appWidgetId: Int) {
        val prefs = WidgetPuzzlePrefs(context, appWidgetId)
        val originalFen = prefs.originalFen() ?: return
        val current = prefs.loadBoardState() ?: return

        val restarted = PuzzleBoardState.fromFen(originalFen, current.solution.map { it.toString() })
        prefs.clearReveals()
        val setupMove = prefs.setupMove()
        if (setupMove != null) prefs.setLastMove(setupMove.first, setupMove.second) else prefs.clearLastMove()
        prefs.saveBoardState(restarted)
        WidgetUpdater.render(context, appWidgetId)
    }

    private fun handleSquareTap(context: Context, appWidgetId: Int, intent: Intent) {
        val square = intent.getIntExtra(EXTRA_SQUARE, -1)
        if (square == -1) return

        val prefs = WidgetPuzzlePrefs(context, appWidgetId)
        prefs.clearReveals()
        val before = prefs.loadBoardState() ?: return
        val fromSquare = before.selectedSquare

        // First tap (select a piece), tapping empty air, or tapping the already-selected square
        // (deselect) — no move is being attempted, handle synchronously via the plain engine call.
        if (fromSquare == null || fromSquare == square) {
            prefs.saveBoardState(before.tapSquare(square))
            WidgetUpdater.render(context, appWidgetId)
            return
        }

        // Tapping another one of your own pieces re-selects it — not a move attempt either.
        if (before.isOwnPieceAt(square)) {
            prefs.saveBoardState(before.tapSquare(square))
            WidgetUpdater.render(context, appWidgetId)
            return
        }

        val flipped = prefs.isFlipped()
        val expectedMove = before.expectedMoveMatches(fromSquare, square)

        if (expectedMove != null) {
            val afterUserMove = before.applyUserMove(expectedMove)
            prefs.setLastMove(expectedMove.from, expectedMove.to)
            prefs.saveBoardState(afterUserMove)

            if (afterUserMove.status == PuzzleStatus.SOLVED) {
                confirmSolvedIfKnown(context, appWidgetId, prefs, win = true)
                Log.d(TAG, "solved(immediate) puzzleId=${prefs.puzzleId()} tainted=${prefs.isTainted()} counted=${prefs.hasCountedSolve()}")
                if (!prefs.isTainted() && !prefs.hasCountedSolve()) {
                    PuzzleStatsPrefs(context).recordPerfectSolve()
                    prefs.setCountedSolve(true)
                    Log.d(TAG, "recordPerfectSolve called, new todayCount=${PuzzleStatsPrefs(context).todayCount()}")
                }
                WidgetUpdater.render(context, appWidgetId)
                return
            }

            // Show just the solver's move, pause, then apply the opponent's scripted reply.
            WidgetUpdater.renderTransientPosition(
                context, appWidgetId, afterUserMove.position, flipped,
                lastMoveFrom = expectedMove.from, lastMoveTo = expectedMove.to
            )
            val replyMove = afterUserMove.solution[afterUserMove.solutionIndex]
            val pendingResult = goAsync()
            Handler(Looper.getMainLooper()).postDelayed({
                val afterReply = afterUserMove.applyAutoReply()
                prefs.setLastMove(replyMove.from, replyMove.to)
                prefs.saveBoardState(afterReply)
                if (afterReply.status == PuzzleStatus.SOLVED) {
                    confirmSolvedIfKnown(context, appWidgetId, prefs, win = true)
                    Log.d(TAG, "solved(afterReply) puzzleId=${prefs.puzzleId()} tainted=${prefs.isTainted()} counted=${prefs.hasCountedSolve()}")
                    if (!prefs.isTainted() && !prefs.hasCountedSolve()) {
                        PuzzleStatsPrefs(context).recordPerfectSolve()
                        prefs.setCountedSolve(true)
                        Log.d(TAG, "recordPerfectSolve called, new todayCount=${PuzzleStatsPrefs(context).todayCount()}")
                    }
                }
                WidgetUpdater.render(context, appWidgetId)
                pendingResult.finish()
            }, MOVE_PAUSE_MS)
            return
        }

        if (!before.isPseudoLegalMove(fromSquare, square)) {
            // Geometrically impossible for this piece — reject immediately, no fake move shown.
            prefs.saveBoardState(before.tapSquare(square))
            WidgetUpdater.render(context, appWidgetId)
            return
        }

        // Legal move, just not the solution: show it landing on the tapped square, pause, then revert.
        prefs.setTainted(true)
        WidgetUpdater.renderTransientPosition(context, appWidgetId, cosmeticMove(before.position, fromSquare, square), flipped)
        val pendingResult = goAsync()
        Handler(Looper.getMainLooper()).postDelayed({
            prefs.saveBoardState(before.tapSquare(square))
            WidgetUpdater.render(context, appWidgetId)
            pendingResult.finish()
        }, MOVE_PAUSE_MS)
    }

    private fun confirmSolvedIfKnown(context: Context, appWidgetId: Int, prefs: WidgetPuzzlePrefs, win: Boolean) {
        val puzzleId = prefs.puzzleId() ?: return
        ChessPuzzleWidgetProvider.enqueueConfirm(context, appWidgetId, puzzleId, prefs.puzzleAngle(), win)
    }

    private fun cosmeticMove(position: Position, from: Int, to: Int): Position {
        val board = position.board.toMutableList()
        board[to] = board[from]
        board[from] = null
        return position.copy(board = board)
    }

    companion object {
        const val ACTION_SQUARE_TAP = "com.masc.chesspuzzlewidget.action.SQUARE_TAP"
        const val ACTION_FETCH_PUZZLE = "com.masc.chesspuzzlewidget.action.FETCH_PUZZLE"
        const val ACTION_HINT = "com.masc.chesspuzzlewidget.action.HINT"
        const val ACTION_SHOW_SOLUTION = "com.masc.chesspuzzlewidget.action.SHOW_SOLUTION"
        const val ACTION_RESTART = "com.masc.chesspuzzlewidget.action.RESTART"
        const val EXTRA_APPWIDGET_ID = "extra_appwidget_id"
        const val EXTRA_SQUARE = "extra_square"
        private const val MOVE_PAUSE_MS = 500L
        private const val TAG = "WidgetClickReceiver"
    }
}
