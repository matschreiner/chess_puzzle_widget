package com.masc.chesspuzzlewidget.engine

import kotlin.math.abs

private const val STANDARD_START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

private val SAN_REGEX = Regex("^([KQRBN])?([a-h])?([1-8])?(x)?([a-h][1-8])(=([QRBN]))?$")
private val MOVE_NUMBER_REGEX = Regex("^\\d+\\.+$")

private data class ParsedSan(
    val piece: Char?,
    val disambiguationFile: Int?,
    val disambiguationRank: Int?,
    val toSquare: Int,
    val promotion: Char?,
    val isCastleKingside: Boolean,
    val isCastleQueenside: Boolean
)

data class PgnReplayResult(val position: Position, val lastMove: UciMove?)

/**
 * Replays a space-separated SAN move list (Lichess's `game.pgn` field, with no move numbers)
 * from the standard starting position and returns the resulting [Position] plus the final move
 * played — that final move is the "setup" move that created the puzzle's tactic, shown highlighted
 * like Lichess does even before the solver has made a move.  Needed because `/api/puzzle/next`
 * (unlike `/api/puzzle/daily`) doesn't include the puzzle's FEN directly.
 */
fun replayPgnWithLastMove(pgn: String): PgnReplayResult {
    var position = FenParser.parse(STANDARD_START_FEN)
    var lastMove: UciMove? = null
    val tokens = pgn.trim().split(Regex("\\s+")).filter { it.isNotBlank() && !MOVE_NUMBER_REGEX.matches(it) }
    for (token in tokens) {
        val san = parseSan(token)
        val move = resolveSanMove(position, san)
        position = applyUciMove(position, move)
        lastMove = move
    }
    return PgnReplayResult(position, lastMove)
}

fun replayPgn(pgn: String): Position = replayPgnWithLastMove(pgn).position

private fun parseSan(tokenRaw: String): ParsedSan {
    val token = tokenRaw.trim().trimEnd('+', '#')
    if (token == "O-O" || token == "0-0") {
        return ParsedSan(null, null, null, -1, null, isCastleKingside = true, isCastleQueenside = false)
    }
    if (token == "O-O-O" || token == "0-0-0") {
        return ParsedSan(null, null, null, -1, null, isCastleKingside = false, isCastleQueenside = true)
    }

    val match = SAN_REGEX.matchEntire(token) ?: error("Cannot parse SAN move: $tokenRaw")
    val piece = match.groups[1]?.value?.get(0)
    val disambiguationFile = match.groups[2]?.value?.get(0)?.let { it - 'a' }
    val disambiguationRank = match.groups[3]?.value?.get(0)?.let { it - '1' }
    val toSquare = algebraicToSquare(match.groups[5]!!.value)
    val promotion = match.groups[7]?.value?.get(0)?.lowercaseChar()

    return ParsedSan(piece, disambiguationFile, disambiguationRank, toSquare, promotion, false, false)
}

private fun resolveSanMove(position: Position, san: ParsedSan): UciMove {
    if (san.isCastleKingside || san.isCastleQueenside) {
        val rank = if (position.whiteToMove) 0 else 7
        val kingFrom = squareIndex(4, rank)
        val kingTo = if (san.isCastleKingside) squareIndex(6, rank) else squareIndex(2, rank)
        return UciMove(kingFrom, kingTo)
    }

    val pieceType = san.piece ?: 'P'
    var candidates = (0..63).filter { square ->
        val occupant = position.board[square]
        occupant != null &&
            isWhitePiece(occupant) == position.whiteToMove &&
            occupant.uppercaseChar() == pieceType &&
            (san.disambiguationFile == null || fileOf(square) == san.disambiguationFile) &&
            (san.disambiguationRank == null || rankOf(square) == san.disambiguationRank) &&
            canReach(pieceType, square, san.toSquare, position)
    }

    // canReach ignores pins, so two pieces can look like they both reach the square even
    // though only one move is actually legal — real SAN only disambiguates when needed, so
    // fall back to filtering out candidates that would leave the mover's own king in check.
    if (candidates.size > 1) {
        candidates = candidates.filter { square ->
            !leavesOwnKingInCheck(position, UciMove(square, san.toSquare, san.promotion))
        }
    }

    require(candidates.size == 1) {
        "Ambiguous or unresolved SAN move to ${squareToAlgebraic(san.toSquare)}: ${candidates.size} candidates"
    }
    return UciMove(candidates[0], san.toSquare, san.promotion)
}

private fun leavesOwnKingInCheck(position: Position, move: UciMove): Boolean {
    val moverWasWhite = position.whiteToMove
    val after = applyUciMove(position, move)
    val kingChar = if (moverWasWhite) 'K' else 'k'
    val kingSquare = after.board.indexOf(kingChar)
    if (kingSquare == -1) return false
    return boardAttacksSquare(after.board, kingSquare, !moverWasWhite)
}

private fun boardAttacksSquare(board: List<Char?>, square: Int, byWhite: Boolean): Boolean {
    val probePosition = Position(board, byWhite, emptySet(), null, 0, 1)
    for (origin in 0..63) {
        val piece = board[origin] ?: continue
        if (isWhitePiece(piece) != byWhite) continue
        val type = piece.uppercaseChar()
        val attacks = if (type == 'P') {
            val direction = if (byWhite) 1 else -1
            abs(fileOf(square) - fileOf(origin)) == 1 && rankOf(square) - rankOf(origin) == direction
        } else {
            canReach(type, origin, square, probePosition)
        }
        if (attacks) return true
    }
    return false
}

/** Whether a piece of [pieceType] could geometrically move from [origin] to [dest] (ignores check/pins). */
fun canReach(pieceType: Char, origin: Int, dest: Int, position: Position): Boolean {
    val fileDelta = fileOf(dest) - fileOf(origin)
    val rankDelta = rankOf(dest) - rankOf(origin)
    return when (pieceType) {
        'N' -> (abs(fileDelta) == 1 && abs(rankDelta) == 2) || (abs(fileDelta) == 2 && abs(rankDelta) == 1)
        'B' -> abs(fileDelta) == abs(rankDelta) && pathIsClear(origin, dest, position)
        'R' -> (fileDelta == 0 || rankDelta == 0) && pathIsClear(origin, dest, position)
        'Q' ->
            ((fileDelta == 0 || rankDelta == 0) || abs(fileDelta) == abs(rankDelta)) &&
                pathIsClear(origin, dest, position)
        'K' -> maxOf(abs(fileDelta), abs(rankDelta)) == 1
        'P' -> canPawnReach(origin, dest, position)
        else -> false
    }
}

private fun canPawnReach(origin: Int, dest: Int, position: Position): Boolean {
    val isWhite = isWhitePiece(position.board[origin]!!)
    val direction = if (isWhite) 1 else -1
    val startRank = if (isWhite) 1 else 6
    val fileDelta = fileOf(dest) - fileOf(origin)
    val rankDelta = rankOf(dest) - rankOf(origin)

    val isDiagonalCapture = abs(fileDelta) == 1 && rankDelta == direction
    if (isDiagonalCapture) {
        val targetOccupant = position.board[dest]
        val isEnPassant = dest == position.enPassantSquare
        return targetOccupant != null && isWhitePiece(targetOccupant) != isWhite || isEnPassant
    }

    if (fileDelta != 0) return false
    if (rankDelta == direction) return position.board[dest] == null
    if (rankOf(origin) == startRank && rankDelta == 2 * direction) {
        val midSquare = squareIndex(fileOf(origin), rankOf(origin) + direction)
        return position.board[midSquare] == null && position.board[dest] == null
    }
    return false
}

private fun pathIsClear(origin: Int, dest: Int, position: Position): Boolean {
    val fileStep = (fileOf(dest) - fileOf(origin)).let { if (it > 0) 1 else if (it < 0) -1 else 0 }
    val rankStep = (rankOf(dest) - rankOf(origin)).let { if (it > 0) 1 else if (it < 0) -1 else 0 }

    var file = fileOf(origin) + fileStep
    var rank = rankOf(origin) + rankStep
    while (file != fileOf(dest) || rank != rankOf(dest)) {
        if (position.board[squareIndex(file, rank)] != null) return false
        file += fileStep
        rank += rankStep
    }
    return true
}
