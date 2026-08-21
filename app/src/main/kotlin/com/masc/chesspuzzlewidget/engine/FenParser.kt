package com.masc.chesspuzzlewidget.engine

object FenParser {

    private val CASTLING_ORDER = listOf('K', 'Q', 'k', 'q')

    fun parse(fen: String): Position {
        val parts = fen.trim().split(" ")
        require(parts.size >= 4) { "Invalid FEN, too few fields: $fen" }

        val placement = parts[0]
        val sideToMove = parts[1]
        val castling = parts[2]
        val enPassant = parts[3]
        val halfmove = parts.getOrNull(4)?.toIntOrNull() ?: 0
        val fullmove = parts.getOrNull(5)?.toIntOrNull() ?: 1

        val board = arrayOfNulls<Char>(64)
        val ranks = placement.split("/")
        require(ranks.size == 8) { "Invalid FEN placement, expected 8 ranks: $placement" }

        for (rankFromTop in ranks.indices) {
            val rank = 7 - rankFromTop
            var file = 0
            for (ch in ranks[rankFromTop]) {
                if (ch.isDigit()) {
                    file += ch - '0'
                } else {
                    require(file in 0..7) { "Invalid FEN rank overflow: ${ranks[rankFromTop]}" }
                    board[squareIndex(file, rank)] = ch
                    file += 1
                }
            }
            require(file == 8) { "Invalid FEN rank length: ${ranks[rankFromTop]}" }
        }

        val castlingRights = if (castling == "-") emptySet() else castling.toSet()
        val enPassantSquare = if (enPassant == "-") null else algebraicToSquare(enPassant)

        return Position(
            board = board.toList(),
            whiteToMove = sideToMove == "w",
            castlingRights = castlingRights,
            enPassantSquare = enPassantSquare,
            halfmoveClock = halfmove,
            fullmoveNumber = fullmove
        )
    }

    fun toFen(position: Position): String {
        val placement = StringBuilder()
        for (rankFromTop in 0..7) {
            val rank = 7 - rankFromTop
            var emptyCount = 0
            for (file in 0..7) {
                val piece = position.board[squareIndex(file, rank)]
                if (piece == null) {
                    emptyCount += 1
                } else {
                    if (emptyCount > 0) {
                        placement.append(emptyCount)
                        emptyCount = 0
                    }
                    placement.append(piece)
                }
            }
            if (emptyCount > 0) placement.append(emptyCount)
            if (rankFromTop != 7) placement.append('/')
        }

        val side = if (position.whiteToMove) "w" else "b"
        val castling = if (position.castlingRights.isEmpty()) {
            "-"
        } else {
            CASTLING_ORDER.filter { it in position.castlingRights }.joinToString("")
        }
        val enPassant = position.enPassantSquare?.let { squareToAlgebraic(it) } ?: "-"

        return "$placement $side $castling $enPassant ${position.halfmoveClock} ${position.fullmoveNumber}"
    }
}
