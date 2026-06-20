package com.joguecomigo.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * Fornece instâncias únicas de [DataStore] de preferências para o app.
 * Um único arquivo armazena tanto o progresso quanto as configurações.
 */
val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "jogue_comigo")
