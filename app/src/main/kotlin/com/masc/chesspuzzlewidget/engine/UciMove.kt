package com.masc.chesspuzzlewidget.engine

import kotlin.math.abs

data class UciMove(val from: Int, val to: Int, val promotion: Char? = null) {

    override fun toString(): String =
        squareToAlgebraic(from) + squareToAlgebraic(to) + (promotion?.toString() ?: "")

    companion object {
        fun parse(uci: String): UciMove {
            require(uci.length == 4 || uci.length == 5) { "Invalid UCI move: $uci" }
            val from = algebraicToSquare(uci.substring(0, 2))
            val to = algebraicToSquare(uci.substring(2, 4))
            val promotion = if (uci.length == 5) uci[4].lowercaseChar() else null
            return UciMove(from, to, promotion)
        }
    }
}

/**
 * Applies [move] to [position], correctly handling castling (moves the rook too),
 * en passant (removes the captured pawn from beside, not on, the destination square),
 * and promotion. Does not check general chess legality (check, pins, etc.) — callers
 * are expected to only apply moves already known to be legal (e.g. a puzzle's solution).
 */
fun applyUciMove(position: Position, move: UciMove): Position {
    val piece = position.board[move.from]
        ?: error("No piece on ${squareToAlgebraic(move.from)}")

    val isPawn = piece == 'P' || piece == 'p'
    val isKing = piece == 'K' || piece == 'k'
    val isCastle = isKing && abs(fileOf(move.to) - fileOf(move.from)) == 2
    val isEnPassant = isPawn &&
        fileOf(move.to) != fileOf(move.from) &&
        position.board[move.to] == null
    val isCapture = position.board[move.to] != null || isEnPassant

    val newBoard = position.board.toMutableList()
    newBoard[move.to] = if (move.promotion != null) {
        if (isWhitePiece(piece)) move.promotion.uppercaseChar() else move.promotion.lowercaseChar()
    } else {
        piece
    }
    newBoard[move.from] = null

    if (isCastle) {
        val rank = rankOf(move.from)
        if (fileOf(move.to) == 6) {
            val rookFrom = squareIndex(7, rank)
            val rookTo = squareIndex(5, rank)
            newBoard[rookTo] = newBoard[rookFrom]
            newBoard[rookFrom] = null
        } else {
            val rookFrom = squareIndex(0, rank)
            val rookTo = squareIndex(3, rank)
            newBoard[rookTo] = newBoard[rookFrom]
            newBoard[rookFrom] = null
        }
    }

    if (isEnPassant) {
        val capturedSquare = squareIndex(fileOf(move.to), rankOf(move.from))
        newBoard[capturedSquare] = null
    }

    val newCastlingRights = position.castlingRights.toMutableSet()
    if (isKing) {
        if (isWhitePiece(piece)) {
            newCastlingRights.remove('K')
            newCastlingRights.remove('Q')
        } else {
            newCastlingRights.remove('k')
            newCastlingRights.remove('q')
        }
    }
    fun revokeRightForRookSquare(square: Int) {
        when (square) {
            squareIndex(0, 0) -> newCastlingRights.remove('Q')
            squareIndex(7, 0) -> newCastlingRights.remove('K')
            squareIndex(0, 7) -> newCastlingRights.remove('q')
            squareIndex(7, 7) -> newCastlingRights.remove('k')
        }
    }
    revokeRightForRookSquare(move.from)
    revokeRightForRookSquare(move.to)

    val newEnPassantSquare = if (isPawn && abs(rankOf(move.to) - rankOf(move.from)) == 2) {
        squareIndex(fileOf(move.from), (rankOf(move.from) + rankOf(move.to)) / 2)
    } else {
        null
    }

    val newHalfmoveClock = if (isPawn || isCapture) 0 else position.halfmoveClock + 1
    val newFullmoveNumber = if (!position.whiteToMove) position.fullmoveNumber + 1 else position.fullmoveNumber

    return Position(
        board = newBoard,
        whiteToMove = !position.whiteToMove,
        castlingRights = newCastlingRights,
        enPassantSquare = newEnPassantSquare,
        halfmoveClock = newHalfmoveClock,
        fullmoveNumber = newFullmoveNumber
    )
}
