package com.joguecomigo.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Gerencia a persistência local do progresso do jogador.
 *
 * Serializa [PlayerProgress] em JSON e o guarda no [DataStore] de preferências,
 * garantindo que todo o progresso seja salvo offline no dispositivo.
 */
class ProgressManager(private val dataStore: DataStore<Preferences>) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val progressKey = stringPreferencesKey("player_progress")

    /** Fluxo reativo com o progresso atual do jogador. */
    val progress: Flow<PlayerProgress> = dataStore.data.map { prefs ->
        prefs[progressKey]?.let { raw ->
            runCatching { json.decodeFromString<PlayerProgress>(raw) }.getOrNull()
        } ?: PlayerProgress()
    }

    private suspend fun update(transform: (PlayerProgress) -> PlayerProgress) {
        dataStore.edit { prefs ->
            val current = prefs[progressKey]?.let {
                runCatching { json.decodeFromString<PlayerProgress>(it) }.getOrNull()
            } ?: PlayerProgress()
            prefs[progressKey] = json.encodeToString(transform(current))
        }
    }

    /** Atualiza o nome do jogador. */
    suspend fun setPlayerName(name: String) = update { it.copy(playerName = name.trim().ifBlank { "Jogador" }) }

    /**
     * Registra o resultado de uma partida, atualizando estatísticas,
     * XP, nível e histórico. O avanço de dificuldade é automático,
     * pois decorre do XP acumulado.
     */
    suspend fun recordMatch(record: MatchRecord) = update { progress ->
        val updatedStats = progress.statsFor(record.game).let { stats ->
            stats.copy(
                xp = stats.xp + record.xpGained,
                wins = stats.wins + if (record.outcome == MatchOutcome.WIN) 1 else 0,
                losses = stats.losses + if (record.outcome == MatchOutcome.LOSS) 1 else 0,
                draws = stats.draws + if (record.outcome == MatchOutcome.DRAW) 1 else 0,
                matchesPlayed = stats.matchesPlayed + 1,
                totalDurationSeconds = stats.totalDurationSeconds + record.durationSeconds,
                totalHintsUsed = stats.totalHintsUsed + record.hintsUsed,
            )
        }

        // Mantém o histórico limitado às últimas 50 partidas.
        val newHistory = (progress.history + record).takeLast(50)

        when (record.game) {
            GameType.SUDOKU -> progress.copy(sudoku = updatedStats, history = newHistory)
            GameType.CHECKERS -> progress.copy(checkers = updatedStats, history = newHistory)
        }
    }

    /** Reseta todo o progresso, preservando o nome do jogador. */
    suspend fun resetProgress() = update { PlayerProgress(playerName = it.playerName) }
}
