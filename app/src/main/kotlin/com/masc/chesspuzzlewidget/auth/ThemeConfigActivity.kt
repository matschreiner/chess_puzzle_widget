package com.masc.chesspuzzlewidget.auth

import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.masc.chesspuzzlewidget.R
import com.masc.chesspuzzlewidget.state.PuzzleDifficulty
import com.masc.chesspuzzlewidget.state.PuzzleStatsPrefs
import com.masc.chesspuzzlewidget.state.PuzzleThemes
import com.masc.chesspuzzlewidget.state.WidgetPuzzlePrefs
import com.masc.chesspuzzlewidget.widget.ChessPuzzleWidgetProvider
import com.masc.chesspuzzlewidget.widget.WidgetClickReceiver
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val MIX_ANGLE = "mix"
private enum class Page { THEME, DIFFICULTY, STATS }

/**
 * Lets the user toggle which Lichess puzzle themes ("angles") the widget draws from — RemoteViews
 * can't show a real dropdown or checkbox list, so this is a plain settings screen. Each tap toggles
 * that theme immediately; the actual re-fetch (with the new theme set) happens once when the screen
 * closes, not on every tap, so rapid toggling doesn't spam network requests. "Mixed" is mutually
 * exclusive with everything else: picking it clears other themes, and clearing every specific theme
 * falls back to "Mixed".
 *
 * "Openings" is a 4-level collapsible group (there are far too many named openings to show flat):
 * Openings → White / Black → the 10 most common → nested "Advanced Openings" → everything else.
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
    private lateinit var tabStats: TextView
    private val statsDayRows = mutableMapOf<LocalDate, TextView>()
    private val statsDayDetailContainers = mutableMapOf<LocalDate, LinearLayout>()
    private val statsDayExpanded = mutableMapOf<LocalDate, Boolean>()

    private lateinit var openingsHeaderRow: TextView
    private var openingsExpanded = false

    private lateinit var whiteHeaderRow: TextView
    private var whiteExpanded = false
    private val whiteRows = mutableMapOf<String, TextView>()
    private lateinit var whiteAdvancedHeaderRow: TextView
    private var whiteAdvancedExpanded = false
    private val whiteAdvancedRows = mutableMapOf<String, TextView>()

    private lateinit var blackHeaderRow: TextView
    private var blackExpanded = false
    private val blackRows = mutableMapOf<String, TextView>()
    private lateinit var blackAdvancedHeaderRow: TextView
    private var blackAdvancedExpanded = false
    private val blackAdvancedRows = mutableMapOf<String, TextView>()

    private lateinit var advancedThemesHeaderRow: TextView
    private var advancedThemesExpanded = false
    private val advancedThemesRows = mutableMapOf<String, TextView>()

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
            val row = buildRow(24) { onSelectDifficulty(value) }
            difficultyRows[value] = row
            difficultyPage.addView(row)
        }
        refreshDifficultyRows()

        val themePage = findViewById<LinearLayout>(R.id.theme_page)

        // "Mixed" first, then the "Openings" group second, then the rest of the tactical themes.
        val mixRow = buildRow(24) { onToggle(MIX_ANGLE) }
        rows[MIX_ANGLE] = mixRow
        themePage.addView(mixRow)

        openingsHeaderRow = buildRow(24) {
            openingsExpanded = !openingsExpanded
            refreshOpeningsHeader()
            updateSubGroupVisibility()
        }
        themePage.addView(openingsHeaderRow)

        whiteHeaderRow = buildRow(64) {
            whiteExpanded = !whiteExpanded
            refreshSubHeader(whiteHeaderRow, "White Openings", whiteExpanded)
            updateSubGroupVisibility()
        }
        themePage.addView(whiteHeaderRow)
        for ((angle, _) in PuzzleThemes.WHITE_OPENINGS) {
            val row = buildRow(104) { onToggle(angle) }
            whiteRows[angle] = row
            themePage.addView(row)
        }
        whiteAdvancedHeaderRow = buildRow(104) {
            whiteAdvancedExpanded = !whiteAdvancedExpanded
            refreshSubHeader(whiteAdvancedHeaderRow, "Advanced Openings", whiteAdvancedExpanded)
            updateSubGroupVisibility()
        }
        themePage.addView(whiteAdvancedHeaderRow)
        for ((angle, _) in PuzzleThemes.ADVANCED_WHITE_OPENINGS) {
            val row = buildRow(144) { onToggle(angle) }
            whiteAdvancedRows[angle] = row
            themePage.addView(row)
        }

        blackHeaderRow = buildRow(64) {
            blackExpanded = !blackExpanded
            refreshSubHeader(blackHeaderRow, "Black Openings", blackExpanded)
            updateSubGroupVisibility()
        }
        themePage.addView(blackHeaderRow)
        for ((angle, _) in PuzzleThemes.BLACK_OPENINGS) {
            val row = buildRow(104) { onToggle(angle) }
            blackRows[angle] = row
            themePage.addView(row)
        }
        blackAdvancedHeaderRow = buildRow(104) {
            blackAdvancedExpanded = !blackAdvancedExpanded
            refreshSubHeader(blackAdvancedHeaderRow, "Advanced Openings", blackAdvancedExpanded)
            updateSubGroupVisibility()
        }
        themePage.addView(blackAdvancedHeaderRow)
        for ((angle, _) in PuzzleThemes.ADVANCED_BLACK_OPENINGS) {
            val row = buildRow(144) { onToggle(angle) }
            blackAdvancedRows[angle] = row
            themePage.addView(row)
        }

        for ((angle, _) in PuzzleThemes.ALL) {
            if (angle == MIX_ANGLE) continue
            val row = buildRow(24) { onToggle(angle) }
            rows[angle] = row
            themePage.addView(row)
        }

        advancedThemesHeaderRow = buildRow(24) {
            advancedThemesExpanded = !advancedThemesExpanded
            refreshSubHeader(advancedThemesHeaderRow, "Advanced Themes", advancedThemesExpanded)
            updateSubGroupVisibility()
        }
        themePage.addView(advancedThemesHeaderRow)
        for ((angle, _) in PuzzleThemes.ADVANCED_THEMES) {
            val row = buildRow(64) { onToggle(angle) }
            advancedThemesRows[angle] = row
            themePage.addView(row)
        }

        refreshAllBullets()
        refreshOpeningsHeader()
        refreshSubHeader(whiteHeaderRow, "White Openings", whiteExpanded)
        refreshSubHeader(whiteAdvancedHeaderRow, "Advanced Openings", whiteAdvancedExpanded)
        refreshSubHeader(blackHeaderRow, "Black Openings", blackExpanded)
        refreshSubHeader(blackAdvancedHeaderRow, "Advanced Openings", blackAdvancedExpanded)
        refreshSubHeader(advancedThemesHeaderRow, "Advanced Themes", advancedThemesExpanded)
        updateSubGroupVisibility()

        buildStatsPage()

        tabDifficulty = findViewById(R.id.tab_difficulty)
        tabTheme = findViewById(R.id.tab_theme)
        tabStats = findViewById(R.id.tab_stats)
        tabDifficulty.setOnClickListener { showPage(Page.DIFFICULTY) }
        tabTheme.setOnClickListener { showPage(Page.THEME) }
        tabStats.setOnClickListener { showPage(Page.STATS) }
        showPage(Page.THEME)

        findViewById<Button>(R.id.ok_button).setOnClickListener { finish() }
    }

    /** Bar chart (last 14 days) + a collapsible per-day list of every puzzle's full record. */
    private fun buildStatsPage() {
        val statsPage = findViewById<LinearLayout>(R.id.stats_page)
        val stats = PuzzleStatsPrefs(this)

        val today = LocalDate.now()
        val chartData = (13 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            date to stats.historyFor(date).size
        }
        val chart = PuzzleStatsChartView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 480)
            setData(chartData)
        }
        statsPage.addView(chart)

        val dates = stats.historyDates()
        if (dates.isEmpty()) {
            statsPage.addView(buildRow(24) {}.apply {
                text = "No puzzles solved yet."
                isClickable = false
                isFocusable = false
            })
            return
        }

        for (date in dates) {
            val records = stats.historyFor(date)
            val perfectCount = stats.countFor(date)
            statsDayExpanded[date] = false

            val headerRow = buildRow(24) {
                val expanded = !(statsDayExpanded[date] ?: false)
                statsDayExpanded[date] = expanded
                refreshStatsDayHeader(date, records.size, perfectCount, expanded)
                statsDayDetailContainers[date]?.visibility = if (expanded) View.VISIBLE else View.GONE
            }
            statsDayRows[date] = headerRow
            statsPage.addView(headerRow)
            refreshStatsDayHeader(date, records.size, perfectCount, false)

            val detailContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                visibility = View.GONE
            }
            statsDayDetailContainers[date] = detailContainer
            for (record in records) {
                val themesText = record.themes.joinToString(", ") { PuzzleThemes.labelFor(it) }
                val helpText = when {
                    record.usedSolution -> "Solution used"
                    record.usedHint -> "Hint used"
                    else -> "No help used"
                }
                val row = buildRow(64) {}.apply {
                    text = "Rating ${record.rating} · $themesText\n$helpText"
                    textSize = 13f
                    isClickable = false
                    isFocusable = false
                }
                detailContainer.addView(row)
            }
            statsPage.addView(detailContainer)
        }
    }

    private fun refreshStatsDayHeader(date: LocalDate, total: Int, perfect: Int, expanded: Boolean) {
        val row = statsDayRows[date] ?: return
        val chevron = if (expanded) "▾" else "▸"
        val dateLabel = date.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
        row.text = "$chevron  $dateLabel — $total solved ($perfect perfect)"
    }

    /** Every row — at every nesting depth — shares the same text/bullet size; only [paddingStartPx] varies. */
    private fun buildRow(paddingStartPx: Int, onClick: () -> Unit): TextView =
        TextView(this).apply {
            textSize = 16f
            setPadding(paddingStartPx, 16, 24, 16)
            isClickable = true
            isFocusable = true
            val outValue = TypedValue()
            theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            setBackgroundResource(outValue.resourceId)
            setOnClickListener { onClick() }
        }

    private fun showPage(page: Page) {
        findViewById<LinearLayout>(R.id.difficulty_page).visibility = if (page == Page.DIFFICULTY) View.VISIBLE else View.GONE
        findViewById<LinearLayout>(R.id.theme_page).visibility = if (page == Page.THEME) View.VISIBLE else View.GONE
        findViewById<LinearLayout>(R.id.stats_page).visibility = if (page == Page.STATS) View.VISIBLE else View.GONE

        val activeColor = Color.WHITE
        val inactiveColor = Color.parseColor("#80FFFFFF")
        tabDifficulty.setTextColor(if (page == Page.DIFFICULTY) activeColor else inactiveColor)
        tabTheme.setTextColor(if (page == Page.THEME) activeColor else inactiveColor)
        tabStats.setTextColor(if (page == Page.STATS) activeColor else inactiveColor)
    }

    private fun onSelectDifficulty(value: String) {
        selectedDifficulty = value
        refreshDifficultyRows()
        prefs.setDifficulty(value)
    }

    private fun refreshDifficultyRows() {
        for ((value, label) in PuzzleDifficulty.ALL) {
            val row = difficultyRows[value] ?: continue
            row.text = bulletText(value == selectedDifficulty, label)
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
        refreshAllBullets()
        prefs.setSelectedAngles(selected)
    }

    private fun refreshAllBullets() {
        applyBullets(rows, PuzzleThemes.ALL)
        applyBullets(whiteRows, PuzzleThemes.WHITE_OPENINGS)
        applyBullets(blackRows, PuzzleThemes.BLACK_OPENINGS)
        applyBullets(whiteAdvancedRows, PuzzleThemes.ADVANCED_WHITE_OPENINGS)
        applyBullets(blackAdvancedRows, PuzzleThemes.ADVANCED_BLACK_OPENINGS)
        applyBullets(advancedThemesRows, PuzzleThemes.ADVANCED_THEMES)
    }

    private fun applyBullets(map: Map<String, TextView>, list: List<Pair<String, String>>) {
        for ((angle, label) in list) {
            val row = map[angle] ?: continue
            row.text = bulletText(angle in selected, label)
        }
    }

    private fun bulletText(selected: Boolean, label: String): SpannableString {
        val bullet = if (selected) "●" else "○"
        return SpannableString("$bullet  $label").apply {
            setSpan(RelativeSizeSpan(1.5f), 0, 1, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    /** "▸ Openings" (collapsed) / "▾ Openings" (expanded) — a symbol showing it can be expanded. */
    private fun refreshOpeningsHeader() {
        // No bullet drawn here, but padded to roughly match where the label text starts on the
        // other rows (which all lead with a "●/○  " bullet before their label).
        openingsHeaderRow.text = "     Openings"
    }

    private fun refreshSubHeader(row: TextView, label: String, expanded: Boolean) {
        val chevron = if (expanded) "▾" else "▸"
        row.text = "$chevron  $label"
    }

    private fun updateSubGroupVisibility() {
        fun vis(visible: Boolean) = if (visible) View.VISIBLE else View.GONE
        whiteHeaderRow.visibility = vis(openingsExpanded)
        blackHeaderRow.visibility = vis(openingsExpanded)
        for (row in whiteRows.values) row.visibility = vis(openingsExpanded && whiteExpanded)
        whiteAdvancedHeaderRow.visibility = vis(openingsExpanded && whiteExpanded)
        for (row in whiteAdvancedRows.values) {
            row.visibility = vis(openingsExpanded && whiteExpanded && whiteAdvancedExpanded)
        }
        for (row in blackRows.values) row.visibility = vis(openingsExpanded && blackExpanded)
        blackAdvancedHeaderRow.visibility = vis(openingsExpanded && blackExpanded)
        for (row in blackAdvancedRows.values) {
            row.visibility = vis(openingsExpanded && blackExpanded && blackAdvancedExpanded)
        }
        for (row in advancedThemesRows.values) row.visibility = vis(advancedThemesExpanded)
    }

    override fun onPause() {
        super.onPause()
        if (appWidgetId != -1) {
            prefs.clearStagedPuzzle()
            ChessPuzzleWidgetProvider.forceFetch(this, appWidgetId)
        }
    }
}
