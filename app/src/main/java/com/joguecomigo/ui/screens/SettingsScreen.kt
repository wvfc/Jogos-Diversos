package com.joguecomigo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joguecomigo.data.GameMode
import com.joguecomigo.data.ThemeOption
import com.joguecomigo.viewmodel.AppViewModelProvider
import com.joguecomigo.viewmodel.SettingsViewModel

/** Tela de configurações do aplicativo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val settings by viewModel.settings.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }
    var apiKeyField by remember(settings.apiKey) { mutableStateOf(settings.apiKey) }
    var apiUrlField by remember(settings.apiUrl) { mutableStateOf(settings.apiUrl) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurações") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Modo de jogo.
            SettingsSection(title = "Modo de jogo") {
                GameMode.entries.forEach { mode ->
                    OptionRow(
                        label = mode.label,
                        selected = settings.gameMode == mode,
                        onClick = { viewModel.setGameMode(mode) },
                    )
                }
                Text(
                    "No modo IA, é preciso configurar uma chave de API abaixo. " +
                        "Sem chave/Internet, o app usa a máquina local automaticamente.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            // Dicas.
            SettingsSection(title = "Dicas") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Ativar dicas")
                    Switch(
                        checked = settings.hintsEnabled,
                        onCheckedChange = { viewModel.setHintsEnabled(it) },
                    )
                }
            }

            // Tema.
            SettingsSection(title = "Tema") {
                ThemeOption.entries.forEach { theme ->
                    OptionRow(
                        label = theme.label,
                        selected = settings.theme == theme,
                        onClick = { viewModel.setTheme(theme) },
                    )
                }
            }

            // IA externa.
            SettingsSection(title = "IA externa (opcional)") {
                OutlinedTextField(
                    value = apiKeyField,
                    onValueChange = { apiKeyField = it },
                    label = { Text("Chave de API") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = apiUrlField,
                    onValueChange = { apiUrlField = it },
                    label = { Text("URL da API (compatível com OpenAI)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        viewModel.setApiKey(apiKeyField)
                        viewModel.setApiUrl(apiUrlField)
                    },
                ) {
                    Text("Salvar chave de API")
                }
                Text(
                    "A chave fica salva apenas neste dispositivo e nunca é embutida no app.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            // Resetar progresso.
            SettingsSection(title = "Progresso") {
                OutlinedButton(onClick = { showResetDialog = true }) {
                    Text("Resetar progresso")
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Resetar progresso") },
            text = { Text("Tem certeza? Todo o XP, níveis e histórico serão apagados.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetProgress()
                    showResetDialog = false
                }) { Text("Resetar") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun OptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}
