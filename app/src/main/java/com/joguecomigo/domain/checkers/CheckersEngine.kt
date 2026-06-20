package com.joguecomigo.domain.checkers

import com.joguecomigo.domain.Difficulty
import kotlin.random.Random

/**
 * Motor (IA local) do jogo de Dama.
 *
 * Usa o algoritmo Minimax com poda alfa-beta. A profundidade de busca e a
 * função de avaliação variam conforme a dificuldade:
 * - Fácil: jogadas válidas aleatórias (sem busca).
 * - Médio: Minimax de baixa profundidade.
 * - Difícil: Minimax mais profundo com avaliação melhorada.
 * - Especialista: Minimax profundo com poda alfa-beta e melhor avaliação.
 */
class CheckersEngine(private val random: Random = Random.Default) {

    private fun depthFor(difficulty: Difficulty): Int = when (difficulty) {
        Difficulty.FACIL -> 0          // joga aleatório
        Difficulty.MEDIO -> 3
        Difficulty.DIFICIL -> 5
        Difficulty.ESPECIALISTA -> 7
    }

    /**
     * Escolhe o melhor lance para [side] no [board], de acordo com a [difficulty].
     * Retorna null se não houver lances (derrota).
     */
    fun bestMove(board: CheckersBoard, side: Side, difficulty: Difficulty): CheckersMove? {
        val moves = board.legalMoves(side)
        if (moves.isEmpty()) return null
        if (moves.size == 1) return moves.first()

        val depth = depthFor(difficulty)
        if (depth == 0) {
            // Fácil: prefere capturas (são obrigatórias) e escolhe ao acaso.
            return moves.random(random)
        }

        var bestMove: CheckersMove? = null
        var bestScore = Int.MIN_VALUE
        // Ordena para começar por capturas, melhorando a poda alfa-beta.
        for (move in moves.sortedByDescending { it.captured.size }) {
            val next = board.applyMove(move)
            val score = minimax(
                board = next,
                toMove = side.opponent(),
                maximizing = side,
                depth = depth - 1,
                alpha = Int.MIN_VALUE,
                beta = Int.MAX_VALUE,
                useAlphaBeta = difficulty != Difficulty.MEDIO,
                difficulty = difficulty,
            )
            if (score > bestScore || (score == bestScore && random.nextBoolean())) {
                bestScore = score
                bestMove = move
            }
        }
        return bestMove ?: moves.first()
    }

    /**
     * Minimax recursivo. [maximizing] é o lado para o qual estamos otimizando;
     * [toMove] é quem joga no nó atual.
     */
    private fun minimax(
        board: CheckersBoard,
        toMove: Side,
        maximizing: Side,
        depth: Int,
        alpha: Int,
        beta: Int,
        useAlphaBeta: Boolean,
        difficulty: Difficulty,
    ): Int {
        val moves = board.legalMoves(toMove)

        // Estado terminal: lado da vez sem movimentos perde.
        if (moves.isEmpty()) {
            return if (toMove == maximizing) -WIN_SCORE else WIN_SCORE
        }
        if (depth == 0) {
            return evaluate(board, maximizing, difficulty)
        }

        var a = alpha
        var b = beta

        if (toMove == maximizing) {
            var best = Int.MIN_VALUE
            for (move in moves.sortedByDescending { it.captured.size }) {
                val score = minimax(
                    board.applyMove(move), toMove.opponent(), maximizing,
                    depth - 1, a, b, useAlphaBeta, difficulty
                )
                if (score > best) best = score
                if (useAlphaBeta) {
                    if (best > a) a = best
                    if (b <= a) break // poda
                }
            }
            return best
        } else {
            var best = Int.MAX_VALUE
            for (move in moves.sortedByDescending { it.captured.size }) {
                val score = minimax(
                    board.applyMove(move), toMove.opponent(), maximizing,
                    depth - 1, a, b, useAlphaBeta, difficulty
                )
                if (score < best) best = score
                if (useAlphaBeta) {
                    if (best < b) b = best
                    if (b <= a) break // poda
                }
            }
            return best
        }
    }

    /**
     * Função de avaliação heurística (sob a ótica de [maximizing]).
     * No nível Difícil/Especialista considera também posição (avanço e centro).
     */
    fun evaluate(board: CheckersBoard, maximizing: Side, difficulty: Difficulty): Int {
        var score = 0
        val advanced = difficulty == Difficulty.DIFICIL || difficulty == Difficulty.ESPECIALISTA

        for (index in 0 until 64) {
            val piece = board.pieceAt(index)
            if (piece == Cell.EMPTY) continue

            val pieceSide = if (Cell.isWhite(piece)) Side.WHITE else Side.BLACK
            val sign = if (pieceSide == maximizing) 1 else -1

            // Valor material base.
            var value = if (Cell.isKing(piece)) KING_VALUE else MAN_VALUE

            if (advanced) {
                val row = index / 8
                val col = index % 8
                // Incentiva avanço dos peões rumo à promoção.
                if (!Cell.isKing(piece)) {
                    val advance = if (pieceSide == Side.WHITE) (7 - row) else row
                    value += advance
                }
                // Pequeno bônus para controle do centro.
                if (col in 2..5) value += 1
            }

            score += sign * value
        }
        return score
    }

    companion object {
        private const val MAN_VALUE = 10
        private const val KING_VALUE = 18
        private const val WIN_SCORE = 100_000
    }
}
