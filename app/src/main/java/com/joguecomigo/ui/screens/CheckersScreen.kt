package com.joguecomigo.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joguecomigo.domain.checkers.Cell
import com.joguecomigo.domain.checkers.CheckersBoard
import com.joguecomigo.domain.checkers.Side
import com.joguecomigo.ui.theme.SuccessGreen
import com.joguecomigo.viewmodel.AppViewModelProvider
import com.joguecomigo.viewmodel.CheckersGameViewModel
import com.joguecomigo.viewmodel.CheckersUiState

/** Tela do jogo de Dama. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckersScreen(
    onBack: () -> Unit,
    viewModel: CheckersGameViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        if (state.squares.isEmpty()) {
            viewModel.newGame()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dama — ${state.difficulty.label}") },
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                StatusItemCk("Tempo", formatTime(state.elapsedSeconds))
                StatusItemCk("Suas peças", state.whiteCount.toString())
                StatusItemCk("Adversário", state.blackCount.toString())
                StatusItemCk("Dicas", state.hintsUsed.toString())
            }

            Spacer(Modifier.height(8.dp))

            val turnText = when {
                state.isGameOver -> "Fim de jogo"
                state.isThinking -> "Máquina pensando..."
                state.turn == state.humanSide -> "Sua vez (Brancas)"
                else -> "Vez do adversário"
            }
            Text(
                turnText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp),
            )

            DifficultySelector(
                selected = state.difficulty,
                unlocked = viewModel.unlockedDifficulty(),
                onSelect = { viewModel.newGame(it) },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            CheckersBoardView(state = state, onSquareClick = viewModel::onSquareTapped)

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.hintsAllowed) {
                    OutlinedButton(onClick = viewModel::requestHint, modifier = Modifier.weight(1f)) {
                        Text(if (state.aiMode) "Sugerir (IA)" else "Dica")
                    }
                }
                Button(onClick = { viewModel.newGame() }, modifier = Modifier.weight(1f)) {
                    Text("Nova partida")
                }
            }

            AnimatedVisibility(visible = state.assistantMessage != null) {
                AssistantBannerCk(
                    message = state.assistantMessage.orEmpty(),
                    onDismiss = viewModel::dismissMessage,
                )
            }
        }
    }

    if (state.isGameOver) {
        val playerWon = state.winner == state.humanSide
        AlertDialog(
            onDismissRequest = { },
            title = { Text(if (playerWon) "Você venceu! 🏆" else "Você perdeu") },
            text = {
                Text(
                    if (playerWon) {
                        "Mandou bem na Dama ${state.difficulty.label}! Progresso salvo."
                    } else {
                        "Não foi dessa vez. Tente novamente e melhore sua estratégia!"
                    }
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
private fun StatusItemCk(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CheckersBoardView(state: CheckersUiState, onSquareClick: (Int) -> Unit) {
    val lightSquare = Color(0xFFEED9B6)
    val darkSquare = Color(0xFF7A4B25)
    val highlightTarget = SuccessGreen.copy(alpha = 0.55f)
    val highlightSelected = Color(0xFF2196F3).copy(alpha = 0.6f)
    val highlightSuggest = Color(0xFFFFC107).copy(alpha = 0.7f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .border(2.dp, MaterialTheme.colorScheme.onSurface),
    ) {
        for (row in 0 until 8) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                for (col in 0 until 8) {
                    val index = row * 8 + col
                    val playable = CheckersBoard.isPlayable(index)
                    val baseColor = if (playable) darkSquare else lightSquare

                    val overlay = when {
                        index == state.suggestedFrom || index == state.suggestedTo -> highlightSuggest
                        index == state.selectedIndex -> highlightSelected
                        index in state.legalTargets -> highlightTarget
                        else -> Color.Transparent
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(baseColor)
                            .background(overlay)
                            .clickable(enabled = playable) { onSquareClick(index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        val piece = state.squares.getOrElse(index) { Cell.EMPTY }
                        if (piece != Cell.EMPTY) {
                            CheckerPiece(piece)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckerPiece(piece: Int) {
    val isWhite = Cell.isWhite(piece)
    val isKing = Cell.isKing(piece)
    val pieceColor = if (isWhite) Color(0xFFFAFAFA) else Color(0xFF1A1A1A)
    val borderColor = if (isWhite) Color(0xFFBDBDBD) else Color(0xFF000000)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(6.dp)
            .background(pieceColor, CircleShape)
            .border(2.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (isKing) {
            // Marca de dama (rei): coroa simbolizada por uma letra "D".
            Text(
                "D",
                color = if (isWhite) Color(0xFF7A4B25) else Color(0xFFFFC107),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@Composable
private fun AssistantBannerCk(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("Assistente", fontWeight = FontWeight.Bold, color = SuccessGreen)
            Text(message, modifier = Modifier.padding(top = 4.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("Ok")
            }
            Spacer(Modifier.size(0.dp))
        }
    }
}
