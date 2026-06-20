package com.joguecomigo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joguecomigo.ai.AiAssistant
import com.joguecomigo.ai.AiAssistantFactory
import com.joguecomigo.data.AppSettings
import com.joguecomigo.data.GameType
import com.joguecomigo.data.MatchOutcome
import com.joguecomigo.data.MatchRecord
import com.joguecomigo.data.PlayerProgress
import com.joguecomigo.data.ProgressManager
import com.joguecomigo.data.SettingsManager
import com.joguecomigo.data.XpCalculator
import com.joguecomigo.domain.Difficulty
import com.joguecomigo.domain.sudoku.SudokuBoard
import com.joguecomigo.domain.sudoku.SudokuGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Estado imutável da tela de Sudoku (consumido pelo Compose). */
data class SudokuUiState(
    val cells: List<Int> = emptyList(),
    val given: List<Boolean> = emptyList(),
    val selectedIndex: Int? = null,
    val conflicts: Set<Int> = emptySet(),
    val hintCellIndex: Int? = null,
    val difficulty: Difficulty = Difficulty.FACIL,
    val elapsedSeconds: Long = 0,
    val mistakes: Int = 0,
    val hintsUsed: Int = 0,
    val isSolved: Boolean = false,
    val isLoading: Boolean = true,
    val hintsAllowed: Boolean = true,
    val aiMode: Boolean = false,
    val assistantMessage: String? = null,
)

/**
 * ViewModel da partida de Sudoku. Controla geração de tabuleiro, timer,
 * erros, dicas, validação e o registro de progresso ao concluir.
 */
class SudokuGameViewModel(
    private val progressManager: ProgressManager,
    private val settingsManager: SettingsManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SudokuUiState())
    val uiState: StateFlow<SudokuUiState> = _uiState.asStateFlow()

    private var board: SudokuBoard? = null
    private var timerJob: Job? = null
    private var currentSettings: AppSettings = AppSettings()
    private var currentProgress: PlayerProgress = PlayerProgress()
    private var matchRecorded = false

    init {
        viewModelScope.launch {
            settingsManager.settings.collect { settings ->
                currentSettings = settings
                _uiState.value = _uiState.value.copy(
                    hintsAllowed = settings.hintsEnabled,
                    aiMode = settings.gameMode == com.joguecomigo.data.GameMode.AI,
                )
            }
        }
        viewModelScope.launch {
            progressManager.progress.collect { currentProgress = it }
        }
    }

    private fun assistant(): AiAssistant = AiAssistantFactory.create(currentSettings)

    /** Dificuldade sugerida para o jogador (sistema dinâmico). */
    fun suggestedDifficulty(): Difficulty = currentProgress.suggestedDifficulty(GameType.SUDOKU)

    /** Maior dificuldade desbloqueada no Sudoku. */
    fun unlockedDifficulty(): Difficulty = currentProgress.sudoku.unlockedDifficulty

    /** Inicia uma nova partida. [difficulty] nulo usa a dificuldade sugerida. */
    fun newGame(difficulty: Difficulty? = null) {
        val chosen = difficulty ?: suggestedDifficulty()
        timerJob?.cancel()
        matchRecorded = false
        _uiState.value = SudokuUiState(
            difficulty = chosen,
            isLoading = true,
            hintsAllowed = currentSettings.hintsEnabled,
            aiMode = currentSettings.gameMode == com.joguecomigo.data.GameMode.AI,
        )
        viewModelScope.launch {
            val generated = withContext(Dispatchers.Default) {
                SudokuGenerator.generate(chosen)
            }
            board = generated
            publishBoard()
            startTimer()
        }
    }

    /** (Re)inicia a partida atual com o mesmo tabuleiro. */
    fun restart() {
        val current = board ?: return
        timerJob?.cancel()
        matchRecorded = false
        // Restaura o estado inicial do puzzle.
        board = SudokuBoard(current.solution.copyOf(), current.puzzle.copyOf())
        _uiState.value = _uiState.value.copy(
            elapsedSeconds = 0,
            mistakes = 0,
            hintsUsed = 0,
            isSolved = false,
            selectedIndex = null,
            assistantMessage = null,
            hintCellIndex = null,
        )
        publishBoard()
        startTimer()
    }

    fun selectCell(index: Int) {
        if (_uiState.value.isSolved) return
        _uiState.value = _uiState.value.copy(selectedIndex = index, hintCellIndex = null)
    }

    /** Insere um número na célula selecionada. */
    fun inputNumber(value: Int) {
        val b = board ?: return
        val index = _uiState.value.selectedIndex ?: return
        if (b.isGiven(index) || _uiState.value.isSolved) return

        b.setValue(index, value)

        // Conta erro quando o valor difere da solução (uma vez por jogada errada).
        var mistakes = _uiState.value.mistakes
        if (value != 0 && value != b.solution[index]) {
            mistakes++
        }
        _uiState.value = _uiState.value.copy(mistakes = mistakes, assistantMessage = null)
        publishBoard()
        checkForWin()
    }

    /** Apaga a célula selecionada. */
    fun eraseCell() {
        val b = board ?: return
        val index = _uiState.value.selectedIndex ?: return
        if (b.isGiven(index)) return
        b.clear(index)
        publishBoard()
    }

    /** Verifica a jogada atual e informa se está correta (regras + solução). */
    fun checkSelectedMove() {
        val b = board ?: return
        val index = _uiState.value.selectedIndex
        if (index == null) {
            setMessage("Selecione uma célula para verificar.")
            return
        }
        val value = b.value(index)
        val message = when {
            value == 0 -> "A célula está vazia."
            value == b.solution[index] -> "Correto! Esse número está no lugar certo."
            else -> "Esse número não está correto. Tente novamente."
        }
        setMessage(message)
    }

    /** Pede uma dica ao assistente (preenche e explica uma célula). */
    fun requestHint() {
        val b = board ?: return
        if (!currentSettings.hintsEnabled) {
            setMessage("As dicas estão desativadas nas configurações.")
            return
        }
        viewModelScope.launch {
            val suggestion = assistant().suggestSudokuMove(b)
            if (suggestion == null) {
                setMessage("Não há mais jogadas para sugerir.")
                return@launch
            }
            // Preenche a célula sugerida com o valor correto.
            b.setValue(suggestion.index, suggestion.value)
            _uiState.value = _uiState.value.copy(
                hintsUsed = _uiState.value.hintsUsed + 1,
                selectedIndex = suggestion.index,
                hintCellIndex = suggestion.index,
                assistantMessage = suggestion.explanation,
            )
            publishBoard()
            checkForWin()
        }
    }

    /** No modo IA, analisa a jogada atual da célula selecionada. */
    fun analyzeSelected() {
        val b = board ?: return
        val index = _uiState.value.selectedIndex ?: run {
            setMessage("Selecione uma célula para analisar.")
            return
        }
        val value = b.value(index)
        if (value == 0) {
            setMessage("Coloque um número antes de pedir a análise.")
            return
        }
        viewModelScope.launch {
            val response = if (b.isCorrect(index)) {
                assistant().analyzeSudokuMove(b, index, value)
            } else {
                assistant().explainSudokuError(b, index)
            }
            setMessage(response.message)
        }
    }

    fun dismissMessage() {
        _uiState.value = _uiState.value.copy(assistantMessage = null)
    }

    // --- Internos ---

    private fun publishBoard() {
        val b = board ?: return
        val conflicts = (0 until SudokuBoard.TOTAL_CELLS).filter { b.hasConflict(it) }.toSet()
        _uiState.value = _uiState.value.copy(
            cells = b.cells.toList(),
            given = (0 until SudokuBoard.TOTAL_CELLS).map { b.isGiven(it) },
            conflicts = conflicts,
            isLoading = false,
        )
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                if (_uiState.value.isSolved) break
                _uiState.value = _uiState.value.copy(
                    elapsedSeconds = _uiState.value.elapsedSeconds + 1
                )
            }
        }
    }

    private fun checkForWin() {
        val b = board ?: return
        if (b.isSolved() && !matchRecorded) {
            matchRecorded = true
            timerJob?.cancel()
            val state = _uiState.value
            val xp = XpCalculator.sudokuXp(
                solved = true,
                difficulty = state.difficulty,
                durationSeconds = state.elapsedSeconds,
                mistakes = state.mistakes,
                hintsUsed = state.hintsUsed,
            )
            _uiState.value = state.copy(isSolved = true)
            viewModelScope.launch {
                progressManager.recordMatch(
                    MatchRecord(
                        game = GameType.SUDOKU,
                        outcome = MatchOutcome.WIN,
                        difficultyOrder = state.difficulty.order,
                        xpGained = xp,
                        durationSeconds = state.elapsedSeconds,
                        mistakes = state.mistakes,
                        hintsUsed = state.hintsUsed,
                        timestamp = System.currentTimeMillis(),
                    )
                )
            }
        }
    }

    private fun setMessage(message: String) {
        _uiState.value = _uiState.value.copy(assistantMessage = message)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
