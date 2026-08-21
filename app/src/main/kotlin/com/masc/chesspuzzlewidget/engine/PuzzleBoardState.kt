package com.masc.chesspuzzlewidget.engine

enum class PuzzleStatus { AWAITING_MOVE, SOLVED }

/**
 * Tracks solving progress through a single Lichess puzzle: the current position,
 * the known solution (as UCI moves), how far into it we are, and any in-progress
 * square selection. Does not implement general chess legality — a user's move is
 * only ever compared against the next expected move in [solution].
 */
data class PuzzleBoardState(
    val position: Position,
    val solution: List<UciMove>,
    val solutionIndex: Int,
    val selectedSquare: Int? = null,
    val status: PuzzleStatus = PuzzleStatus.AWAITING_MOVE
) {

    /** Handles a single square tap, whether it's a selection or a move attempt. */
    fun tapSquare(square: Int): PuzzleBoardState {
        if (status == PuzzleStatus.SOLVED) return this

        val from = selectedSquare
        if (from == null) {
            val piece = position.board[square] ?: return this
            return if (isWhitePiece(piece) == position.whiteToMove) copy(selectedSquare = square) else this
        }

        if (from == square) return copy(selectedSquare = null)

        return attemptMove(from, square)
    }

    private fun attemptMove(from: Int, to: Int): PuzzleBoardState {
        if (solutionIndex >= solution.size) return copy(selectedSquare = null)

        val piece = position.board[from] ?: return copy(selectedSquare = null)
        val isPromotion = (piece == 'P' && rankOf(to) == 7) || (piece == 'p' && rankOf(to) == 0)
        val candidate = if (isPromotion) UciMove(from, to, 'q') else UciMove(from, to)
        val expected = solution[solutionIndex]

        if (candidate != expected) {
            val tappedPiece = position.board[to]
            val reselect = tappedPiece != null && isWhitePiece(tappedPiece) == position.whiteToMove
            return copy(selectedSquare = if (reselect) to else null)
        }

        var newPosition = applyUciMove(position, candidate)
        var newIndex = solutionIndex + 1
        if (newIndex < solution.size) {
            newPosition = applyUciMove(newPosition, solution[newIndex])
            newIndex += 1
        }
        val newStatus = if (newIndex >= solution.size) PuzzleStatus.SOLVED else PuzzleStatus.AWAITING_MOVE

        return copy(
            position = newPosition,
            solutionIndex = newIndex,
            selectedSquare = null,
            status = newStatus
        )
    }

    fun isOwnPieceAt(square: Int): Boolean {
        val piece = position.board[square] ?: return false
        return isWhitePiece(piece) == position.whiteToMove
    }

    /** Whether the piece on [from] could geometrically move to [to] at all (ignoring check/pins) — used to
     *  tell a "wrong but legal" move (worth a quick preview-then-revert) from a flatly impossible one. */
    fun isPseudoLegalMove(from: Int, to: Int): Boolean {
        val piece = position.board[from] ?: return false
        return canReach(piece.uppercaseChar(), from, to, position)
    }

    /** The candidate UCI move for tapping [from] then [to], if it matches the next expected solution move. */
    fun expectedMoveMatches(from: Int, to: Int): UciMove? {
        if (solutionIndex >= solution.size) return null
        val piece = position.board[from] ?: return null
        val isPromotion = (piece == 'P' && rankOf(to) == 7) || (piece == 'p' && rankOf(to) == 0)
        val candidate = if (isPromotion) UciMove(from, to, 'q') else UciMove(from, to)
        return candidate.takeIf { it == solution[solutionIndex] }
    }

    /** Applies only the user's move (no auto opponent reply) — for staging a "your move, then a pause" UI. */
    fun applyUserMove(move: UciMove): PuzzleBoardState {
        val newPosition = applyUciMove(position, move)
        val newIndex = solutionIndex + 1
        val newStatus = if (newIndex >= solution.size) PuzzleStatus.SOLVED else PuzzleStatus.AWAITING_MOVE
        return copy(position = newPosition, solutionIndex = newIndex, selectedSquare = null, status = newStatus)
    }

    /** Applies the opponent's scripted reply (the next solution move) — call after [applyUserMove] once more remain. */
    fun applyAutoReply(): PuzzleBoardState {
        if (solutionIndex >= solution.size) return this
        val newPosition = applyUciMove(position, solution[solutionIndex])
        val newIndex = solutionIndex + 1
        val newStatus = if (newIndex >= solution.size) PuzzleStatus.SOLVED else PuzzleStatus.AWAITING_MOVE
        return copy(position = newPosition, solutionIndex = newIndex, status = newStatus)
    }

    companion object {
        fun fromFen(fen: String, solution: List<String>): PuzzleBoardState =
            PuzzleBoardState(
                position = FenParser.parse(fen),
                solution = solution.map { UciMove.parse(it) },
                solutionIndex = 0
            )
    }
}
