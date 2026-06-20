package com.joguecomigo.domain.sudoku

/**
 * Representa o estado de uma partida de Sudoku.
 *
 * Mantém o puzzle original (pistas), a solução correta e o estado atual
 * preenchido pelo jogador.
 *
 * @param solution Solução completa (81 valores de 1..9).
 * @param puzzle Tabuleiro inicial com pistas (0 = célula vazia para o jogador).
 */
class SudokuBoard(
    val solution: IntArray,
    val puzzle: IntArray,
) {
    /** Estado atual do tabuleiro, inicia igual ao puzzle. */
    val cells: IntArray = puzzle.copyOf()

    /** Marca quais células são pistas fixas (não editáveis). */
    private val given: BooleanArray = BooleanArray(81) { puzzle[it] != 0 }

    fun isGiven(index: Int): Boolean = given[index]

    fun value(index: Int): Int = cells[index]

    /** Define o valor de uma célula editável. Retorna false para pistas fixas. */
    fun setValue(index: Int, value: Int): Boolean {
        if (given[index]) return false
        cells[index] = value
        return true
    }

    /** Limpa uma célula editável. */
    fun clear(index: Int): Boolean {
        if (given[index]) return false
        cells[index] = 0
        return true
    }

    /** Verifica se a célula contém o valor correto segundo a solução. */
    fun isCorrect(index: Int): Boolean = cells[index] == solution[index]

    /** Indica se todas as células estão preenchidas. */
    fun isComplete(): Boolean = cells.all { it != 0 }

    /** Indica se o tabuleiro está completamente resolvido e correto. */
    fun isSolved(): Boolean = cells.indices.all { cells[it] == solution[it] }

    /**
     * Verifica se o valor atual em [index] conflita com as regras do Sudoku
     * (mesma linha, coluna ou bloco). Não usa a solução, apenas as regras.
     */
    fun hasConflict(index: Int): Boolean {
        val value = cells[index]
        if (value == 0) return false

        val row = index / 9
        val col = index % 9

        for (i in 0 until 9) {
            val rowIdx = row * 9 + i
            if (rowIdx != index && cells[rowIdx] == value) return true
            val colIdx = i * 9 + col
            if (colIdx != index && cells[colIdx] == value) return true
        }

        val boxRow = (row / 3) * 3
        val boxCol = (col / 3) * 3
        for (r in boxRow until boxRow + 3) {
            for (c in boxCol until boxCol + 3) {
                val boxIdx = r * 9 + c
                if (boxIdx != index && cells[boxIdx] == value) return true
            }
        }
        return false
    }

    /** Conta quantas células editáveis ainda estão vazias. */
    fun emptyCount(): Int = cells.count { it == 0 }

    /** Cria uma cópia independente do tabuleiro (preserva o estado atual). */
    fun copy(): SudokuBoard {
        val board = SudokuBoard(solution.copyOf(), puzzle.copyOf())
        cells.copyInto(board.cells)
        return board
    }

    companion object {
        const val SIZE = 9
        const val TOTAL_CELLS = 81

        fun rowOf(index: Int) = index / 9
        fun colOf(index: Int) = index % 9
        fun boxOf(index: Int): Int {
            val row = index / 9
            val col = index % 9
            return (row / 3) * 3 + (col / 3)
        }
    }
}
