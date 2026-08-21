package com.masc.chesspuzzlewidget.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.caverock.androidsvg.RenderOptions
import com.caverock.androidsvg.SVG
import com.masc.chesspuzzlewidget.engine.Position
import com.masc.chesspuzzlewidget.engine.cellForSquare
import com.masc.chesspuzzlewidget.engine.squareForCell
import kotlin.math.hypot

/** Draws a full chess position (board + pieces + selection highlight + optional solution arrow) into a Bitmap. */
object BoardRenderer {

    private val svgCache = mutableMapOf<Char, SVG>()

    fun render(
        context: Context,
        position: Position,
        selectedSquare: Int?,
        sizePx: Int,
        flipped: Boolean = false,
        lastMoveFrom: Int? = null,
        lastMoveTo: Int? = null,
        hintSquare: Int? = null,
        arrowFrom: Int? = null,
        arrowTo: Int? = null,
        lightSquareColor: Int = Color.parseColor("#F0D9B5"),
        darkSquareColor: Int = Color.parseColor("#B58863"),
        selectedSquareColor: Int = Color.parseColor("#80888888"),
        lastMoveColor: Int = Color.parseColor("#80AAA23A"),
        hintColor: Int = Color.parseColor("#4CAF50"),
        arrowColor: Int = Color.parseColor("#66BB6A")
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val squareSize = sizePx / 8f

        val lightPaint = Paint().apply { color = lightSquareColor }
        val darkPaint = Paint().apply { color = darkSquareColor }
        val selectedPaint = Paint().apply { color = selectedSquareColor }
        val lastMovePaint = Paint().apply { color = lastMoveColor }

        for (row in 0..7) {
            for (col in 0..7) {
                val square = squareForCell(row, col, flipped)

                val left = col * squareSize
                val top = row * squareSize
                val isLightSquare = (row + col) % 2 == 0
                val basePaint = if (isLightSquare) lightPaint else darkPaint
                canvas.drawRect(left, top, left + squareSize, top + squareSize, basePaint)

                val overlayPaint = when {
                    square == selectedSquare -> selectedPaint
                    square == lastMoveFrom || square == lastMoveTo -> lastMovePaint
                    else -> null
                }
                if (overlayPaint != null) {
                    canvas.drawRect(left, top, left + squareSize, top + squareSize, overlayPaint)
                }

                val piece = position.board[square]
                if (piece != null) {
                    drawPiece(context, canvas, piece, left, top, squareSize)
                }
            }
        }

        if (hintSquare != null) {
            drawHintRing(canvas, hintSquare, flipped, squareSize, hintColor)
        }

        if (arrowFrom != null && arrowTo != null) {
            drawArrow(canvas, arrowFrom, arrowTo, flipped, squareSize, arrowColor)
        }

        return bitmap
    }

    private fun drawHintRing(canvas: Canvas, square: Int, flipped: Boolean, squareSize: Float, color: Int) {
        val (row, col) = cellForSquare(square, flipped)
        val cx = (col + 0.5f) * squareSize
        val cy = (row + 0.5f) * squareSize
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            alpha = 220
            style = Paint.Style.STROKE
            strokeWidth = squareSize * 0.07f
        }
        canvas.drawCircle(cx, cy, squareSize * 0.44f, paint)
    }

    private fun drawPiece(context: Context, canvas: Canvas, piece: Char, left: Float, top: Float, squareSize: Float) {
        val svg = loadSvg(context, piece)
        val padding = squareSize * 0.03f

        canvas.save()
        canvas.translate(left + padding, top + padding)
        svg.renderToCanvas(
            canvas,
            RenderOptions.create().viewPort(0f, 0f, squareSize - 2 * padding, squareSize - 2 * padding)
        )
        canvas.restore()
    }

    private fun drawArrow(canvas: Canvas, from: Int, to: Int, flipped: Boolean, squareSize: Float, color: Int) {
        val (fromRow, fromCol) = cellForSquare(from, flipped)
        val (toRow, toCol) = cellForSquare(to, flipped)
        val fromCx = (fromCol + 0.5f) * squareSize
        val fromCy = (fromRow + 0.5f) * squareSize
        val toCx = (toCol + 0.5f) * squareSize
        val toCy = (toRow + 0.5f) * squareSize

        val dx = toCx - fromCx
        val dy = toCy - fromCy
        val length = hypot(dx, dy)
        if (length == 0f) return
        val ux = dx / length
        val uy = dy / length
        val perpX = -uy
        val perpY = ux

        val headLength = squareSize * 0.4f
        val headWidth = squareSize * 0.3f
        val shaftEndX = toCx - ux * headLength
        val shaftEndY = toCy - uy * headLength

        val shaftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            alpha = 150
            style = Paint.Style.STROKE
            strokeWidth = squareSize * 0.18f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(fromCx, fromCy, shaftEndX, shaftEndY, shaftPaint)

        val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            alpha = 150
            style = Paint.Style.FILL
        }
        val headPath = Path().apply {
            moveTo(toCx, toCy)
            lineTo(shaftEndX + perpX * headWidth, shaftEndY + perpY * headWidth)
            lineTo(shaftEndX - perpX * headWidth, shaftEndY - perpY * headWidth)
            close()
        }
        canvas.drawPath(headPath, headPaint)
    }

    private fun loadSvg(context: Context, piece: Char): SVG =
        svgCache.getOrPut(piece) {
            val color = if (piece.isUpperCase()) "w" else "b"
            val kind = piece.uppercaseChar()
            SVG.getFromAsset(context.assets, "pieces/$color$kind.svg")
        }
}
