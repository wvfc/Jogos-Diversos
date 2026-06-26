package com.joguecomigo.domain.tictactoe

enum class TicTacToeMark(val label: String) {
    EMPTY(""),
    X("X"),
    O("O");

    fun opponent(): TicTacToeMark = when (this) {
        X -> O
        O -> X
        EMPTY -> EMPTY
    }
}

enum class TicTacToeResult {
    IN_PROGRESS,
    X_WINS,
    O_WINS,
    DRAW
}
