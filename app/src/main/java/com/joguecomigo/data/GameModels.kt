package com.joguecomigo.data

import com.joguecomigo.domain.Difficulty
import kotlinx.serialization.Serializable

/** Jogos disponíveis no aplicativo. */
@Serializable
enum class GameType(val label: String) {
    SUDOKU("Sudoku"),
    CHECKERS("Dama"),
    TICTACTOE("Jogo da Velha")
}

/** Resultado possível de uma partida. */
@Serializable
enum class MatchOutcome(val label: String) {
    WIN("Vitória"),
    LOSS("Derrota"),
    DRAW("Empate")
}

/**
 * Registro de uma partida concluída (compõe o histórico).
 *
 * @param difficultyOrder Ordem da dificuldade (ver [Difficulty]).
 * @param durationSeconds Duração total da partida em segundos.
 * @param xpEarned XP ganho nesta partida.
 */
@Serializable
data class MatchRecord(
    val gameType: GameType,
    val outcome: MatchOutcome,
    val difficultyOrder: Int,
    val durationSeconds: Long,
    val xpEarned: Int,
)
