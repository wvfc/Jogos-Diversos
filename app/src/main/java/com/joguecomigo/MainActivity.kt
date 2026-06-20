package com.joguecomigo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joguecomigo.data.ThemeOption
import com.joguecomigo.ui.navigation.AppNavigation
import com.joguecomigo.ui.theme.JogueComigoTheme
import com.joguecomigo.viewmodel.AppViewModelProvider
import com.joguecomigo.viewmodel.SettingsViewModel

/**
 * Única Activity do app (padrão Single-Activity + Jetpack Compose).
 * Aplica o tema escolhido nas configurações e exibe a navegação.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
            val settings by settingsViewModel.settings.collectAsState()

            val darkTheme = when (settings.theme) {
                ThemeOption.SYSTEM -> isSystemInDarkTheme()
                ThemeOption.LIGHT -> false
                ThemeOption.DARK -> true
            }

            JogueComigoTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}
