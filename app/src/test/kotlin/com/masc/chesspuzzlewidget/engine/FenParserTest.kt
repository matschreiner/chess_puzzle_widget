package com.masc.chesspuzzlewidget.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FenParserTest {

    @Test
    fun `round trips the starting position`() {
        val fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        val position = FenParser.parse(fen)
        assertEquals(fen, FenParser.toFen(position))
    }

    @Test
    fun `round trips a puzzle-style midgame position`() {
        val fen = "5k2/1p2r3/p2p1n2/3P1Q1p/4pPrP/3q2P1/2R4K/8 w - - 0 1"
        val position = FenParser.parse(fen)
        assertEquals(fen, FenParser.toFen(position))
    }

    @Test
    fun `round trips a position with an en passant target and no castling rights`() {
        val fen = "rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR w - d6 0 3"
        val position = FenParser.parse(fen)
        assertEquals(fen, FenParser.toFen(position))
    }

    @Test
    fun `parses piece placement of the starting position correctly`() {
        val position = FenParser.parse("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
        assertEquals('K', position.board[algebraicToSquare("e1")])
        assertEquals('k', position.board[algebraicToSquare("e8")])
        assertEquals('R', position.board[algebraicToSquare("a1")])
        assertEquals('P', position.board[algebraicToSquare("e2")])
        assertNull(position.board[algebraicToSquare("e4")])
        assertEquals(setOf('K', 'Q', 'k', 'q'), position.castlingRights)
        assertNull(position.enPassantSquare)
        assertEquals(true, position.whiteToMove)
    }

    @Test
    fun `parses side to move, en passant square and move counters`() {
        val position = FenParser.parse("rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR w - d6 0 3")
        assertEquals(true, position.whiteToMove)
        assertEquals(algebraicToSquare("d6"), position.enPassantSquare)
        assertEquals(emptySet<Char>(), position.castlingRights)
        assertEquals(0, position.halfmoveClock)
        assertEquals(3, position.fullmoveNumber)
    }
}
