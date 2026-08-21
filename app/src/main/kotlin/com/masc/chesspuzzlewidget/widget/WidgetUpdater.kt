package com.masc.chesspuzzlewidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import android.widget.RemoteViews
import com.masc.chesspuzzlewidget.R
import com.masc.chesspuzzlewidget.auth.OAuthLoginActivity
import com.masc.chesspuzzlewidget.auth.ThemeConfigActivity
import com.masc.chesspuzzlewidget.engine.Position
import com.masc.chesspuzzlewidget.engine.PuzzleStatus
import com.masc.chesspuzzlewidget.engine.squareForCell
import com.masc.chesspuzzlewidget.render.BoardRenderer
import com.masc.chesspuzzlewidget.state.PuzzleThemes
import com.masc.chesspuzzlewidget.state.WidgetPuzzlePrefs
import com.masc.chesspuzzlewidget.state.WidgetStatus

/** Shared render logic used by both [ChessPuzzleWidgetProvider] and [WidgetClickReceiver]. */
object WidgetUpdater {

    private const val BOARD_BITMAP_SIZE_PX = 480
    private const val REQUEST_CODE_ACTION_OFFSET = 64

    fun render(context: Context, appWidgetId: Int) {
        val prefs = WidgetPuzzlePrefs(context, appWidgetId)
        val views = RemoteViews(context.packageName, R.layout.widget_chess_puzzle)

        when (prefs.status()) {
            WidgetStatus.NEEDS_LOGIN -> renderLogin(context, views, appWidgetId)
            WidgetStatus.LOADING -> renderLoading(context, views)
            WidgetStatus.ERROR -> renderError(context, views, appWidgetId)
            WidgetStatus.READY -> renderPuzzle(context, views, appWidgetId, prefs)
        }

        AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views)
    }

    /**
     * Transient, non-persisted frame used to stage a move in two steps (your move, then a pause,
     * then the opponent's reply; or a wrong move, then a pause, then reverting) — [flipped] is
     * passed in explicitly (rather than derived from [position]) so the board orientation stays
     * fixed for the whole staged sequence instead of flipping back and forth mid-exchange.
     */
    fun renderTransientPosition(
        context: Context,
        appWidgetId: Int,
        position: Position,
        flipped: Boolean,
        lastMoveFrom: Int? = null,
        lastMoveTo: Int? = null
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_chess_puzzle)
        paintBoardAndHeader(
            context, views, appWidgetId, position, selectedSquare = null, flipped = flipped,
            lastMoveFrom = lastMoveFrom, lastMoveTo = lastMoveTo
        )
        views.setViewVisibility(R.id.status_overlay, View.GONE)
        AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views)
    }

    private fun renderLogin(context: Context, views: RemoteViews, appWidgetId: Int) {
        showBoard(views, visible = false)
        showStatusOverlay(context, views, showProgress = false, textRes = R.string.status_login)
        val intent = Intent(context, OAuthLoginActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.status_overlay, pendingIntent)
    }

    private fun renderLoading(context: Context, views: RemoteViews) {
        showBoard(views, visible = false)
        showStatusOverlay(context, views, showProgress = true, textRes = R.string.status_loading)
    }

    private fun renderError(context: Context, views: RemoteViews, appWidgetId: Int) {
        showBoard(views, visible = false)
        showStatusOverlay(context, views, showProgress = false, textRes = R.string.status_error)
        val lastError = WidgetPuzzlePrefs(context, appWidgetId).lastError()
        if (lastError != null) {
            views.setTextViewText(R.id.status_text, "${context.getString(R.string.status_error)}\n$lastError")
        }
        views.setOnClickPendingIntent(
            R.id.status_overlay,
            fetchPuzzlePendingIntent(context, appWidgetId)
        )
    }

    private fun renderPuzzle(context: Context, views: RemoteViews, appWidgetId: Int, prefs: WidgetPuzzlePrefs) {
        val boardState = prefs.loadBoardState()
        if (boardState == null) {
            renderLoading(context, views)
            return
        }

        val flipped = prefs.isFlipped()
        val hasNextMove = boardState.solutionIndex < boardState.solution.size
        val nextMove = if (hasNextMove) boardState.solution[boardState.solutionIndex] else null

        val hintSquare = if (prefs.isHintRequested() && nextMove != null) nextMove.from else null
        val arrowFrom = if (prefs.isSolutionRequested() && nextMove != null) nextMove.from else null
        val arrowTo = if (prefs.isSolutionRequested() && nextMove != null) nextMove.to else null

        val lastMove = prefs.lastMove()
        paintBoardAndHeader(
            context, views, appWidgetId, boardState.position, boardState.selectedSquare, flipped,
            lastMoveFrom = lastMove?.first, lastMoveTo = lastMove?.second,
            hintSquare = hintSquare, arrowFrom = arrowFrom, arrowTo = arrowTo
        )

        for (row in 0..7) {
            for (col in 0..7) {
                val viewId = context.resources.getIdentifier("cell_${row}_$col", "id", context.packageName)
                if (viewId == 0) continue
                val square = squareForCell(row, col, flipped)
                views.setOnClickPendingIntent(viewId, squareTapPendingIntent(context, appWidgetId, square))
            }
        }

        views.setViewVisibility(R.id.settings_gear, View.VISIBLE)
        val themeConfigIntent = Intent(context, ThemeConfigActivity::class.java).apply {
            putExtra(WidgetClickReceiver.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        views.setOnClickPendingIntent(
            R.id.settings_gear,
            PendingIntent.getActivity(
                context,
                appWidgetId,
                themeConfigIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        views.setOnClickPendingIntent(R.id.restart_button, actionPendingIntent(context, appWidgetId, WidgetClickReceiver.ACTION_RESTART, 67))
        views.setOnClickPendingIntent(R.id.hint_button, actionPendingIntent(context, appWidgetId, WidgetClickReceiver.ACTION_HINT, 65))
        views.setOnClickPendingIntent(R.id.solution_button, actionPendingIntent(context, appWidgetId, WidgetClickReceiver.ACTION_SHOW_SOLUTION, 66))
        views.setOnClickPendingIntent(R.id.skip_button, fetchPuzzlePendingIntent(context, appWidgetId))

        if (boardState.status == PuzzleStatus.SOLVED) {
            showStatusOverlay(context, views, showProgress = false, textRes = R.string.status_solved)
            views.setOnClickPendingIntent(
                R.id.status_overlay,
                fetchPuzzlePendingIntent(context, appWidgetId)
            )
            views.setViewVisibility(R.id.solved_restart_button, View.VISIBLE)
            views.setImageViewBitmap(R.id.solved_restart_button, buildRestartIconBitmap(context))
            views.setOnClickPendingIntent(
                R.id.solved_restart_button,
                actionPendingIntent(context, appWidgetId, WidgetClickReceiver.ACTION_RESTART, 68)
            )
        } else {
            views.setViewVisibility(R.id.status_overlay, View.GONE)
            views.setViewVisibility(R.id.solved_restart_button, View.GONE)
        }
    }

    private fun paintBoardAndHeader(
        context: Context,
        views: RemoteViews,
        appWidgetId: Int,
        position: Position,
        selectedSquare: Int?,
        flipped: Boolean,
        lastMoveFrom: Int? = null,
        lastMoveTo: Int? = null,
        hintSquare: Int? = null,
        arrowFrom: Int? = null,
        arrowTo: Int? = null
    ) {
        showBoard(views, visible = true)
        val bitmap = BoardRenderer.render(
            context = context,
            position = position,
            selectedSquare = selectedSquare,
            sizePx = BOARD_BITMAP_SIZE_PX,
            flipped = flipped,
            lastMoveFrom = lastMoveFrom,
            lastMoveTo = lastMoveTo,
            hintSquare = hintSquare,
            arrowFrom = arrowFrom,
            arrowTo = arrowTo
        )
        views.setImageViewBitmap(R.id.board_image, bitmap)

        val turnText = context.getString(if (position.whiteToMove) R.string.white_to_move else R.string.black_to_move)
        val puzzlePrefs = WidgetPuzzlePrefs(context, appWidgetId)
        val puzzleAngle = puzzlePrefs.puzzleAngle()
        val headerParts = listOf(turnText, PuzzleThemes.labelFor(puzzleAngle))
        views.setTextViewText(R.id.header_bar, headerParts.joinToString("  •  "))
        views.setViewVisibility(R.id.header_bar, View.VISIBLE)
        views.setViewVisibility(R.id.footer_bar, View.VISIBLE)
    }

    /**
     * Draws the "solved, tap to restart" circle icon on a Canvas ourselves instead of relying on a
     * Unicode glyph in a TextView — font glyphs for symbols like "↺" aren't reliably centered
     * within their own bounding box across devices/fonts, so this guarantees true pixel centering.
     */
    private fun buildRestartIconBitmap(context: Context): Bitmap {
        val sizePx = 132
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = sizePx / 2f
        val cy = sizePx / 2f

        val circleColor = context.getColor(R.color.restart_button_background)
        val arrowColor = context.getColor(R.color.status_text_color)

        canvas.drawCircle(cx, cy, sizePx / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = circleColor
            style = Paint.Style.FILL
        })

        val arcRadius = sizePx * 0.26f
        val strokeWidth = sizePx * 0.065f
        val rect = RectF(cx - arcRadius, cy - arcRadius, cx + arcRadius, cy + arcRadius)
        val startAngle = 60f
        val sweepAngle = 270f
        val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = arrowColor
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawArc(rect, startAngle, sweepAngle, false, arrowPaint)

        val endAngleRad = Math.toRadians((startAngle + sweepAngle).toDouble())
        val endX = cx + arcRadius * kotlin.math.cos(endAngleRad).toFloat()
        val endY = cy + arcRadius * kotlin.math.sin(endAngleRad).toFloat()
        // Direction of travel along the arc at its end point (positive/clockwise sweep here).
        val tangentRad = endAngleRad + Math.PI / 2
        val headLen = sizePx * 0.22f
        val headWidth = sizePx * 0.14f
        // Base of the triangle sits at the arc's end point; the tip extends forward along the
        // tangent, and the two base corners sit perpendicular to THAT tangent (not the radius),
        // so the triangle actually points the way the arrow is moving.
        val tipX = endX + headLen * kotlin.math.cos(tangentRad).toFloat()
        val tipY = endY + headLen * kotlin.math.sin(tangentRad).toFloat()
        val leftX = endX + headWidth * kotlin.math.cos(tangentRad + Math.PI / 2).toFloat()
        val leftY = endY + headWidth * kotlin.math.sin(tangentRad + Math.PI / 2).toFloat()
        val rightX = endX + headWidth * kotlin.math.cos(tangentRad - Math.PI / 2).toFloat()
        val rightY = endY + headWidth * kotlin.math.sin(tangentRad - Math.PI / 2).toFloat()

        val headPath = Path().apply {
            moveTo(tipX, tipY)
            lineTo(leftX, leftY)
            lineTo(rightX, rightY)
            close()
        }
        canvas.drawPath(headPath, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = arrowColor
            style = Paint.Style.FILL
        })

        return bitmap
    }

    private fun showBoard(views: RemoteViews, visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        views.setViewVisibility(R.id.board_image, visibility)
        views.setViewVisibility(R.id.cell_grid, visibility)
    }

    private fun showStatusOverlay(context: Context, views: RemoteViews, showProgress: Boolean, textRes: Int) {
        views.setViewVisibility(R.id.status_overlay, View.VISIBLE)
        views.setViewVisibility(R.id.status_progress, if (showProgress) View.VISIBLE else View.GONE)
        views.setTextViewText(R.id.status_text, context.getString(textRes))
    }

    private fun squareTapPendingIntent(context: Context, appWidgetId: Int, square: Int): PendingIntent {
        val intent = Intent(context, WidgetClickReceiver::class.java).apply {
            action = WidgetClickReceiver.ACTION_SQUARE_TAP
            putExtra(WidgetClickReceiver.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(WidgetClickReceiver.EXTRA_SQUARE, square)
        }
        val requestCode = appWidgetId * 100 + square
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun actionPendingIntent(context: Context, appWidgetId: Int, action: String, requestCodeOffset: Int): PendingIntent {
        val intent = Intent(context, WidgetClickReceiver::class.java).apply {
            this.action = action
            putExtra(WidgetClickReceiver.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val requestCode = appWidgetId * 100 + requestCodeOffset
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun fetchPuzzlePendingIntent(context: Context, appWidgetId: Int): PendingIntent {
        val intent = Intent(context, WidgetClickReceiver::class.java).apply {
            action = WidgetClickReceiver.ACTION_FETCH_PUZZLE
            putExtra(WidgetClickReceiver.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val requestCode = appWidgetId * 100 + REQUEST_CODE_ACTION_OFFSET
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
