package com.joguecomigo.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joguecomigo.domain.sudoku.SudokuBoard
import com.joguecomigo.ui.theme.ErrorRed
import com.joguecomigo.ui.theme.SuccessGreen
import com.joguecomigo.viewmodel.AppViewModelProvider
import com.joguecomigo.viewmodel.SudokuGameViewModel
import com.joguecomigo.viewmodel.SudokuUiState

/** Tela do jogo de Sudoku. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SudokuScreen(
    onBack: () -> Unit,
    viewModel: SudokuGameViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsState()

    // Gera a primeira partida ao abrir a tela.
    LaunchedEffect(Unit) {
        if (state.cells.isEmpty()) {
            viewModel.newGame()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sudoku — ${state.difficulty.label}") },
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
                .padding(12.dp),
        ) {
            // Barra de status: timer, erros, dicas.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                StatusItem("Tempo", formatTime(state.elapsedSeconds))
                StatusItem("Erros", state.mistakes.toString())
                StatusItem("Dicas", state.hintsUsed.toString())
            }

            Spacer(Modifier.height(8.dp))

            DifficultySelector(
                selected = state.difficulty,
                unlocked = viewModel.unlockedDifficulty(),
                onSelect = { viewModel.newGame(it) },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            if (state.isLoading || state.cells.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                SudokuGrid(state = state, onCellClick = viewModel::selectCell)
            }

            Spacer(Modifier.height(12.dp))

            // Teclado numérico 1-9.
            NumberPad(onNumber = viewModel::inputNumber)

            Spacer(Modifier.height(8.dp))

            // Ações.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = viewModel::eraseCell, modifier = Modifier.weight(1f)) {
                    Text("Apagar")
                }
                OutlinedButton(onClick = viewModel::checkSelectedMove, modifier = Modifier.weight(1f)) {
                    Text("Verificar")
                }
                if (state.hintsAllowed) {
                    OutlinedButton(onClick = viewModel::requestHint, modifier = Modifier.weight(1f)) {
                        Text("Dica")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = viewModel::restart, modifier = Modifier.weight(1f)) {
                    Text("Reiniciar")
                }
                Button(onClick = { viewModel.newGame() }, modifier = Modifier.weight(1f)) {
                    Text("Nova partida")
                }
                if (state.aiMode) {
                    OutlinedButton(onClick = viewModel::analyzeSelected, modifier = Modifier.weight(1f)) {
                        Text("Analisar (IA)")
                    }
                }
            }

            // Mensagem do assistente.
            AnimatedVisibility(visible = state.assistantMessage != null) {
                AssistantBanner(
                    message = state.assistantMessage.orEmpty(),
                    onDismiss = viewModel::dismissMessage,
                )
            }
        }
    }

    // Diálogo de vitória.
    if (state.isSolved) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Parabéns! 🎉") },
            text = {
                Text(
                    "Você resolveu o Sudoku ${state.difficulty.label} em " +
                        "${formatTime(state.elapsedSeconds)} com ${state.mistakes} erro(s) e " +
                        "${state.hintsUsed} dica(s). Seu progresso foi salvo!"
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.newGame() }) { Text("Nova partida") }
            },
            dismissButton = {
                TextButton(onClick = onBack) { Text("Voltar") }
            },
        )
    }
}

@Composable
private fun StatusItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SudokuGrid(state: SudokuUiState, onCellClick: (Int) -> Unit) {
    val selected = state.selectedIndex
    val selRow = selected?.let { SudokuBoard.rowOf(it) }
    val selCol = selected?.let { SudokuBoard.colOf(it) }
    val selBox = selected?.let { SudokuBoard.boxOf(it) }

    val gridLineColor = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .drawBehind {
                // Linhas grossas dos blocos 3x3.
                val step = size.width / 9f
                for (i in 0..9) {
                    val stroke = if (i % 3 == 0) 4f else 1.5f
                    val pos = i * step
                    drawLine(gridLineColor, Offset(pos, 0f), Offset(pos, size.height), stroke)
                    drawLine(gridLineColor, Offset(0f, pos), Offset(size.width, pos), stroke)
                }
            },
    ) {
        for (row in 0 until 9) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                for (col in 0 until 9) {
                    val index = row * 9 + col
                    val value = state.cells.getOrElse(index) { 0 }
                    val isGiven = state.given.getOrElse(index) { false }
                    val isConflict = index in state.conflicts
                    val isSelected = index == selected
                    val isPeer = selected != null &&
                        (row == selRow || col == selCol || SudokuBoard.boxOf(index) == selBox)
                    val isHint = index == state.hintCellIndex

                    val background = when {
                        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        isHint -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.30f)
                        isConflict -> ErrorRed.copy(alpha = 0.20f)
                        isPeer -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                        else -> Color.Transparent
                    }

                    val textColor = when {
                        isConflict -> ErrorRed
                        isGiven -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.primary
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(background)
                            .clickable { onCellClick(index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (value != 0) {
                            Text(
                                text = value.toString(),
                                color = textColor,
                                fontWeight = if (isGiven) FontWeight.Bold else FontWeight.Normal,
                                style = MaterialTheme.typography.titleLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberPad(onNumber: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (n in 1..9) {
            OutlinedButton(
                onClick = { onNumber(n) },
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            ) {
                Text(n.toString(), style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
private fun AssistantBanner(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("Assistente", fontWeight = FontWeight.Bold, color = SuccessGreen)
            Text(message, modifier = Modifier.padding(top = 4.dp), textAlign = TextAlign.Start)
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("Ok")
            }
        }
    }
}
