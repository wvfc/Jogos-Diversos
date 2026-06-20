package com.joguecomigo.domain.sudoku

import com.joguecomigo.domain.Difficulty
import kotlin.random.Random

/**
 * Gera tabuleiros de Sudoku válidos com solução única para cada dificuldade.
 *
 * A quantidade de pistas reveladas diminui conforme a dificuldade aumenta.
 */
object SudokuGenerator {

    /** Número aproximado de pistas mantidas por dificuldade. */
    private fun cluesFor(difficulty: Difficulty): Int = when (difficulty) {
        Difficulty.FACIL -> 45
        Difficulty.MEDIO -> 36
        Difficulty.DIFICIL -> 30
        Difficulty.ESPECIALISTA -> 26
    }

    /**
     * Gera um novo [SudokuBoard] para a [difficulty] informada.
     * Garante solução única removendo células de forma controlada.
     */
    fun generate(difficulty: Difficulty, random: Random = Random.Default): SudokuBoard {
        val solution = SudokuSolver.generateFullSolution(random)
        val puzzle = solution.copyOf()

        val targetClues = cluesFor(difficulty)
        val indices = (0 until 81).shuffled(random)

        var remaining = 81
        for (index in indices) {
            if (remaining <= targetClues) break

            val backup = puzzle[index]
            if (backup == 0) continue

            puzzle[index] = 0
            // Mantém a remoção apenas se a solução continuar única.
            if (SudokuSolver.countSolutions(puzzle, limit = 2) != 1) {
                puzzle[index] = backup
            } else {
                remaining--
            }
        }

        return SudokuBoard(solution = solution, puzzle = puzzle)
    }
}
