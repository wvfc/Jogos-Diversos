package com.joguecomigo

import com.joguecomigo.domain.Difficulty
import com.joguecomigo.domain.checkers.Cell
import com.joguecomigo.domain.checkers.CheckersBoard
import com.joguecomigo.domain.checkers.CheckersEngine
import com.joguecomigo.domain.checkers.Side
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Testes de validação da Dama. */
class CheckersTest {

    /** O tabuleiro inicial deve ter 12 peças de cada lado. */
    @Test
    fun initialBoardHasTwelveEach() {
        val board = CheckersBoard.initial()
        assertEquals(12, board.pieceCount(Side.WHITE))
        assertEquals(12, board.pieceCount(Side.BLACK))
    }

    /** Na posição inicial, as Brancas têm exatamente 7 movimentos simples. */
    @Test
    fun initialWhiteHasSevenSimpleMoves() {
        val board = CheckersBoard.initial()
        val moves = board.legalMoves(Side.WHITE)
        assertEquals(7, moves.size)
        assertTrue(moves.none { it.isCapture })
    }

    /** Capturas são obrigatórias: quando existe captura, só capturas são legais. */
    @Test
    fun captureIsMandatory() {
        val squares = IntArray(64) { Cell.EMPTY }
        // Peça branca em (5,2) e peça preta adjacente em (4,3); destino (3,4) livre.
        val whiteIndex = 5 * 8 + 2
        val blackIndex = 4 * 8 + 3
        squares[whiteIndex] = Cell.WHITE_MAN
        squares[blackIndex] = Cell.BLACK_MAN
        val board = CheckersBoard.from(squares)

        val moves = board.legalMoves(Side.WHITE)
        assertTrue(moves.isNotEmpty())
        assertTrue(moves.all { it.isCapture })
        val capture = moves.first()
        assertEquals(blackIndex, capture.captured.first())
        assertEquals(3 * 8 + 4, capture.to)
    }

    /** Após capturar, a peça inimiga é removida do tabuleiro. */
    @Test
    fun applyingCaptureRemovesPiece() {
        val squares = IntArray(64) { Cell.EMPTY }
        val whiteIndex = 5 * 8 + 2
        val blackIndex = 4 * 8 + 3
        squares[whiteIndex] = Cell.WHITE_MAN
        squares[blackIndex] = Cell.BLACK_MAN
        val board = CheckersBoard.from(squares)

        val capture = board.legalMoves(Side.WHITE).first { it.isCapture }
        val next = board.applyMove(capture)
        assertEquals(0, next.pieceCount(Side.BLACK))
        assertEquals(1, next.pieceCount(Side.WHITE))
    }

    /** Um peão branco que chega à última linha vira dama. */
    @Test
    fun manPromotesToKing() {
        val squares = IntArray(64) { Cell.EMPTY }
        // Peça branca em (1,2) que avança para (0,1) ou (0,3) — última linha.
        squares[1 * 8 + 2] = Cell.WHITE_MAN
        val board = CheckersBoard.from(squares)
        val move = board.legalMoves(Side.WHITE).first()
        val next = board.applyMove(move)
        assertTrue(Cell.isKing(next.pieceAt(move.to)))
    }

    /** O motor sempre retorna um lance legal a partir da posição inicial. */
    @Test
    fun engineReturnsLegalMove() {
        val board = CheckersBoard.initial()
        val engine = CheckersEngine()
        val move = engine.bestMove(board, Side.WHITE, Difficulty.ESPECIALISTA)
        assertNotNull(move)
        assertTrue(board.legalMoves(Side.WHITE).any { it.from == move!!.from && it.to == move.to })
    }

    /** Sem peças, o lado não tem movimentos (condição de derrota). */
    @Test
    fun noPiecesMeansNoMoves() {
        val board = CheckersBoard.from(IntArray(64) { Cell.EMPTY })
        assertTrue(board.legalMoves(Side.WHITE).isEmpty())
        assertFalse(board.legalMoves(Side.BLACK).isNotEmpty())
    }
}
