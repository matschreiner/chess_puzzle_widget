package com.masc.chesspuzzlewidget.auth

import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.masc.chesspuzzlewidget.R
import com.masc.chesspuzzlewidget.state.PuzzleDifficulty
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
    private var selectedDifficulty = PuzzleDifficulty.DEFAULT
    private val difficultyRows = mutableMapOf<String, TextView>()
    private lateinit var tabDifficulty: TextView
    private lateinit var tabTheme: TextView

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
        selectedDifficulty = prefs.difficulty()

        val difficultyPage = findViewById<LinearLayout>(R.id.difficulty_page)
        for ((value, _) in PuzzleDifficulty.ALL) {
            val row = TextView(this).apply {
                textSize = 16f
                setPadding(24, 28, 24, 28)
                isClickable = true
                isFocusable = true
                val outValue = android.util.TypedValue()
                theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                setBackgroundResource(outValue.resourceId)
                setOnClickListener { onSelectDifficulty(value) }
            }
            difficultyRows[value] = row
            difficultyPage.addView(row)
        }
        refreshDifficultyRows()

        val themePage = findViewById<LinearLayout>(R.id.theme_page)
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
            themePage.addView(row)
        }
        refreshAllRows()

        tabDifficulty = findViewById(R.id.tab_difficulty)
        tabTheme = findViewById(R.id.tab_theme)
        tabDifficulty.setOnClickListener { showPage(isDifficultyPage = true) }
        tabTheme.setOnClickListener { showPage(isDifficultyPage = false) }
        showPage(isDifficultyPage = true)

        findViewById<Button>(R.id.ok_button).setOnClickListener { finish() }
    }

    private fun showPage(isDifficultyPage: Boolean) {
        findViewById<LinearLayout>(R.id.difficulty_page).visibility =
            if (isDifficultyPage) android.view.View.VISIBLE else android.view.View.GONE
        findViewById<LinearLayout>(R.id.theme_page).visibility =
            if (isDifficultyPage) android.view.View.GONE else android.view.View.VISIBLE

        val activeColor = Color.WHITE
        val inactiveColor = Color.parseColor("#80FFFFFF")
        tabDifficulty.setTextColor(if (isDifficultyPage) activeColor else inactiveColor)
        tabTheme.setTextColor(if (isDifficultyPage) inactiveColor else activeColor)
    }

    private fun onSelectDifficulty(value: String) {
        selectedDifficulty = value
        refreshDifficultyRows()
        prefs.setDifficulty(value)
    }

    private fun refreshDifficultyRows() {
        for ((value, label) in PuzzleDifficulty.ALL) {
            val row = difficultyRows[value] ?: continue
            val bullet = if (value == selectedDifficulty) "●" else "○"
            val text = "$bullet  $label"
            row.text = SpannableString(text).apply {
                setSpan(RelativeSizeSpan(1.5f), 0, 1, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
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
