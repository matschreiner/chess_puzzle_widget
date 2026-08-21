package com.masc.chesspuzzlewidget.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PgnReplayTest {

    // Captured live from a real /api/puzzle/next response.
    private val realGamePgn =
        "d4 Nf6 Bg5 g6 Nd2 Bg7 e4 d6 Ngf3 O-O h3 h6 Bh4 g5 Bg3 Nbd7 Bc4 Nh5 Bh2 e5 " +
            "dxe5 Nxe5 Nxe5 dxe5 Qxh5 Kh7 Bxf7 Qf6 Bb3"
    private val realSolution = listOf("f6f2", "e1d1", "f2g2", "h1g1", "g2h2")

    @Test
    fun `replays a real captured game and lands on black to move with the queen on f6`() {
        val position = replayPgn(realGamePgn)

        assertEquals(false, position.whiteToMove)
        assertEquals('q', position.board[algebraicToSquare("f6")])
        assertEquals('K', position.board[algebraicToSquare("e1")])
    }

    @Test
    fun `the full puzzle solution solves cleanly from the replayed position`() {
        val position = replayPgn(realGamePgn)
        var state = PuzzleBoardState(
            position = position,
            solution = realSolution.map { UciMove.parse(it) },
            solutionIndex = 0
        )

        state = state.tapSquare(algebraicToSquare("f6")).tapSquare(algebraicToSquare("f2"))
        assertEquals(PuzzleStatus.AWAITING_MOVE, state.status)
        assertEquals('K', state.position.board[algebraicToSquare("d1")])

        state = state.tapSquare(algebraicToSquare("f2")).tapSquare(algebraicToSquare("g2"))
        assertEquals(PuzzleStatus.AWAITING_MOVE, state.status)

        state = state.tapSquare(algebraicToSquare("g2")).tapSquare(algebraicToSquare("h2"))
        assertEquals(PuzzleStatus.SOLVED, state.status)
        assertEquals('q', state.position.board[algebraicToSquare("h2")])
    }

    @Test
    fun `replays castling, captures, en passant-free normal play from the standard start`() {
        // 9.O-O castles the rook to f1, then 11.Re1 moves that same rook again.
        val position = replayPgn("e4 e5 Nf3 Nc6 Bb5 a6 Ba4 Nf6 O-O Be7 Re1 b5 Bb3 O-O")

        assertTrue(position.whiteToMove)
        assertEquals('K', position.board[algebraicToSquare("g1")])
        assertEquals('R', position.board[algebraicToSquare("e1")])
        assertEquals('k', position.board[algebraicToSquare("g8")])
        assertEquals('r', position.board[algebraicToSquare("f8")])
    }
}
