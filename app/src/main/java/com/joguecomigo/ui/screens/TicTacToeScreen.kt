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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joguecomigo.domain.tictactoe.TicTacToeMark
import com.joguecomigo.domain.tictactoe.TicTacToeResult
import com.joguecomigo.viewmodel.AppViewModelProvider
import com.joguecomigo.viewmodel.TicTacToeGameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicTacToeScreen(onBack: () -> Unit) {
    val viewModel: TicTacToeGameViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Jogo da Velha") },
                navigationIcon = {
                    Text(
                        text = "← Voltar",
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .clickable { onBack() },
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Seletor de dificuldade
            DifficultyChipRow(
                selected = state.difficulty,
                onSelect = { viewModel.selectDifficulty(it) },
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Mensagem de status
            Text(
                text = state.statusMessage,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = when (state.result) {
                    TicTacToeResult.X_WINS,
                    TicTacToeResult.O_WINS -> MaterialTheme.colorScheme.primary
                    TicTacToeResult.DRAW   -> MaterialTheme.colorScheme.secondary
                    else                   -> MaterialTheme.colorScheme.onBackground
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tabuleiro 3x3
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                for (row in 0..2) {
                    Row {
                        for (col in 0..2) {
                            val index = row * 3 + col
                            TicTacToeCell(
                                mark = state.board.markAt(index),
                                enabled = state.board.canPlay(index) && state.isPlayerTurn,
                                onClick = { viewModel.playerMove(index) },
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botão novo jogo (aparece só quando a partida termina)
            AnimatedVisibility(visible = state.finished) {
                Button(
                    onClick = { viewModel.newGame() },
                    modifier = Modifier.fillMaxWidth(0.6f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Text("Jogar Novamente", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TicTacToeCell(
    mark: TicTacToeMark,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val markColor = when (mark) {
        TicTacToeMark.X     -> MaterialTheme.colorScheme.primary
        TicTacToeMark.O     -> MaterialTheme.colorScheme.error
        TicTacToeMark.EMPTY -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)
    }

    Box(
        modifier = Modifier
            .size(100.dp)
            .padding(4.dp)
            .border(2.dp, MaterialTheme.colorScheme.outline)
            .background(
                if (enabled) MaterialTheme.colorScheme.surface
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = mark.label,
            fontSize = 40.sp,
            fontWeight = FontWeight.ExtraBold,
            color = markColor,
        )
    }
}
