package com.joguecomigo.viewmodel

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.joguecomigo.JogueComigoApp

/**
 * Fábrica central de ViewModels do app (arquitetura MVVM).
 * Injeta os gerenciadores do [AppContainer] em cada ViewModel.
 */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            SettingsViewModel(app().container.settingsManager, app().container.progressManager)
        }
        initializer {
            ProgressViewModel(app().container.progressManager)
        }
        initializer {
            SudokuGameViewModel(app().container.progressManager, app().container.settingsManager)
        }
        initializer {
            CheckersGameViewModel(app().container.progressManager, app().container.settingsManager)
        }
    }
}

/** Recupera a instância de [JogueComigoApp] a partir das CreationExtras. */
private fun CreationExtras.app(): JogueComigoApp =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as JogueComigoApp
