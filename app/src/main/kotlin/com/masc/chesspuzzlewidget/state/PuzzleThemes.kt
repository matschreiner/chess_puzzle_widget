package com.masc.chesspuzzlewidget.state

/** The curated set of Lichess puzzle themes ("angles") offered in the widget's theme picker. */
object PuzzleThemes {
    val ALL = listOf(
        "mix" to "Mixed (all themes)",
        "pin" to "Pin",
        "fork" to "Fork",
        "skewer" to "Skewer",
        "discoveredAttack" to "Discovered Attack",
        "doubleCheck" to "Double Check",
        "sacrifice" to "Sacrifice",
        "deflection" to "Deflection",
        "attraction" to "Attraction",
        "xRayAttack" to "X-Ray Attack",
        "hangingPiece" to "Hanging Piece",
        "trappedPiece" to "Trapped Piece",
        "zugzwang" to "Zugzwang",
        "endgame" to "Endgame",
        "middlegame" to "Middlegame",
        "opening" to "Opening",
        "mateIn1" to "Mate in 1",
        "mateIn2" to "Mate in 2",
        "mateIn3" to "Mate in 3",
        "advancedPawn" to "Advanced Pawn",
        "quietMove" to "Quiet Move"
    )

    fun labelFor(angle: String): String = ALL.firstOrNull { it.first == angle }?.second ?: angle
}
