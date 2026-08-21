package com.masc.chesspuzzlewidget.network

import kotlinx.serialization.Serializable

/**
 * Minimal view of Lichess's `PuzzleAndGame` response shape (shared by
 * `/api/puzzle/daily`, `/api/puzzle/next` and `/api/puzzle/{id}`). Unknown fields are
 * ignored by the [kotlinx.serialization.json.Json] instance that parses this, so schema
 * drift in fields we don't use won't break parsing.
 *
 * Unlike `/api/puzzle/daily`, `/api/puzzle/next` does NOT include `puzzle.fen` directly —
 * only `game.pgn`, requiring the starting position to be reconstructed by replaying the
 * whole game (see [com.masc.chesspuzzlewidget.engine.replayPgn]). `puzzle.fen` is kept as
 * an optional fast path for endpoints that do provide it.
 */
@Serializable
data class PuzzleAndGameDto(
    val game: GameDto,
    val puzzle: PuzzleDto
)

@Serializable
data class GameDto(
    val pgn: String
)

@Serializable
data class PuzzleDto(
    val id: String,
    val rating: Int = 0,
    val solution: List<String>,
    val themes: List<String> = emptyList(),
    val fen: String? = null
)

@Serializable
data class SolveRequestDto(val solutions: List<SolutionEntryDto>)

@Serializable
data class SolutionEntryDto(val id: String, val win: Boolean, val rated: Boolean = false)
