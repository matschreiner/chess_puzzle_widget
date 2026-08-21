package com.masc.chesspuzzlewidget.auth

import android.os.Bundle
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.masc.chesspuzzlewidget.R
import com.masc.chesspuzzlewidget.state.PuzzleThemes
import com.masc.chesspuzzlewidget.state.WidgetPuzzlePrefs
import com.masc.chesspuzzlewidget.widget.ChessPuzzleWidgetProvider
import com.masc.chesspuzzlewidget.widget.WidgetClickReceiver

private const val MIX_ANGLE = "mix"

/**
 * Lets the user toggle which Lichess puzzle themes ("angles") the widget draws from — RemoteViews
 * can't show a real dropdown or checkbox list, so this is a plain settings screen. Each tap toggles
 * that theme immediately; the actual re-fetch (with the new theme set) happens once when the screen
 * closes, not on every tap, so rapid toggling doesn't spam network requests. "Mixed" is mutually
 * exclusive with everything else: picking it clears other themes, and clearing every specific theme
 * falls back to "Mixed".
 */
class ThemeConfigActivity : AppCompatActivity() {

    private var appWidgetId = -1
    private lateinit var prefs: WidgetPuzzlePrefs
    private val selected = mutableSetOf<String>()
    private val rows = mutableMapOf<String, TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_theme_config)

        appWidgetId = intent.getIntExtra(WidgetClickReceiver.EXTRA_APPWIDGET_ID, -1)
        if (appWidgetId == -1) {
            finish()
            return
        }

        prefs = WidgetPuzzlePrefs(this, appWidgetId)
        selected += prefs.selectedAngles()

        val container = findViewById<LinearLayout>(R.id.theme_list_container)
        for ((angle, _) in PuzzleThemes.ALL) {
            val row = TextView(this).apply {
                textSize = 16f
                setPadding(24, 28, 24, 28)
                isClickable = true
                isFocusable = true
                val outValue = android.util.TypedValue()
                theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                setBackgroundResource(outValue.resourceId)
                setOnClickListener { onToggle(angle) }
            }
            rows[angle] = row
            container.addView(row)
        }
        refreshAllRows()

        findViewById<Button>(R.id.ok_button).setOnClickListener { finish() }
    }

    private fun onToggle(angle: String) {
        if (angle == MIX_ANGLE) {
            selected.clear()
            selected.add(MIX_ANGLE)
        } else {
            selected.remove(MIX_ANGLE)
            if (angle in selected) {
                selected.remove(angle)
            } else {
                selected.add(angle)
            }
            if (selected.isEmpty()) selected.add(MIX_ANGLE)
        }
        refreshAllRows()
        prefs.setSelectedAngles(selected)
    }

    private fun refreshAllRows() {
        for ((angle, label) in PuzzleThemes.ALL) {
            val row = rows[angle] ?: continue
            val bullet = if (angle in selected) "●" else "○"
            val text = "$bullet  $label"
            row.text = SpannableString(text).apply {
                setSpan(RelativeSizeSpan(1.5f), 0, 1, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (appWidgetId != -1) {
            prefs.clearStagedPuzzle()
            ChessPuzzleWidgetProvider.forceFetch(this, appWidgetId)
        }
    }
}
