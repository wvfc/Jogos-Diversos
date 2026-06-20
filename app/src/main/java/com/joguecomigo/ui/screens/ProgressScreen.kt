package com.joguecomigo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joguecomigo.data.GameStats
import com.joguecomigo.data.GameType
import com.joguecomigo.data.MatchRecord
import com.joguecomigo.viewmodel.AppViewModelProvider
import com.joguecomigo.viewmodel.ProgressViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Tela de perfil/progresso do jogador. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    onBack: () -> Unit,
    viewModel: ProgressViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val progress by viewModel.progress.collectAsState()
    var editingName by remember { mutableStateOf(false) }
    var nameField by remember { mutableStateOf(progress.playerName) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Progresso") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Jogador", style = MaterialTheme.typography.labelMedium)
                        if (editingName) {
                            OutlinedTextField(
                                value = nameField,
                                onValueChange = { nameField = it },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                Text(
                                    text = "Salvar",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .clickableText {
                                            viewModel.setPlayerName(nameField)
                                            editingName = false
                                        },
                                )
                            }
                        } else {
                            Text(
                                text = progress.playerName,
                                style = MaterialTheme.typography.headlineMedium,
                                modifier = Modifier.clickableText {
                                    nameField = progress.playerName
                                    editingName = true
                                },
                            )
                            Text(
                                "Toque no nome para editar",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            item { GameStatsCard(title = "Sudoku", stats = progress.sudoku) }
            item { GameStatsCard(title = "Dama", stats = progress.checkers) }

            item {
                Text(
                    "Histórico de partidas",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (progress.history.isEmpty()) {
                item { Text("Nenhuma partida registrada ainda. Bora jogar!") }
            } else {
                items(progress.history.reversed()) { record ->
                    HistoryRow(record)
                }
            }
        }
    }
}

@Composable
private fun GameStatsCard(title: String, stats: GameStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Nível ${stats.level}  •  ${stats.xp} XP")
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { stats.xpIntoLevel / stats.xpForNextLevel.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Text("Dificuldade desbloqueada: ${stats.unlockedDifficulty.label}")
            Text("Vitórias: ${stats.wins}  •  Derrotas: ${stats.losses}  •  Empates: ${stats.draws}")
            Text("Partidas: ${stats.matchesPlayed}")
            Text("Tempo médio: ${formatTime(stats.averageDurationSeconds)}")
            Text("Dicas usadas: ${stats.totalHintsUsed}")
        }
    }
}

@Composable
private fun HistoryRow(record: MatchRecord) {
    val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                val gameLabel = if (record.game == GameType.SUDOKU) "Sudoku" else "Dama"
                Text("$gameLabel • ${record.difficulty.label}", fontWeight = FontWeight.SemiBold)
                Text(
                    "${record.outcome.label} • ${formatTime(record.durationSeconds)} • " +
                        "+${record.xpGained} XP",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(dateFormat.format(Date(record.timestamp)), style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Pequeno helper para tornar um texto clicável. */
private fun Modifier.clickableText(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)
