package com.masc.chesspuzzlewidget.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UciMoveApplierTest {

    @Test
    fun `applies a normal pawn push and sets the en passant target`() {
        val start = FenParser.parse("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
        val result = applyUciMove(start, UciMove.parse("e2e4"))

        assertNull(result.board[algebraicToSquare("e2")])
        assertEquals('P', result.board[algebraicToSquare("e4")])
        assertEquals(algebraicToSquare("e3"), result.enPassantSquare)
        assertEquals(false, result.whiteToMove)
        assertEquals(0, result.halfmoveClock)
        assertEquals(1, result.fullmoveNumber)
    }

    @Test
    fun `applies a capture and resets the halfmove clock`() {
        val start = FenParser.parse("4k3/8/8/8/3p4/4P3/8/4K3 w - - 7 20")
        val result = applyUciMove(start, UciMove.parse("e3d4"))

        assertNull(result.board[algebraicToSquare("e3")])
        assertEquals('P', result.board[algebraicToSquare("d4")])
        assertEquals(0, result.halfmoveClock)
        assertEquals(20, result.fullmoveNumber)
    }

    @Test
    fun `kingside and queenside castling also relocate the rook and revoke rights`() {
        val start = FenParser.parse("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1")

        val afterWhiteCastles = applyUciMove(start, UciMove.parse("e1g1"))
        assertEquals('K', afterWhiteCastles.board[algebraicToSquare("g1")])
        assertEquals('R', afterWhiteCastles.board[algebraicToSquare("f1")])
        assertNull(afterWhiteCastles.board[algebraicToSquare("e1")])
        assertNull(afterWhiteCastles.board[algebraicToSquare("h1")])
        assertEquals(setOf('k', 'q'), afterWhiteCastles.castlingRights)

        val afterBlackCastles = applyUciMove(afterWhiteCastles, UciMove.parse("e8c8"))
        assertEquals('k', afterBlackCastles.board[algebraicToSquare("c8")])
        assertEquals('r', afterBlackCastles.board[algebraicToSquare("d8")])
        assertNull(afterBlackCastles.board[algebraicToSquare("e8")])
        assertNull(afterBlackCastles.board[algebraicToSquare("a8")])
        assertTrue(afterBlackCastles.castlingRights.isEmpty())
    }

    @Test
    fun `en passant capture removes the pawn beside, not on, the destination square`() {
        val start = FenParser.parse("8/8/8/3pP3/8/8/8/4K2k w - d6 0 1")
        val result = applyUciMove(start, UciMove.parse("e5d6"))

        assertEquals('P', result.board[algebraicToSquare("d6")])
        assertNull(result.board[algebraicToSquare("e5")])
        assertNull(result.board[algebraicToSquare("d5")])
    }

    @Test
    fun `promotion replaces the pawn with the requested piece`() {
        val start = FenParser.parse("8/P3k3/8/8/8/8/8/4K3 w - - 0 1")
        val result = applyUciMove(start, UciMove.parse("a7a8q"))

        assertEquals('Q', result.board[algebraicToSquare("a8")])
        assertNull(result.board[algebraicToSquare("a7")])
    }

    @Test
    fun `black pawn promotion uses lowercase piece letters`() {
        val start = FenParser.parse("4k3/8/8/8/8/8/p7/4K3 b - - 0 1")
        val result = applyUciMove(start, UciMove.parse("a2a1q"))

        assertEquals('q', result.board[algebraicToSquare("a1")])
        assertNull(result.board[algebraicToSquare("a2")])
    }
}
