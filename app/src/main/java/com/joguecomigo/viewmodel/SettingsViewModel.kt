package com.joguecomigo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joguecomigo.data.AppSettings
import com.joguecomigo.data.GameMode
import com.joguecomigo.data.ProgressManager
import com.joguecomigo.data.SettingsManager
import com.joguecomigo.data.ThemeOption
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel da tela de configurações. Expõe e atualiza [AppSettings] e
 * também permite resetar todo o progresso.
 */
class SettingsViewModel(
    private val settingsManager: SettingsManager,
    private val progressManager: ProgressManager,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsManager.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings(),
    )

    fun setGameMode(mode: GameMode) = viewModelScope.launch { settingsManager.setGameMode(mode) }
    fun setHintsEnabled(enabled: Boolean) = viewModelScope.launch { settingsManager.setHintsEnabled(enabled) }
    fun setTheme(theme: ThemeOption) = viewModelScope.launch { settingsManager.setTheme(theme) }
    fun setApiKey(key: String) = viewModelScope.launch { settingsManager.setApiKey(key) }
    fun setApiUrl(url: String) = viewModelScope.launch { settingsManager.setApiUrl(url) }

    fun resetProgress() = viewModelScope.launch { progressManager.resetProgress() }
}
