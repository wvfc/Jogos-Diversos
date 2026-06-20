package com.joguecomigo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joguecomigo.ai.AiAssistant
import com.joguecomigo.ai.AiAssistantFactory
import com.joguecomigo.data.AppSettings
import com.joguecomigo.data.GameMode
import com.joguecomigo.data.GameType
import com.joguecomigo.data.MatchOutcome
import com.joguecomigo.data.MatchRecord
import com.joguecomigo.data.PlayerProgress
import com.joguecomigo.data.ProgressManager
import com.joguecomigo.data.SettingsManager
import com.joguecomigo.data.XpCalculator
import com.joguecomigo.domain.Difficulty
import com.joguecomigo.domain.checkers.CheckersBoard
import com.joguecomigo.domain.checkers.CheckersEngine
import com.joguecomigo.domain.checkers.CheckersMove
import com.joguecomigo.domain.checkers.Side
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Estado imutável da tela de Dama. */
data class CheckersUiState(
    val squares: List<Int> = emptyList(),
    val selectedIndex: Int? = null,
    val legalTargets: Set<Int> = emptySet(),
    val suggestedFrom: Int? = null,
    val suggestedTo: Int? = null,
    val turn: Side = Side.WHITE,
    val humanSide: Side = Side.WHITE,
    val difficulty: Difficulty = Difficulty.FACIL,
    val whiteCount: Int = 12,
    val blackCount: Int = 12,
    val piecesLost: Int = 0,
    val hintsUsed: Int = 0,
    val elapsedSeconds: Long = 0,
    val isGameOver: Boolean = false,
    val winner: Side? = null,
    val isThinking: Boolean = false,
    val hintsAllowed: Boolean = true,
    val aiMode: Boolean = false,
    val assistantMessage: String? = null,
)

/**
 * ViewModel da partida de Dama. O jogador é sempre as Brancas e a máquina
 * (motor Minimax) joga com as Pretas. Controla seleção, movimentos legais,
 * capturas obrigatórias, turno da máquina, timer e registro de progresso.
 */
class CheckersGameViewModel(
    private val progressManager: ProgressManager,
    private val settingsManager: SettingsManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckersUiState())
    val uiState: StateFlow<CheckersUiState> = _uiState.asStateFlow()

    private val engine = CheckersEngine()
    private var board: CheckersBoard = CheckersBoard.initial()
    private var timerJob: Job? = null
    private var currentSettings: AppSettings = AppSettings()
    private var currentProgress: PlayerProgress = PlayerProgress()
    private var matchRecorded = false

    private val humanSide = Side.WHITE
    private val machineSide = Side.BLACK

    init {
        viewModelScope.launch {
            settingsManager.settings.collect { settings ->
                currentSettings = settings
                _uiState.value = _uiState.value.copy(
                    hintsAllowed = settings.hintsEnabled,
                    aiMode = settings.gameMode == GameMode.AI,
                )
            }
        }
        viewModelScope.launch {
            progressManager.progress.collect { currentProgress = it }
        }
    }

    private fun assistant(): AiAssistant = AiAssistantFactory.create(currentSettings)

    fun suggestedDifficulty(): Difficulty = currentProgress.suggestedDifficulty(GameType.CHECKERS)
    fun unlockedDifficulty(): Difficulty = currentProgress.checkers.unlockedDifficulty

    /** Inicia uma nova partida. [difficulty] nulo usa a dificuldade sugerida. */
    fun newGame(difficulty: Difficulty? = null) {
        val chosen = difficulty ?: suggestedDifficulty()
        timerJob?.cancel()
        matchRecorded = false
        board = CheckersBoard.initial()
        _uiState.value = CheckersUiState(
            difficulty = chosen,
            humanSide = humanSide,
            turn = humanSide,
            hintsAllowed = currentSettings.hintsEnabled,
            aiMode = currentSettings.gameMode == GameMode.AI,
        )
        publishBoard(Side.WHITE)
        startTimer()
    }

    /** Trata o toque em uma casa do tabuleiro. */
    fun onSquareTapped(index: Int) {
        val state = _uiState.value
        if (state.isGameOver || state.turn != humanSide || state.isThinking) return

        val piece = board.pieceAt(index)
        val selected = state.selectedIndex

        // Já há uma peça selecionada: tenta mover para a casa tocada.
        if (selected != null && index in state.legalTargets) {
            val move = humanMovesFrom(selected).firstOrNull { it.to == index } ?: return
            applyHumanMove(move)
            return
        }

        // Seleciona uma peça do jogador que possua movimentos legais.
        val isHumanPiece = (humanSide == Side.WHITE && piece > 0) || (humanSide == Side.BLACK && piece < 0)
        if (isHumanPiece) {
            val targets = humanMovesFrom(index).map { it.to }.toSet()
            _uiState.value = state.copy(
                selectedIndex = if (targets.isEmpty()) null else index,
                legalTargets = targets,
                suggestedFrom = null,
                suggestedTo = null,
            )
        } else {
            _uiState.value = state.copy(selectedIndex = null, legalTargets = emptySet())
        }
    }

    private fun humanMovesFrom(index: Int): List<CheckersMove> =
        board.legalMoves(humanSide).filter { it.from == index }

    private fun applyHumanMove(move: CheckersMove) {
        val before = board
        board = board.applyMove(move)
        _uiState.value = _uiState.value.copy(
            selectedIndex = null,
            legalTargets = emptySet(),
            assistantMessage = null,
        )
        publishBoard(machineSide)

        // No modo IA, comenta a jogada do jogador.
        if (currentSettings.gameMode == GameMode.AI) {
            viewModelScope.launch {
                val response = assistant().analyzeCheckersMove(before, move, humanSide)
                setMessage(response.message)
            }
        }

        if (checkGameOver()) return

        // Vez da máquina (motor local), em thread de background.
        _uiState.value = _uiState.value.copy(isThinking = true)
        viewModelScope.launch {
            delay(350) // pequena pausa para naturalidade
            val machineMove = withContext(Dispatchers.Default) {
                engine.bestMove(board, machineSide, _uiState.value.difficulty)
            }
            if (machineMove != null) {
                board = board.applyMove(machineMove)
            }
            _uiState.value = _uiState.value.copy(isThinking = false)
            publishBoard(humanSide)
            checkGameOver()
        }
    }

    /** Pede uma sugestão de jogada ao assistente. */
    fun requestHint() {
        val state = _uiState.value
        if (state.isGameOver || state.turn != humanSide || state.isThinking) return
        if (!currentSettings.hintsEnabled) {
            setMessage("As dicas estão desativadas nas configurações.")
            return
        }
        viewModelScope.launch {
            val suggestion = assistant().suggestCheckersMove(board, humanSide, state.difficulty)
            if (suggestion == null) {
                setMessage("Não há jogadas disponíveis.")
                return@launch
            }
            _uiState.value = _uiState.value.copy(
                hintsUsed = _uiState.value.hintsUsed + 1,
                suggestedFrom = suggestion.move.from,
                suggestedTo = suggestion.move.to,
                selectedIndex = suggestion.move.from,
                legalTargets = humanMovesFrom(suggestion.move.from).map { it.to }.toSet(),
                assistantMessage = suggestion.explanation,
            )
        }
    }

    fun dismissMessage() {
        _uiState.value = _uiState.value.copy(assistantMessage = null)
    }

    // --- Internos ---

    private fun publishBoard(turn: Side) {
        _uiState.value = _uiState.value.copy(
            squares = board.squares.toList(),
            turn = turn,
            whiteCount = board.pieceCount(Side.WHITE),
            blackCount = board.pieceCount(Side.BLACK),
            piecesLost = 12 - board.pieceCount(humanSide),
        )
    }

    /** Verifica fim de jogo; registra o resultado uma única vez. */
    private fun checkGameOver(): Boolean {
        val whiteHasPieces = board.pieceCount(Side.WHITE) > 0
        val blackHasPieces = board.pieceCount(Side.BLACK) > 0
        val whiteMoves = board.legalMoves(Side.WHITE)
        val blackMoves = board.legalMoves(Side.BLACK)

        val winner: Side? = when {
            !blackHasPieces || blackMoves.isEmpty() -> Side.WHITE
            !whiteHasPieces || whiteMoves.isEmpty() -> Side.BLACK
            else -> null
        }

        if (winner != null) {
            timerJob?.cancel()
            _uiState.value = _uiState.value.copy(isGameOver = true, winner = winner)
            recordResult(winner)
            return true
        }
        return false
    }

    private fun recordResult(winner: Side) {
        if (matchRecorded) return
        matchRecorded = true
        val state = _uiState.value
        val outcome = if (winner == humanSide) MatchOutcome.WIN else MatchOutcome.LOSS
        val xp = XpCalculator.checkersXp(
            outcome = outcome,
            difficulty = state.difficulty,
            durationSeconds = state.elapsedSeconds,
            piecesLost = state.piecesLost,
            hintsUsed = state.hintsUsed,
        )
        viewModelScope.launch {
            progressManager.recordMatch(
                MatchRecord(
                    game = GameType.CHECKERS,
                    outcome = outcome,
                    difficultyOrder = state.difficulty.order,
                    xpGained = xp,
                    durationSeconds = state.elapsedSeconds,
                    mistakes = state.piecesLost,
                    hintsUsed = state.hintsUsed,
                    timestamp = System.currentTimeMillis(),
                )
            )
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                if (_uiState.value.isGameOver) break
                _uiState.value = _uiState.value.copy(
                    elapsedSeconds = _uiState.value.elapsedSeconds + 1
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
