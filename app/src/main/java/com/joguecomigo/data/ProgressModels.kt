package com.joguecomigo.data

import com.joguecomigo.domain.Difficulty
import kotlinx.serialization.Serializable

/** Jogos disponíveis no aplicativo. */
@Serializable
enum class GameType(val label: String) {
    SUDOKU("Sudoku"),
    CHECKERS("Dama")
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
 * @param durationSeconds Duração total da partida.
 * @param mistakes Erros cometidos (Sudoku) ou peças perdidas (Dama).
 * @param hintsUsed Quantidade de dicas usadas.
 * @param timestamp Momento da conclusão (epoch millis).
 */
@Serializable
data class MatchRecord(
    val game: GameType,
    val outcome: MatchOutcome,
    val difficultyOrder: Int,
    val xpGained: Int,
    val durationSeconds: Long,
    val mistakes: Int,
    val hintsUsed: Int,
    val timestamp: Long,
) {
    val difficulty: Difficulty get() = Difficulty.fromOrder(difficultyOrder)
}

/**
 * Estatísticas acumuladas por jogo.
 */
@Serializable
data class GameStats(
    val xp: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    val matchesPlayed: Int = 0,
    val totalDurationSeconds: Long = 0,
    val totalHintsUsed: Int = 0,
) {
    /** Nível derivado do XP (cada 100 de XP = 1 nível). */
    val level: Int get() = (xp / 100) + 1

    /** Dificuldade desbloqueada de acordo com o nível atual. */
    val unlockedDifficulty: Difficulty get() = Difficulty.fromLevel(level)

    /** Tempo médio por partida, em segundos. */
    val averageDurationSeconds: Long
        get() = if (matchesPlayed > 0) totalDurationSeconds / matchesPlayed else 0

    /** XP que falta para o próximo nível. */
    val xpIntoLevel: Int get() = xp % 100
    val xpForNextLevel: Int get() = 100
}

/**
 * Progresso completo do jogador, salvo localmente.
 */
@Serializable
data class PlayerProgress(
    val playerName: String = "Jogador",
    val sudoku: GameStats = GameStats(),
    val checkers: GameStats = GameStats(),
    val history: List<MatchRecord> = emptyList(),
) {
    fun statsFor(game: GameType): GameStats = when (game) {
        GameType.SUDOKU -> sudoku
        GameType.CHECKERS -> checkers
    }

    /**
     * Dificuldade sugerida para a próxima partida (sistema dinâmico).
     *
     * Parte da dificuldade desbloqueada e a reduz em um nível caso o
     * desempenho recente esteja ruim (muitas derrotas/dicas nas últimas
     * partidas), conforme os critérios de progressão.
     */
    fun suggestedDifficulty(game: GameType): Difficulty {
        val stats = statsFor(game)
        val unlocked = stats.unlockedDifficulty
        val recent = history.filter { it.game == game }.takeLast(3)
        if (recent.size < 3) return unlocked

        val poorPerformance = recent.count { it.outcome == MatchOutcome.LOSS } >= 2 ||
            recent.sumOf { it.hintsUsed } >= 8
        return if (poorPerformance && unlocked.order > 0) {
            Difficulty.fromOrder(unlocked.order - 1)
        } else {
            unlocked
        }
    }
}
