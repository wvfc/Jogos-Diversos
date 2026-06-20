package com.joguecomigo.domain.sudoku

import kotlin.random.Random

/**
 * Resolvedor e gerador de soluções completas de Sudoku.
 *
 * O tabuleiro é representado por um [IntArray] de 81 posições (9x9),
 * onde 0 representa célula vazia e 1..9 os valores preenchidos.
 */
object SudokuSolver {

    /** Verifica se [value] pode ser colocado em [index] sem violar as regras. */
    fun isValidPlacement(grid: IntArray, index: Int, value: Int): Boolean {
        val row = index / 9
        val col = index % 9

        // Linha e coluna.
        for (i in 0 until 9) {
            if (grid[row * 9 + i] == value) return false
            if (grid[i * 9 + col] == value) return false
        }

        // Bloco 3x3.
        val boxRow = (row / 3) * 3
        val boxCol = (col / 3) * 3
        for (r in boxRow until boxRow + 3) {
            for (c in boxCol until boxCol + 3) {
                if (grid[r * 9 + c] == value) return false
            }
        }
        return true
    }

    /**
     * Resolve o tabuleiro [grid] no lugar (backtracking).
     * @return true se houver solução.
     */
    fun solve(grid: IntArray): Boolean {
        val empty = grid.indexOfFirst { it == 0 }
        if (empty == -1) return true // Tabuleiro completo.

        for (value in 1..9) {
            if (isValidPlacement(grid, empty, value)) {
                grid[empty] = value
                if (solve(grid)) return true
                grid[empty] = 0
            }
        }
        return false
    }

    /**
     * Conta quantas soluções o tabuleiro possui, parando ao atingir [limit].
     * Usado para garantir que o puzzle tenha solução única.
     */
    fun countSolutions(grid: IntArray, limit: Int = 2): Int {
        val work = grid.copyOf()
        return countSolutionsInternal(work, limit)
    }

    private fun countSolutionsInternal(grid: IntArray, limit: Int): Int {
        val empty = grid.indexOfFirst { it == 0 }
        if (empty == -1) return 1

        var count = 0
        for (value in 1..9) {
            if (isValidPlacement(grid, empty, value)) {
                grid[empty] = value
                count += countSolutionsInternal(grid, limit)
                grid[empty] = 0
                if (count >= limit) return count
            }
        }
        return count
    }

    /**
     * Gera uma solução completa e válida de Sudoku usando backtracking
     * com ordem aleatória de valores, garantindo tabuleiros variados.
     */
    fun generateFullSolution(random: Random = Random.Default): IntArray {
        val grid = IntArray(81)
        fillGrid(grid, random)
        return grid
    }

    private fun fillGrid(grid: IntArray, random: Random): Boolean {
        val empty = grid.indexOfFirst { it == 0 }
        if (empty == -1) return true

        val values = (1..9).shuffled(random)
        for (value in values) {
            if (isValidPlacement(grid, empty, value)) {
                grid[empty] = value
                if (fillGrid(grid, random)) return true
                grid[empty] = 0
            }
        }
        return false
    }
}
