package com.joguecomigo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joguecomigo.data.GameType
import com.joguecomigo.data.MatchOutcome
import com.joguecomigo.data.MatchRecord
import com.joguecomigo.data.ProgressManager
import com.joguecomigo.data.SettingsManager
import com.joguecomigo.data.XpCalculator
import com.joguecomigo.domain.Difficulty
import com.joguecomigo.domain.tictactoe.TicTacToeBoard
import com.joguecomigo.domain.tictactoe.TicTacToeEngine
import com.joguecomigo.domain.tictactoe.TicTacToeMark
import com.joguecomigo.domain.tictactoe.TicTacToeResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estado da tela do Jogo da Velha. */
data class TicTacToeUiState(
    val board: TicTacToeBoard = TicTacToeBoard(),
    val playerMark: TicTacToeMark = TicTacToeMark.X,
    val aiMark: TicTacToeMark = TicTacToeMark.O,
    val difficulty: Difficulty = Difficulty.MEDIO,
    val result: TicTacToeResult = TicTacToeResult.IN_PROGRESS,
    val isPlayerTurn: Boolean = true,
    val statusMessage: String = "Sua vez!",
    val finished: Boolean = false,
)

class TicTacToeGameViewModel(
    private val progressManager: ProgressManager,
    private val settingsManager: SettingsManager,
) : ViewModel() {

    private val engine = TicTacToeEngine()
    private val _uiState = MutableStateFlow(TicTacToeUiState())
    val uiState: StateFlow<TicTacToeUiState> = _uiState.asStateFlow()

    private var startTime: Long = System.currentTimeMillis()

    fun newGame(difficulty: Difficulty = _uiState.value.difficulty) {
        startTime = System.currentTimeMillis()
        _uiState.value = TicTacToeUiState(difficulty = difficulty, statusMessage = "Sua vez!")
    }

    fun selectDifficulty(difficulty: Difficulty) = newGame(difficulty)

    fun playerMove(index: Int) {
        val state = _uiState.value
        if (!state.isPlayerTurn || state.finished || !state.board.canPlay(index)) return

        val newBoard = state.board.play(index, state.playerMark)
        val result = newBoard.result()

        if (result != TicTacToeResult.IN_PROGRESS) {
            finishGame(newBoard, result, state)
            return
        }

        _uiState.value = state.copy(
            board = newBoard,
            isPlayerTurn = false,
            statusMessage = "Vez da máquina...",
        )

        viewModelScope.launch {
            delay(600)
            aiMove()
        }
    }

    private fun aiMove() {
        val state = _uiState.value
        val index = engine.bestMove(state.board, state.aiMark, state.difficulty) ?: return
        val newBoard = state.board.play(index, state.aiMark)
        val result = newBoard.result()

        if (result != TicTacToeResult.IN_PROGRESS) {
            finishGame(newBoard, result, state)
            return
        }

        _uiState.value = state.copy(
            board = newBoard,
            isPlayerTurn = true,
            statusMessage = "Sua vez!",
        )
    }

    private fun finishGame(
        board: TicTacToeBoard,
        result: TicTacToeResult,
        state: TicTacToeUiState,
    ) {
        val outcome = when (result) {
            TicTacToeResult.X_WINS ->
                if (state.playerMark == TicTacToeMark.X) MatchOutcome.WIN else MatchOutcome.LOSS
            TicTacToeResult.O_WINS ->
                if (state.playerMark == TicTacToeMark.O) MatchOutcome.WIN else MatchOutcome.LOSS
            TicTacToeResult.DRAW -> MatchOutcome.DRAW
            TicTacToeResult.IN_PROGRESS -> return
        }

        val message = when (outcome) {
            MatchOutcome.WIN  -> "Você venceu! 🎉"
            MatchOutcome.LOSS -> "Máquina venceu! 🤖"
            MatchOutcome.DRAW -> "Empate! 🤝"
        }

        val duration = (System.currentTimeMillis() - startTime) / 1000

        _uiState.value = state.copy(
            board = board,
            result = result,
            finished = true,
            isPlayerTurn = false,
            statusMessage = message,
        )

        viewModelScope.launch {
            val xp = XpCalculator.ticTacToeXp(
                outcome = outcome,
                difficulty = state.difficulty,
                durationSeconds = duration,
            )
            progressManager.recordMatch(
                MatchRecord(
                    gameType = GameType.TICTACTOE,
                    outcome = outcome,
                    difficultyOrder = state.difficulty.order,
                    durationSeconds = duration,
                    xpEarned = xp,
                )
            )
        }
    }
}
