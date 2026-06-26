package com.joguecomigo.domain.tictactoe

/**
 * Representa o estado imutável do tabuleiro 3x3 do Jogo da Velha.
 *
 * Cada índice de 0..8 mapeia para:
 *   0 | 1 | 2
 *   3 | 4 | 5
 *   6 | 7 | 8
 */
class TicTacToeBoard(
    val cells: List<TicTacToeMark> = List(9) { TicTacToeMark.EMPTY }
) {
    fun markAt(index: Int): TicTacToeMark = cells[index]

    fun canPlay(index: Int): Boolean =
        index in 0..8 &&
        cells[index] == TicTacToeMark.EMPTY &&
        result() == TicTacToeResult.IN_PROGRESS

    fun play(index: Int, mark: TicTacToeMark): TicTacToeBoard {
        if (!canPlay(index) || mark == TicTacToeMark.EMPTY) return this
        return TicTacToeBoard(cells.toMutableList().also { it[index] = mark })
    }

    fun availableMoves(): List<Int> {
        if (result() != TicTacToeResult.IN_PROGRESS) return emptyList()
        return cells.mapIndexedNotNull { i, m -> if (m == TicTacToeMark.EMPTY) i else null }
    }

    fun result(): TicTacToeResult {
        val lines = listOf(
            listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
            listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
            listOf(0, 4, 8), listOf(2, 4, 6)
        )
        for (line in lines) {
            val a = cells[line[0]]; val b = cells[line[1]]; val c = cells[line[2]]
            if (a != TicTacToeMark.EMPTY && a == b && b == c)
                return if (a == TicTacToeMark.X) TicTacToeResult.X_WINS else TicTacToeResult.O_WINS
        }
        return if (cells.all { it != TicTacToeMark.EMPTY }) TicTacToeResult.DRAW
               else TicTacToeResult.IN_PROGRESS
    }
}
