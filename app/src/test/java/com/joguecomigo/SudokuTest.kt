package com.joguecomigo

import com.joguecomigo.domain.Difficulty
import com.joguecomigo.domain.sudoku.SudokuBoard
import com.joguecomigo.domain.sudoku.SudokuGenerator
import com.joguecomigo.domain.sudoku.SudokuSolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Testes de validação do Sudoku. */
class SudokuTest {

    /** Verifica que uma solução gerada respeita todas as regras do Sudoku. */
    @Test
    fun fullSolutionIsValid() {
        val solution = SudokuSolver.generateFullSolution(Random(42))
        assertEquals(81, solution.size)
        assertTrue(isValidCompleteGrid(solution))
    }

    /** O puzzle gerado deve ter solução única e bater com a solução conhecida. */
    @Test
    fun generatedPuzzleHasUniqueSolution() {
        val board = SudokuGenerator.generate(Difficulty.FACIL, Random(7))
        assertEquals(1, SudokuSolver.countSolutions(board.puzzle, limit = 2))

        val toSolve = board.puzzle.copyOf()
        assertTrue(SudokuSolver.solve(toSolve))
        assertTrue(toSolve.contentEquals(board.solution))
    }

    /** Detecção de conflito segundo as regras (mesma linha). */
    @Test
    fun conflictDetectionWorks() {
        val board = SudokuGenerator.generate(Difficulty.MEDIO, Random(3))
        // Encontra duas células vazias na mesma linha e coloca valores iguais.
        val emptyIndex = (0 until 81).first { board.value(it) == 0 }
        val row = emptyIndex / 9
        val otherEmpty = (0 until 9)
            .map { row * 9 + it }
            .firstOrNull { it != emptyIndex && board.value(it) == 0 }

        if (otherEmpty != null) {
            board.setValue(emptyIndex, 5)
            board.setValue(otherEmpty, 5)
            assertTrue(board.hasConflict(emptyIndex) || board.hasConflict(otherEmpty))
        }
    }

    /** Validação de colocação respeitando linha, coluna e bloco. */
    @Test
    fun placementValidation() {
        val grid = IntArray(81)
        grid[0] = 5
        assertFalse(SudokuSolver.isValidPlacement(grid, 1, 5)) // mesma linha
        assertFalse(SudokuSolver.isValidPlacement(grid, 9, 5)) // mesma coluna
        assertFalse(SudokuSolver.isValidPlacement(grid, 10, 5)) // mesmo bloco
        // Posição livre: linha 4, coluna 4 (bloco central) não compartilha
        // linha, coluna nem bloco com a célula 0.
        assertTrue(SudokuSolver.isValidPlacement(grid, 40, 5))
    }

    private fun isValidCompleteGrid(grid: IntArray): Boolean {
        // Linhas, colunas e blocos devem conter 1..9 sem repetição.
        for (i in 0 until 9) {
            val rowSet = mutableSetOf<Int>()
            val colSet = mutableSetOf<Int>()
            for (j in 0 until 9) {
                rowSet.add(grid[i * 9 + j])
                colSet.add(grid[j * 9 + i])
            }
            if (rowSet != (1..9).toSet() || colSet != (1..9).toSet()) return false
        }
        for (boxRow in 0 until 3) {
            for (boxCol in 0 until 3) {
                val boxSet = mutableSetOf<Int>()
                for (r in 0 until 3) {
                    for (c in 0 until 3) {
                        boxSet.add(grid[(boxRow * 3 + r) * 9 + (boxCol * 3 + c)])
                    }
                }
                if (boxSet != (1..9).toSet()) return false
            }
        }
        return true
    }
}
