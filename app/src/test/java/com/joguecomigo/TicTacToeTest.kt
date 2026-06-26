package com.joguecomigo

import com.joguecomigo.domain.Difficulty
import com.joguecomigo.domain.tictactoe.TicTacToeBoard
import com.joguecomigo.domain.tictactoe.TicTacToeEngine
import com.joguecomigo.domain.tictactoe.TicTacToeMark
import com.joguecomigo.domain.tictactoe.TicTacToeResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Testes unitários do Jogo da Velha. */
class TicTacToeTest {

    /** Tabuleiro inicial deve ter 9 células vazias. */
    @Test
    fun initialBoardIsEmpty() {
        val board = TicTacToeBoard()
        assertTrue(board.cells.all { it == TicTacToeMark.EMPTY })
        assertEquals(9, board.availableMoves().size)
    }

    /** Resultado inicial deve ser IN_PROGRESS. */
    @Test
    fun initialResultIsInProgress() {
        assertEquals(TicTacToeResult.IN_PROGRESS, TicTacToeBoard().result())
    }

    /** X vence na linha horizontal superior. */
    @Test
    fun xWinsTopRow() {
        var board = TicTacToeBoard()
        board = board.play(0, TicTacToeMark.X)
        board = board.play(3, TicTacToeMark.O)
        board = board.play(1, TicTacToeMark.X)
        board = board.play(4, TicTacToeMark.O)
        board = board.play(2, TicTacToeMark.X)
        assertEquals(TicTacToeResult.X_WINS, board.result())
    }

    /** O vence na coluna da esquerda. */
    @Test
    fun oWinsLeftColumn() {
        var board = TicTacToeBoard()
        board = board.play(1, TicTacToeMark.X)
        board = board.play(0, TicTacToeMark.O)
        board = board.play(2, TicTacToeMark.X)
        board = board.play(3, TicTacToeMark.O)
        board = board.play(4, TicTacToeMark.X)
        board = board.play(6, TicTacToeMark.O)
        assertEquals(TicTacToeResult.O_WINS, board.result())
    }

    /** Empate quando o tabuleiro enche sem vencedor. */
    @Test
    fun drawWhenBoardFull() {
        // X O X
        // X X O
        // O X O  -> empate
        val moves = listOf(
            0 to TicTacToeMark.X,
            1 to TicTacToeMark.O,
            2 to TicTacToeMark.X,
            3 to TicTacToeMark.X,
            5 to TicTacToeMark.O,
            4 to TicTacToeMark.X,
            6 to TicTacToeMark.O,
            7 to TicTacToeMark.X,
            8 to TicTacToeMark.O,
        )
        var board = TicTacToeBoard()
        for ((index, mark) in moves) board = board.play(index, mark)
        assertEquals(TicTacToeResult.DRAW, board.result())
    }

    /** Não deve permitir jogar em célula já ocupada. */
    @Test
    fun cannotPlayOnOccupiedCell() {
        var board = TicTacToeBoard()
        board = board.play(4, TicTacToeMark.X)
        assertFalse(board.canPlay(4))
        val boardAfter = board.play(4, TicTacToeMark.O)
        // Tabuleiro não muda
        assertEquals(TicTacToeMark.X, boardAfter.markAt(4))
    }

    /** Motor Especialista nunca perde contra X humano aleatório. */
    @Test
    fun expertEngineNeverLoses() {
        val engine = TicTacToeEngine()
        repeat(50) {
            var board = TicTacToeBoard()
            var turn = TicTacToeMark.X // humano = X, IA = O
            while (board.result() == TicTacToeResult.IN_PROGRESS) {
                val index = if (turn == TicTacToeMark.O) {
                    engine.bestMove(board, TicTacToeMark.O, Difficulty.ESPECIALISTA)!!
                } else {
                    board.availableMoves().random()
                }
                board = board.play(index, turn)
                turn = turn.opponent()
            }
            assertFalse(
                "IA Especialista não deveria perder",
                board.result() == TicTacToeResult.X_WINS
            )
        }
    }

    /** Motor Fácil sempre retorna um movimento válido. */
    @Test
    fun easyEngineReturnsValidMove() {
        val engine = TicTacToeEngine()
        val board = TicTacToeBoard()
        val move = engine.bestMove(board, TicTacToeMark.O, Difficulty.FACIL)
        assertNotNull(move)
        assertTrue(board.canPlay(move!!))
    }

    /** Motor não retorna movimento quando o jogo acabou. */
    @Test
    fun engineReturnsNullWhenGameOver() {
        val engine = TicTacToeEngine()
        var board = TicTacToeBoard()
        board = board.play(0, TicTacToeMark.X)
        board = board.play(3, TicTacToeMark.O)
        board = board.play(1, TicTacToeMark.X)
        board = board.play(4, TicTacToeMark.O)
        board = board.play(2, TicTacToeMark.X) // X venceu
        val move = engine.bestMove(board, TicTacToeMark.O, Difficulty.ESPECIALISTA)
        assertEquals(null, move)
    }
}
