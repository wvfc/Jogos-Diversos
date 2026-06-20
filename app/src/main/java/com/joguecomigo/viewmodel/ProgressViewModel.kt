package com.joguecomigo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joguecomigo.data.PlayerProgress
import com.joguecomigo.data.ProgressManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel da tela de progresso/perfil. Expõe o [PlayerProgress] salvo.
 */
class ProgressViewModel(
    private val progressManager: ProgressManager,
) : ViewModel() {

    val progress: StateFlow<PlayerProgress> = progressManager.progress.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlayerProgress(),
    )

    fun setPlayerName(name: String) = viewModelScope.launch { progressManager.setPlayerName(name) }
}
