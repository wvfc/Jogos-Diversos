package com.joguecomigo.data

import com.joguecomigo.domain.Difficulty
import kotlin.math.max

/**
 * Calcula o XP ganho em cada partida com base no desempenho.
 *
 * Critérios considerados: resultado, dificuldade, tempo, erros e dicas usadas.
 * Um desempenho fraco (muitos erros/dicas, derrota) gera pouco XP, o que faz
 * o jogador não avançar de nível — exatamente como pede o sistema dinâmico.
 */
object XpCalculator {

    /**
     * XP de uma partida de Sudoku.
     *
     * @param solved Se o tabuleiro foi resolvido corretamente (vitória).
     * @param difficulty Dificuldade jogada.
     * @param durationSeconds Tempo total.
     * @param mistakes Erros cometidos.
     * @param hintsUsed Dicas usadas.
     */
    fun sudokuXp(
        solved: Boolean,
        difficulty: Difficulty,
        durationSeconds: Long,
        mistakes: Int,
        hintsUsed: Int,
    ): Int {
        val diffMultiplier = difficulty.order + 1 // 1..4
        val base = if (solved) 60 else 8
        var xp = base * diffMultiplier

        if (solved) {
            // Bônus por rapidez: até +30 se resolver em menos de 5 minutos.
            val minutes = durationSeconds / 60.0
            xp += max(0, (30 - (minutes * 3).toInt()))
        }

        // Penalidades por erros e dicas.
        xp -= mistakes * 5
        xp -= hintsUsed * 4

        // Garante um mínimo simbólico.
        return max(if (solved) 15 else 0, xp)
    }

    /**
     * XP de uma partida de Dama.
     *
     * @param outcome Resultado da partida.
     * @param difficulty Dificuldade jogada.
     * @param durationSeconds Tempo total.
     * @param piecesLost Peças próprias perdidas.
     * @param hintsUsed Sugestões/dicas usadas.
     */
    fun checkersXp(
        outcome: MatchOutcome,
        difficulty: Difficulty,
        durationSeconds: Long,
        piecesLost: Int,
        hintsUsed: Int,
    ): Int {
        val diffMultiplier = difficulty.order + 1
        val base = when (outcome) {
            MatchOutcome.WIN -> 70
            MatchOutcome.DRAW -> 25
            MatchOutcome.LOSS -> 8
        }
        var xp = base * diffMultiplier

        if (outcome == MatchOutcome.WIN) {
            // Recompensa vencer perdendo poucas peças.
            xp += max(0, (12 - piecesLost) * 3)
            // Bônus por decisões rápidas: vencer em menos de 8 minutos.
            val minutes = durationSeconds / 60.0
            xp += max(0, (16 - (minutes * 2).toInt()))
        }
        xp -= hintsUsed * 4

        return max(if (outcome == MatchOutcome.WIN) 20 else 0, xp)
    }
}
