package com.masc.chesspuzzlewidget.state

/** The curated set of Lichess puzzle themes ("angles") offered in the widget's theme picker. */
object PuzzleThemes {
    val ALL = listOf("mix" to "Mixed (all themes)") + listOf(
        "advancedPawn" to "Advanced Pawn",
        "attraction" to "Attraction",
        "capturingDefender" to "Capture the Defender",
        "clearance" to "Clearance",
        "deflection" to "Deflection",
        "discoveredAttack" to "Discovered Attack",
        "doubleCheck" to "Double Check",
        "endgame" to "Endgame",
        "fork" to "Fork",
        "hangingPiece" to "Hanging Piece",
        "mateIn1" to "Mate in 1",
        "mateIn2" to "Mate in 2",
        "mateIn3" to "Mate in 3",
        "middlegame" to "Middlegame",
        "pin" to "Pin",
        "quietMove" to "Quiet Move",
        "sacrifice" to "Sacrifice",
        "skewer" to "Skewer",
        "trappedPiece" to "Trapped Piece",
        "xRayAttack" to "X-Ray Attack",
        "zugzwang" to "Zugzwang"
    )

    /** Every other real Lichess puzzle theme, nested under an "Advanced Themes" group (alphabetical). */
    val ADVANCED_THEMES = listOf(
        "anastasiaMate" to "Anastasia's Mate",
        "arabianMate" to "Arabian Mate",
        "attackingF2F7" to "Attacking F2/F7",
        "backRankMate" to "Back Rank Mate",
        "balestraMate" to "Balestra Mate",
        "bishopEndgame" to "Bishop Endgame",
        "blindSwineMate" to "Blind Swine Mate",
        "bodenMate" to "Boden's Mate",
        "castling" to "Castling",
        "collinearMove" to "Collinear Move",
        "cornerMate" to "Corner Mate",
        "defensiveMove" to "Defensive Move",
        "discoveredCheck" to "Discovered Check",
        "doubleBishopMate" to "Double Bishop Mate",
        "dovetailMate" to "Dovetail Mate",
        "enPassant" to "En Passant",
        "epauletteMate" to "Epaulette Mate",
        "exposedKing" to "Exposed King",
        "hookMate" to "Hook Mate",
        "interference" to "Interference",
        "intermezzo" to "Intermezzo",
        "killBoxMate" to "Kill Box Mate",
        "kingsideAttack" to "Kingside Attack",
        "knightEndgame" to "Knight Endgame",
        "mateIn4" to "Mate in 4",
        "mateIn5" to "Mate in 5 or More",
        "morphysMate" to "Morphy's Mate",
        "operaMate" to "Opera Mate",
        "pawnEndgame" to "Pawn Endgame",
        "pillsburysMate" to "Pillsbury's Mate",
        "promotion" to "Promotion",
        "queenEndgame" to "Queen Endgame",
        "queenRookEndgame" to "Queen and Rook Endgame",
        "queensideAttack" to "Queenside Attack",
        "rookEndgame" to "Rook Endgame",
        "smotheredMate" to "Smothered Mate",
        "swallowstailMate" to "Swallow's Tail Mate",
        "triangleMate" to "Triangle Mate",
        "underPromotion" to "Underpromotion",
        "vukovicMate" to "Vuković Mate"
    )

    /** The 10 most common openings White chooses, shown under "Openings" → "White" (alphabetical). */
    val WHITE_OPENINGS = listOf(
        "Catalan_Opening" to "Catalan Opening",
        "English_Opening" to "English Opening",
        "Italian_Game" to "Italian Game",
        "Kings_Gambit" to "King's Gambit",
        "London_System" to "London System",
        "Queens_Gambit" to "Queen's Gambit",
        "Reti_Opening" to "Réti Opening",
        "Ruy_Lopez" to "Ruy Lopez",
        "Scotch_Game" to "Scotch Game",
        "Vienna_Game" to "Vienna Game"
    )

    /** The 10 most common defenses Black chooses, shown under "Openings" → "Black" (alphabetical). */
    val BLACK_OPENINGS = listOf(
        "Caro-Kann_Defense" to "Caro-Kann Defense",
        "Dutch_Defense" to "Dutch Defense",
        "French_Defense" to "French Defense",
        "Grunfeld_Defense" to "Grünfeld Defense",
        "Kings_Indian_Defense" to "King's Indian Defense",
        "Nimzo-Indian_Defense" to "Nimzo-Indian Defense",
        "Pirc_Defense" to "Pirc Defense",
        "Scandinavian_Defense" to "Scandinavian Defense",
        "Sicilian_Defense" to "Sicilian Defense",
        "Slav_Defense" to "Slav Defense"
    )

    /** Every other White-associated opening, nested under "Openings" → "White" → "Advanced" (alphabetical). */
    val ADVANCED_WHITE_OPENINGS = listOf(
        "Amar_Opening" to "Amar Opening",
        "Amazon_Attack" to "Amazon Attack",
        "Anderssens_Opening" to "Anderssen's Opening",
        "Barnes_Opening" to "Barnes Opening",
        "Bird_Opening" to "Bird Opening",
        "Bishops_Opening" to "Bishop's Opening",
        "Blackmar-Diemer_Gambit" to "Blackmar-Diemer Gambit",
        "Blackmar-Diemer_Gambit_Accepted" to "Blackmar-Diemer Gambit Accepted",
        "Blackmar-Diemer_Gambit_Declined" to "Blackmar-Diemer Gambit Declined",
        "Canard_Opening" to "Canard Opening",
        "Center_Game" to "Center Game",
        "Center_Game_Accepted" to "Center Game Accepted",
        "Clemenz_Opening" to "Clemenz Opening",
        "Danish_Gambit" to "Danish Gambit",
        "Danish_Gambit_Accepted" to "Danish Gambit Accepted",
        "Danish_Gambit_Declined" to "Danish Gambit Declined",
        "Duras_Gambit" to "Duras Gambit",
        "Four_Knights_Game" to "Four Knights Game",
        "Grob_Opening" to "Grob Opening",
        "Hungarian_Opening" to "Hungarian Opening",
        "Kadas_Opening" to "Kádas Opening",
        "Kings_Gambit_Accepted" to "King's Gambit Accepted",
        "Kings_Gambit_Declined" to "King's Gambit Declined",
        "Kings_Indian_Attack" to "King's Indian Attack",
        "Kings_Knight_Opening" to "King's Knight Opening",
        "Kings_Pawn_Game" to "King's Pawn Game",
        "Kings_Pawn_Opening" to "King's Pawn Opening",
        "Lasker_Simul_Special" to "Lasker Simul Special",
        "Mieses_Opening" to "Mieses Opening",
        "Nimzo-Larsen_Attack" to "Nimzo-Larsen Attack",
        "Paleface_Attack" to "Paleface Attack",
        "Polish_Opening" to "Polish Opening",
        "Ponziani_Opening" to "Ponziani Opening",
        "Portuguese_Opening" to "Portuguese Opening",
        "Queens_Gambit_Accepted" to "Queen's Gambit Accepted",
        "Queens_Gambit_Declined" to "Queen's Gambit Declined",
        "Queens_Pawn_Game" to "Queen's Pawn Game",
        "Rapport-Jobava_System" to "Rapport-Jobava System",
        "Rapport-Jobava_System_with_e6" to "Rapport-Jobava System (e6)",
        "Richter-Veresov_Attack" to "Richter-Veresov Attack",
        "Rubinstein_Opening" to "Rubinstein Opening",
        "Saragossa_Opening" to "Saragossa Opening",
        "Sodium_Attack" to "Sodium Attack",
        "Three_Knights_Opening" to "Three Knights Opening",
        "Torre_Attack" to "Torre Attack",
        "Trompowsky_Attack" to "Trompowsky Attack",
        "Van_Geet_Opening" to "Van Geet Opening",
        "Vant_Kruijs_Opening" to "Van't Kruijs Opening",
        "Vienna_Gambit_with_Max_Lange_Defense" to "Vienna Gambit (Max Lange)",
        "Ware_Opening" to "Ware Opening",
        "Yusupov-Rubinstein_System" to "Yusupov-Rubinstein System",
        "Zukertort_Opening" to "Zukertort Opening"
    )

    /** Every other Black-associated defense, nested under "Openings" → "Black" → "Advanced" (alphabetical). */
    val ADVANCED_BLACK_OPENINGS = listOf(
        "Alekhine_Defense" to "Alekhine Defense",
        "Barnes_Defense" to "Barnes Defense",
        "Benko_Gambit" to "Benko Gambit",
        "Benko_Gambit_Accepted" to "Benko Gambit Accepted",
        "Benko_Gambit_Declined" to "Benko Gambit Declined",
        "Benoni_Defense" to "Benoni Defense",
        "Blumenfeld_Countergambit" to "Blumenfeld Countergambit",
        "Bogo-Indian_Defense" to "Bogo-Indian Defense",
        "Borg_Defense" to "Borg Defense",
        "Carr_Defense" to "Carr Defense",
        "Czech_Defense" to "Czech Defense",
        "East_Indian_Defense" to "East Indian Defense",
        "Elephant_Gambit" to "Elephant Gambit",
        "English_Defense" to "English Defense",
        "Englund_Gambit" to "Englund Gambit",
        "Englund_Gambit_Declined" to "Englund Gambit Declined",
        "Fried_Fox_Defense" to "Fried Fox Defense",
        "Goldsmith_Defense" to "Goldsmith Defense",
        "Gunderam_Defense" to "Gunderam Defense",
        "Hippopotamus_Defense" to "Hippopotamus Defense",
        "Horwitz_Defense" to "Horwitz Defense",
        "Indian_Defense" to "Indian Defense",
        "Kangaroo_Defense" to "Kangaroo Defense",
        "Latvian_Gambit" to "Latvian Gambit",
        "Latvian_Gambit_Accepted" to "Latvian Gambit Accepted",
        "Lemming_Defense" to "Lemming Defense",
        "Lion_Defense" to "Lion Defense",
        "Mexican_Defense" to "Mexican Defense",
        "Mikenas_Defense" to "Mikenas Defense",
        "Modern_Defense" to "Modern Defense",
        "Neo-Grunfeld_Defense" to "Neo-Grünfeld Defense",
        "Nimzowitsch_Defense" to "Nimzowitsch Defense",
        "Old_Indian_Defense" to "Old Indian Defense",
        "Owen_Defense" to "Owen Defense",
        "Petrovs_Defense" to "Petrov's Defense",
        "Philidor_Defense" to "Philidor Defense",
        "Polish_Defense" to "Polish Defense",
        "Pseudo_Queens_Indian_Defense" to "Pseudo Queen's Indian Defense",
        "Pterodactyl_Defense" to "Pterodactyl Defense",
        "Queens_Indian_Accelerated" to "Queen's Indian Accelerated",
        "Queens_Indian_Defense" to "Queen's Indian Defense",
        "Rat_Defense" to "Rat Defense",
        "Robatsch_Defense" to "Robatsch Defense",
        "Semi-Slav_Defense" to "Semi-Slav Defense",
        "Semi-Slav_Defense_Accepted" to "Semi-Slav Defense Accepted",
        "Slav_Indian" to "Slav Indian",
        "St_George_Defense" to "St. George Defense",
        "Tarrasch_Defense" to "Tarrasch Defense",
        "Wade_Defense" to "Wade Defense",
        "Ware_Defense" to "Ware Defense"
    )

    private val labelLookup: Map<String, String> by lazy {
        (ALL + ADVANCED_THEMES + WHITE_OPENINGS + BLACK_OPENINGS + ADVANCED_WHITE_OPENINGS + ADVANCED_BLACK_OPENINGS).toMap()
    }

    fun labelFor(angle: String): String {
        val (base, color) = parseAngleSelection(angle)
        labelLookup[angle]?.let { return it }
        val baseLabel = labelLookup[base] ?: base
        return when (color) {
            "white" -> "$baseLabel (White)"
            "black" -> "$baseLabel (Black)"
            else -> baseLabel
        }
    }
}

/**
 * Stored angle selections can encode an optional color filter as "angle:color" (only used by
 * the two "Openings as White/Black" entries) — this splits that back into the real Lichess
 * `angle` value and an optional `color` value for the `/api/puzzle/next` call.
 */
data class AngleSelection(val angle: String, val color: String?)

fun parseAngleSelection(raw: String): AngleSelection {
    val parts = raw.split(":", limit = 2)
    return if (parts.size == 2) AngleSelection(parts[0], parts[1]) else AngleSelection(raw, null)
}
