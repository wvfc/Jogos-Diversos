package com.joguecomigo.ai

import com.joguecomigo.data.GameType
import com.joguecomigo.domain.Difficulty
import com.joguecomigo.domain.checkers.CheckersBoard
import com.joguecomigo.domain.checkers.CheckersMove
import com.joguecomigo.domain.checkers.Side
import com.joguecomigo.domain.sudoku.SudokuBoard

/** Resposta textual do assistente, com um tom positivo/negativo opcional. */
data class AiResponse(
    val message: String,
    val isPositive: Boolean? = null,
)

/** Sugestão de jogada de Sudoku (jogada concreta + explicação didática). */
data class SudokuSuggestion(
    val index: Int,
    val value: Int,
    val explanation: String,
)

/** Sugestão de jogada de Dama (lance concreto + explicação didática). */
data class CheckersSuggestion(
    val move: CheckersMove,
    val explanation: String,
)

/**
 * Contrato do assistente inteligente do app.
 *
 * Atua como tutor: explica e sugere, mas não resolve o jogo inteiro sozinho,
 * a menos que o usuário peça explicitamente uma sugestão.
 *
 * Há duas implementações: [LocalMachineAssistant] (offline) e
 * [ExternalAiAssistant] (usa API externa quando configurada).
 */
interface AiAssistant {

    /** Analisa uma jogada de Sudoku (valor [value] na célula [index]). */
    suspend fun analyzeSudokuMove(board: SudokuBoard, index: Int, value: Int): AiResponse

    /** Explica, de forma simples, por que a célula [index] está incorreta. */
    suspend fun explainSudokuError(board: SudokuBoard, index: Int): AiResponse

    /** Sugere uma próxima jogada de Sudoku com explicação (ou null se completo). */
    suspend fun suggestSudokuMove(board: SudokuBoard): SudokuSuggestion?

    /** Julga uma jogada de Dama feita por [side] (boa, ruim ou arriscada). */
    suspend fun analyzeCheckersMove(board: CheckersBoard, move: CheckersMove, side: Side): AiResponse

    /** Sugere um lance estratégico de Dama para [side] (ou null se não houver). */
    suspend fun suggestCheckersMove(board: CheckersBoard, side: Side, difficulty: Difficulty): CheckersSuggestion?

    /** Explica uma estratégia geral para o jogo informado. */
    suspend fun explainStrategy(game: GameType): AiResponse
}
