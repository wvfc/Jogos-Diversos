package com.joguecomigo.domain.tictactoe

/**
 * Modelos básicos do Jogo da Velha (Tic-Tac-Toe) 3x3.
 *
 * Convenções:
 * - O tabuleiro é representado por um [IntArray] de 9 posições (índices 0..8),
 *   organizadas em linhas (0,1,2 / 3,4,5 / 6,7,8).
 * - 0 = célula vazia, 1 = jogador humano (X), 2 = máquina (O).
 * - O jogador humano sempre usa X e começa a partida.
 */

/** Marcas possíveis em uma célula. */
object Mark {
    const val EMPTY = 0
    const val HUMAN = 1   // X
    const val MACHINE = 2 // O

    /** Símbolo exibido na interface. */
    fun symbol(value: Int): String = when (value) {
        HUMAN -> "X"
        MACHINE -> "O"
        else -> ""
    }
}

/** Resultado de uma partida de Jogo da Velha. */
enum class TicTacToeResult {
    /** Partida ainda em andamento. */
    ONGOING,

    /** O jogador humano (X) venceu. */
    HUMAN_WINS,

    /** A máquina (O) venceu. */
    MACHINE_WINS,

    /** Empate (velha) — tabuleiro cheio sem vencedor. */
    DRAW,
}
