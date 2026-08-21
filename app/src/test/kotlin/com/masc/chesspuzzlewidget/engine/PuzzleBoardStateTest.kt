package com.masc.chesspuzzlewidget.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PuzzleBoardStateTest {

    @Test
    fun `selecting an empty square is a no-op`() {
        val state = PuzzleBoardState.fromFen(
            "4k3/8/8/8/8/8/4P3/4K3 w - - 0 1",
            listOf("e2e4", "e8e7")
        )
        val after = state.tapSquare(algebraicToSquare("e3"))
        assertNull(after.selectedSquare)
        assertEquals(state.position, after.position)
    }

    @Test
    fun `selecting the opponent's piece is a no-op`() {
        val state = PuzzleBoardState.fromFen(
            "4k3/8/8/8/8/8/4P3/4K3 w - - 0 1",
            listOf("e2e4", "e8e7")
        )
        val after = state.tapSquare(algebraicToSquare("e8"))
        assertNull(after.selectedSquare)
    }

    @Test
    fun `tapping the selected square again deselects it`() {
        val state = PuzzleBoardState.fromFen(
            "4k3/8/8/8/8/8/4P3/4K3 w - - 0 1",
            listOf("e2e4", "e8e7")
        )
        val selected = state.tapSquare(algebraicToSquare("e2"))
        assertEquals(algebraicToSquare("e2"), selected.selectedSquare)

        val deselected = selected.tapSquare(algebraicToSquare("e2"))
        assertNull(deselected.selectedSquare)
    }

    @Test
    fun `a correct move applies the auto opponent reply and advances the index by two`() {
        val state = PuzzleBoardState.fromFen(
            "4k3/8/8/8/8/8/4P3/4K3 w - - 0 1",
            listOf("e2e4", "e8e7")
        )
        val afterSelect = state.tapSquare(algebraicToSquare("e2"))
        val afterMove = afterSelect.tapSquare(algebraicToSquare("e4"))

        assertNull(afterMove.selectedSquare)
        assertEquals(2, afterMove.solutionIndex)
        assertEquals(PuzzleStatus.SOLVED, afterMove.status)
        assertEquals('P', afterMove.position.board[algebraicToSquare("e4")])
        assertEquals('k', afterMove.position.board[algebraicToSquare("e7")])
        assertNull(afterMove.position.board[algebraicToSquare("e8")])
    }

    @Test
    fun `an incorrect move deselects without mutating the board`() {
        val state = PuzzleBoardState.fromFen(
            "4k3/8/8/8/8/8/4P3/4K3 w - - 0 1",
            listOf("e2e4", "e8e7")
        )
        val afterSelect = state.tapSquare(algebraicToSquare("e2"))
        val afterWrongMove = afterSelect.tapSquare(algebraicToSquare("e3"))

        assertNull(afterWrongMove.selectedSquare)
        assertEquals(0, afterWrongMove.solutionIndex)
        assertEquals(PuzzleStatus.AWAITING_MOVE, afterWrongMove.status)
        assertEquals(state.position, afterWrongMove.position)
    }

    @Test
    fun `a single-move solution is solved with no opponent reply`() {
        val state = PuzzleBoardState.fromFen(
            "6k1/5ppp/8/8/8/8/8/R6K w - - 0 1",
            listOf("a1a8")
        )
        val afterSelect = state.tapSquare(algebraicToSquare("a1"))
        val afterMove = afterSelect.tapSquare(algebraicToSquare("a8"))

        assertEquals(1, afterMove.solutionIndex)
        assertEquals(PuzzleStatus.SOLVED, afterMove.status)
        assertEquals('R', afterMove.position.board[algebraicToSquare("a8")])
    }

    @Test
    fun `promotion moves are assumed to be to a queen`() {
        val state = PuzzleBoardState.fromFen(
            "8/P3k3/8/8/8/8/8/4K3 w - - 0 1",
            listOf("a7a8q")
        )
        val afterSelect = state.tapSquare(algebraicToSquare("a7"))
        val afterMove = afterSelect.tapSquare(algebraicToSquare("a8"))

        assertEquals(PuzzleStatus.SOLVED, afterMove.status)
        assertEquals('Q', afterMove.position.board[algebraicToSquare("a8")])
    }

    @Test
    fun `expectedMoveMatches returns the move only when it matches the solution`() {
        val state = PuzzleBoardState.fromFen(
            "4k3/8/8/8/8/8/4P3/4K3 w - - 0 1",
            listOf("e2e4", "e8e7")
        )
        assertEquals(UciMove.parse("e2e4"), state.expectedMoveMatches(algebraicToSquare("e2"), algebraicToSquare("e4")))
        assertNull(state.expectedMoveMatches(algebraicToSquare("e2"), algebraicToSquare("e3")))
    }

    @Test
    fun `applyUserMove advances the index by one without auto-playing the reply`() {
        val state = PuzzleBoardState.fromFen(
            "4k3/8/8/8/8/8/4P3/4K3 w - - 0 1",
            listOf("e2e4", "e8e7")
        )
        val afterUserMove = state.applyUserMove(UciMove.parse("e2e4"))

        assertEquals(1, afterUserMove.solutionIndex)
        assertEquals(PuzzleStatus.AWAITING_MOVE, afterUserMove.status)
        assertEquals('P', afterUserMove.position.board[algebraicToSquare("e4")])
        assertEquals('k', afterUserMove.position.board[algebraicToSquare("e8")])

        val afterReply = afterUserMove.applyAutoReply()
        assertEquals(2, afterReply.solutionIndex)
        assertEquals(PuzzleStatus.SOLVED, afterReply.status)
        assertEquals('k', afterReply.position.board[algebraicToSquare("e7")])
    }

    @Test
    fun `isOwnPieceAt reflects side to move`() {
        val state = PuzzleBoardState.fromFen(
            "4k3/8/8/8/8/8/4P3/4K3 w - - 0 1",
            listOf("e2e4")
        )
        assertEquals(true, state.isOwnPieceAt(algebraicToSquare("e2")))
        assertEquals(false, state.isOwnPieceAt(algebraicToSquare("e8")))
        assertEquals(false, state.isOwnPieceAt(algebraicToSquare("e4")))
    }

    @Test
    fun `isPseudoLegalMove distinguishes a legal-but-wrong move from a geometrically impossible one`() {
        val state = PuzzleBoardState.fromFen(
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            emptyList()
        )
        // Knight b1 can legally hop to c3, but not to b3 (not an L-shape) or a3 (blocked isn't the issue, shape is).
        assertEquals(true, state.isPseudoLegalMove(algebraicToSquare("b1"), algebraicToSquare("c3")))
        assertEquals(false, state.isPseudoLegalMove(algebraicToSquare("b1"), algebraicToSquare("b3")))
        // Rook a1 is blocked by its own pawn on a2.
        assertEquals(false, state.isPseudoLegalMove(algebraicToSquare("a1"), algebraicToSquare("a3")))
        // Pawn e2 can't leap to e5.
        assertEquals(false, state.isPseudoLegalMove(algebraicToSquare("e2"), algebraicToSquare("e5")))
    }

    @Test
    fun `tapping is a no-op once the puzzle is solved`() {
        val state = PuzzleBoardState.fromFen(
            "6k1/5ppp/8/8/8/8/8/R6K w - - 0 1",
            listOf("a1a8")
        )
        val solved = state.tapSquare(algebraicToSquare("a1")).tapSquare(algebraicToSquare("a8"))
        val afterExtraTap = solved.tapSquare(algebraicToSquare("h1"))

        assertEquals(solved, afterExtraTap)
    }
}
