package com.joguecomigo.domain.tictactoe

import com.joguecomigo.domain.Difficulty
import kotlin.random.Random

/**
 * Motor de IA do Jogo da Velha usando Minimax.
 *
 * - Fácil: jogada aleatória.
 * - Médio: 50% aleatório, 50% Minimax.
 * - Difícil: 85% Minimax, 15% aleatório.
 * - Especialista: Minimax completo (imbatível).
 */
class TicTacToeEngine(private val random: Random = Random.Default) {

    fun bestMove(board: TicTacToeBoard, mark: TicTacToeMark, difficulty: Difficulty): Int? {
        val moves = board.availableMoves()
        if (moves.isEmpty()) return null
        return when (difficulty) {
            Difficulty.FACIL -> moves.random(random)
            Difficulty.MEDIO ->
                if (random.nextFloat() < 0.5f) moves.random(random) else minimaxMove(board, mark)
            Difficulty.DIFICIL ->
                if (random.nextFloat() < 0.85f) minimaxMove(board, mark) else moves.random(random)
            Difficulty.ESPECIALISTA -> minimaxMove(board, mark)
        }
    }

    private fun minimaxMove(board: TicTacToeBoard, mark: TicTacToeMark): Int {
        var bestScore = Int.MIN_VALUE
        var bestIndex = board.availableMoves().first()
        for (index in board.availableMoves()) {
            val score = minimax(board.play(index, mark), mark.opponent(), mark, depth = 1)
            if (score > bestScore) { bestScore = score; bestIndex = index }
        }
        return bestIndex
    }

    private fun minimax(
        board: TicTacToeBoard,
        current: TicTacToeMark,
        ai: TicTacToeMark,
        depth: Int
    ): Int {
        when (board.result()) {
            TicTacToeResult.X_WINS ->
                return if (ai == TicTacToeMark.X) 10 - depth else depth - 10
            TicTacToeResult.O_WINS ->
                return if (ai == TicTacToeMark.O) 10 - depth else depth - 10
            TicTacToeResult.DRAW -> return 0
            TicTacToeResult.IN_PROGRESS -> {}
        }
        val scores = board.availableMoves().map { i ->
            minimax(board.play(i, current), current.opponent(), ai, depth + 1)
        }
        return if (current == ai) scores.max() else scores.min()
    }
}
