package com.masc.chesspuzzlewidget.engine

/**
 * A chess position. `board` has 64 entries indexed as `rank * 8 + file`,
 * where rank 0 is rank "1" and file 0 is file "a" (so index 0 = a1, index 63 = h8).
 * Pieces are FEN letters: uppercase = white, lowercase = black, null = empty.
 */
data class Position(
    val board: List<Char?>,
    val whiteToMove: Boolean,
    val castlingRights: Set<Char>,
    val enPassantSquare: Int?,
    val halfmoveClock: Int,
    val fullmoveNumber: Int
) {
    init {
        require(board.size == 64) { "Board must have exactly 64 squares, got ${board.size}" }
    }
}

fun fileOf(square: Int): Int = square % 8

fun rankOf(square: Int): Int = square / 8

fun squareIndex(file: Int, rank: Int): Int = rank * 8 + file

/**
 * Maps a widget grid cell (row 0 = top, col 0 = left) to a board square, accounting for
 * orientation: unflipped shows White at the bottom (row 7 = rank 1); flipped shows Black
 * at the bottom (row 7 = rank 8), matching Lichess's convention of orienting the board
 * toward whoever is to move.
 */
fun squareForCell(row: Int, col: Int, flipped: Boolean): Int {
    val rank = if (flipped) row else 7 - row
    val file = if (flipped) 7 - col else col
    return squareIndex(file, rank)
}

/** Inverse of [squareForCell]: the (row, col) grid cell a square is drawn at. */
fun cellForSquare(square: Int, flipped: Boolean): Pair<Int, Int> {
    val rank = rankOf(square)
    val file = fileOf(square)
    val row = if (flipped) rank else 7 - rank
    val col = if (flipped) 7 - file else file
    return row to col
}

fun algebraicToSquare(algebraic: String): Int {
    require(algebraic.length == 2) { "Invalid algebraic square: $algebraic" }
    val file = algebraic[0] - 'a'
    val rank = algebraic[1] - '1'
    require(file in 0..7 && rank in 0..7) { "Invalid algebraic square: $algebraic" }
    return squareIndex(file, rank)
}

fun squareToAlgebraic(square: Int): String {
    val file = ('a' + fileOf(square))
    val rank = ('1' + rankOf(square))
    return "$file$rank"
}

fun isWhitePiece(piece: Char): Boolean = piece.isUpperCase()
