package com.joguecomigo

import android.app.Application
import com.joguecomigo.data.ProgressManager
import com.joguecomigo.data.SettingsManager
import com.joguecomigo.data.appDataStore

/**
 * Container simples de dependências (Service Locator) do aplicativo.
 * Mantém instâncias únicas dos gerenciadores de dados.
 */
class AppContainer(application: Application) {
    val progressManager: ProgressManager = ProgressManager(application.appDataStore)
    val settingsManager: SettingsManager = SettingsManager(application.appDataStore)
}

/**
 * Classe [Application] do "Jogue Comigo". Inicializa o container de
 * dependências usado pelos ViewModels (arquitetura MVVM).
 */
class JogueComigoApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
