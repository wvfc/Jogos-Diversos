package com.joguecomigo.data

import com.joguecomigo.domain.Difficulty
import kotlin.math.max

/**
 * Calcula o XP ganho em cada partida com base no desempenho.
 */
object XpCalculator {

    /** XP de uma partida de Sudoku. */
    fun sudokuXp(
        solved: Boolean,
        difficulty: Difficulty,
        durationSeconds: Long,
        errorCount: Int,
        hintsUsed: Int,
    ): Int {
        val base = if (solved) when (difficulty) {
            Difficulty.FACIL       -> 30
            Difficulty.MEDIO       -> 50
            Difficulty.DIFICIL     -> 75
            Difficulty.ESPECIALISTA -> 100
        } else 5

        val penalty = (errorCount * 3) + (hintsUsed * 5)
        val speedBonus = if (solved) when {
            durationSeconds < 120 -> 20
            durationSeconds < 300 -> 10
            else -> 0
        } else 0

        return max(1, base + speedBonus - penalty)
    }

    /** XP de uma partida de Dama. */
    fun checkersXp(
        outcome: MatchOutcome,
        difficulty: Difficulty,
        durationSeconds: Long,
    ): Int {
        val base = when (outcome) {
            MatchOutcome.WIN  -> when (difficulty) {
                Difficulty.FACIL       -> 25
                Difficulty.MEDIO       -> 40
                Difficulty.DIFICIL     -> 60
                Difficulty.ESPECIALISTA -> 85
            }
            MatchOutcome.DRAW -> 15
            MatchOutcome.LOSS -> 5
        }
        val speedBonus = when {
            durationSeconds < 60  -> 10
            durationSeconds < 180 -> 5
            else -> 0
        }
        return max(1, base + speedBonus)
    }

    /** XP de uma partida de Jogo da Velha. */
    fun ticTacToeXp(
        outcome: MatchOutcome,
        difficulty: Difficulty,
        durationSeconds: Long,
    ): Int {
        val base = when (outcome) {
            MatchOutcome.WIN  -> 35
            MatchOutcome.DRAW -> 18
            MatchOutcome.LOSS -> 6
        }
        val diffBonus = when (difficulty) {
            Difficulty.FACIL       -> 1.0
            Difficulty.MEDIO       -> 1.25
            Difficulty.DIFICIL     -> 1.5
            Difficulty.ESPECIALISTA -> 1.8
        }
        val speedBonus = when {
            durationSeconds <= 20 -> 10
            durationSeconds <= 45 -> 5
            else -> 0
        }
        return max(1, ((base + speedBonus) * diffBonus).toInt())
    }
}
