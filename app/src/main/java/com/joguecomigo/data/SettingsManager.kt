package com.joguecomigo.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Modo de jogo: contra a máquina local ou com auxílio de IA. */
enum class GameMode(val label: String) {
    MACHINE("Contra Máquina"),
    AI("Com auxílio de IA")
}

/** Opção de tema da interface. */
enum class ThemeOption(val label: String) {
    SYSTEM("Automático (sistema)"),
    LIGHT("Claro"),
    DARK("Escuro")
}

/**
 * Configurações do aplicativo.
 *
 * A chave de API nunca é embutida no código: fica somente neste armazenamento
 * local, preenchida pelo usuário. Sem chave/Internet, o app usa o modo máquina.
 */
data class AppSettings(
    val gameMode: GameMode = GameMode.MACHINE,
    val hintsEnabled: Boolean = true,
    val theme: ThemeOption = ThemeOption.SYSTEM,
    val apiKey: String = "",
    val apiUrl: String = "https://api.openai.com/v1/chat/completions",
) {
    /** Indica se há uma chave de API configurada para a IA externa. */
    val hasApiKey: Boolean get() = apiKey.isNotBlank()
}

/**
 * Gerencia a persistência das configurações no [DataStore] de preferências.
 */
class SettingsManager(private val dataStore: DataStore<Preferences>) {

    private object Keys {
        val GAME_MODE = stringPreferencesKey("settings_game_mode")
        val HINTS = booleanPreferencesKey("settings_hints_enabled")
        val THEME = stringPreferencesKey("settings_theme")
        val API_KEY = stringPreferencesKey("settings_api_key")
        val API_URL = stringPreferencesKey("settings_api_url")
    }

    /** Fluxo reativo com as configurações atuais. */
    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            gameMode = prefs[Keys.GAME_MODE]?.let { runCatching { GameMode.valueOf(it) }.getOrNull() }
                ?: GameMode.MACHINE,
            hintsEnabled = prefs[Keys.HINTS] ?: true,
            theme = prefs[Keys.THEME]?.let { runCatching { ThemeOption.valueOf(it) }.getOrNull() }
                ?: ThemeOption.SYSTEM,
            apiKey = prefs[Keys.API_KEY] ?: "",
            apiUrl = prefs[Keys.API_URL] ?: "https://api.openai.com/v1/chat/completions",
        )
    }

    suspend fun setGameMode(mode: GameMode) = dataStore.edit { it[Keys.GAME_MODE] = mode.name }
    suspend fun setHintsEnabled(enabled: Boolean) = dataStore.edit { it[Keys.HINTS] = enabled }
    suspend fun setTheme(theme: ThemeOption) = dataStore.edit { it[Keys.THEME] = theme.name }
    suspend fun setApiKey(key: String) = dataStore.edit { it[Keys.API_KEY] = key.trim() }
    suspend fun setApiUrl(url: String) = dataStore.edit { it[Keys.API_URL] = url.trim() }
}
