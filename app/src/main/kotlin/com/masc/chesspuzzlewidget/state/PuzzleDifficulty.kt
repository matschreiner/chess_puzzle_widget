package com.masc.chesspuzzlewidget.state

/** Lichess's `/api/puzzle/next` difficulty levels — relative to the user's own puzzle rating. */
object PuzzleDifficulty {
    val ALL = listOf(
        "easiest" to "Easiest",
        "easier" to "Easier",
        "normal" to "Normal",
        "harder" to "Harder",
        "hardest" to "Hardest"
    )

    const val DEFAULT = "normal"

    fun labelFor(value: String): String = ALL.firstOrNull { it.first == value }?.second ?: value
}
