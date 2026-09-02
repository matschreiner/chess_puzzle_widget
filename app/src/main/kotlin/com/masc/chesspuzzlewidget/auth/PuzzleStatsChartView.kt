package com.masc.chesspuzzlewidget.auth

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** A simple bar chart: dates on the X axis, puzzles-solved count on the Y axis. */
class PuzzleStatsChartView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private var entries: List<Pair<LocalDate, Int>> = emptyList()
    private val dateFormatter = DateTimeFormatter.ofPattern("MM/dd")

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#6AADD5") }
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#66FFFFFF")
        strokeWidth = 2f
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CCFFFFFF")
        textSize = 22f
        textAlign = Paint.Align.CENTER
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 22f
        textAlign = Paint.Align.CENTER
    }

    fun setData(data: List<Pair<LocalDate, Int>>) {
        entries = data
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (entries.isEmpty()) return

        val leftPad = 16f
        val rightPad = 16f
        val topPad = 32f
        val bottomPad = 60f
        val chartWidth = width - leftPad - rightPad
        val chartHeight = height - topPad - bottomPad
        val baselineY = topPad + chartHeight

        canvas.drawLine(leftPad, baselineY, width - rightPad, baselineY, axisPaint)

        val maxCount = (entries.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
        val slotWidth = chartWidth / entries.size
        val barWidth = slotWidth * 0.5f

        entries.forEachIndexed { index, (date, count) ->
            val slotCenterX = leftPad + slotWidth * index + slotWidth / 2f
            val barHeight = if (count == 0) 0f else chartHeight * (count.toFloat() / maxCount)
            val barTop = baselineY - barHeight

            if (count > 0) {
                canvas.drawRect(
                    slotCenterX - barWidth / 2f, barTop,
                    slotCenterX + barWidth / 2f, baselineY,
                    barPaint
                )
                canvas.drawText(count.toString(), slotCenterX, barTop - 8f, valuePaint)
            }

            canvas.drawText(date.format(dateFormatter), slotCenterX, height - bottomPad + 36f, labelPaint)
        }
    }
}
