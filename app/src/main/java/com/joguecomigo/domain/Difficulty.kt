package com.joguecomigo.domain

/**
 * Níveis de dificuldade compartilhados pelos dois jogos (Sudoku e Dama).
 *
 * @property label Nome exibido na interface (português do Brasil).
 * @property order Ordem crescente de dificuldade, usada na progressão.
 */
enum class Difficulty(val label: String, val order: Int) {
    FACIL("Fácil", 0),
    MEDIO("Médio", 1),
    DIFICIL("Difícil", 2),
    ESPECIALISTA("Especialista", 3);

    companion object {
        /** Retorna a dificuldade liberada para um determinado nível do jogador. */
        fun fromLevel(level: Int): Difficulty = when {
            level <= 5 -> FACIL
            level <= 12 -> MEDIO
            level <= 20 -> DIFICIL
            else -> ESPECIALISTA
        }

        /** Converte um valor de ordem (0..3) na dificuldade correspondente. */
        fun fromOrder(order: Int): Difficulty =
            entries.firstOrNull { it.order == order } ?: FACIL
    }
}
