package com.joguecomigo.ai

import com.joguecomigo.data.GameType
import com.joguecomigo.domain.Difficulty
import com.joguecomigo.domain.checkers.CheckersBoard
import com.joguecomigo.domain.checkers.CheckersEngine
import com.joguecomigo.domain.checkers.CheckersMove
import com.joguecomigo.domain.checkers.Side
import com.joguecomigo.domain.sudoku.SudokuBoard

/**
 * Assistente que funciona 100% offline usando regras e algoritmos locais.
 *
 * É o modo "máquina": valida jogadas, explica erros com base nas regras e
 * na solução conhecida, e sugere jogadas usando o motor de Dama (Minimax).
 */
class LocalMachineAssistant(
    private val engine: CheckersEngine = CheckersEngine(),
) : AiAssistant {

    override suspend fun analyzeSudokuMove(board: SudokuBoard, index: Int, value: Int): AiResponse {
        val row = SudokuBoard.rowOf(index) + 1
        val col = SudokuBoard.colOf(index) + 1
        val correct = board.solution[index]

        return when {
            value == correct -> AiResponse(
                "Jogada correta! O número $value é o valor certo para a linha $row, " +
                    "coluna $col, pois é o único que respeita a linha, a coluna e o bloco.",
                isPositive = true,
            )
            else -> {
                val reason = ruleConflictReason(board, index, value)
                AiResponse(
                    "O número $value não cabe na linha $row, coluna $col. $reason " +
                        "Tente outro valor que não se repita na linha, coluna ou bloco.",
                    isPositive = false,
                )
            }
        }
    }

    override suspend fun explainSudokuError(board: SudokuBoard, index: Int): AiResponse {
        val value = board.value(index)
        val row = SudokuBoard.rowOf(index) + 1
        val col = SudokuBoard.colOf(index) + 1
        if (value == 0) {
            return AiResponse("Esta célula está vazia, não há erro para explicar.", isPositive = null)
        }
        if (board.isCorrect(index)) {
            return AiResponse("Na verdade o número $value está correto aqui!", isPositive = true)
        }
        val reason = ruleConflictReason(board, index, value)
        return AiResponse(
            "O valor $value na linha $row, coluna $col está incorreto. $reason " +
                "O valor certo desta célula é ${board.solution[index]}.",
            isPositive = false,
        )
    }

    override suspend fun suggestSudokuMove(board: SudokuBoard): SudokuSuggestion? {
        // Procura primeiro uma "única possibilidade" (naked single) para
        // dar uma dica didática; caso contrário, usa a solução conhecida.
        for (index in 0 until SudokuBoard.TOTAL_CELLS) {
            if (board.value(index) != 0) continue
            val candidates = (1..9).filter { candidate ->
                isCandidateValid(board, index, candidate)
            }
            val row = SudokuBoard.rowOf(index) + 1
            val col = SudokuBoard.colOf(index) + 1
            if (candidates.size == 1) {
                return SudokuSuggestion(
                    index = index,
                    value = candidates.first(),
                    explanation = "Na linha $row, coluna $col só é possível o número " +
                        "${candidates.first()}, pois todos os outros já aparecem na linha, " +
                        "coluna ou bloco.",
                )
            }
        }
        // Sem "única possibilidade" óbvia: sugere uma célula vazia pela solução.
        val emptyIndex = (0 until SudokuBoard.TOTAL_CELLS).firstOrNull { board.value(it) == 0 }
            ?: return null
        val row = SudokuBoard.rowOf(emptyIndex) + 1
        val col = SudokuBoard.colOf(emptyIndex) + 1
        return SudokuSuggestion(
            index = emptyIndex,
            value = board.solution[emptyIndex],
            explanation = "Uma boa próxima jogada é colocar ${board.solution[emptyIndex]} na " +
                "linha $row, coluna $col.",
        )
    }

    override suspend fun analyzeCheckersMove(board: CheckersBoard, move: CheckersMove, side: Side): AiResponse {
        // Compara a avaliação antes e depois e verifica risco de captura imediata.
        val before = engine.evaluate(board, side, Difficulty.DIFICIL)
        val after = engine.evaluate(board.applyMove(move), side, Difficulty.DIFICIL)
        val delta = after - before

        // Verifica se o oponente pode capturar logo em seguida.
        val opponentMoves = board.applyMove(move).legalMoves(side.opponent())
        val exposesCapture = opponentMoves.any { it.isCapture }

        return when {
            move.isCapture && !exposesCapture -> AiResponse(
                "Ótima jogada! Você captura ${move.captured.size} peça(s) sem se expor.",
                isPositive = true,
            )
            exposesCapture && delta <= 0 -> AiResponse(
                "Jogada arriscada: após esse lance o adversário pode capturar uma peça sua. " +
                    "Avalie proteger suas peças.",
                isPositive = false,
            )
            delta > 0 -> AiResponse(
                "Boa jogada, ela melhora sua posição no tabuleiro.",
                isPositive = true,
            )
            else -> AiResponse(
                "Jogada neutra. Procure capturas ou avançar peças para virarem damas.",
                isPositive = null,
            )
        }
    }

    override suspend fun suggestCheckersMove(
        board: CheckersBoard,
        side: Side,
        difficulty: Difficulty,
    ): CheckersSuggestion? {
        val move = engine.bestMove(board, side, difficulty) ?: return null
        val explanation = when {
            move.isCapture -> "Sugiro capturar ${move.captured.size} peça(s) com este lance — " +
                "capturas são obrigatórias e ganham vantagem material."
            else -> "Sugiro este avanço diagonal para melhorar sua posição e proteger suas peças."
        }
        return CheckersSuggestion(move, explanation)
    }

    override suspend fun explainStrategy(game: GameType): AiResponse = when (game) {
        GameType.SUDOKU -> AiResponse(
            "Dica de Sudoku: comece pelas linhas, colunas e blocos que já têm mais números. " +
                "Procure células onde só um valor é possível. Evite chutes — use a lógica.",
        )
        GameType.CHECKERS -> AiResponse(
            "Dica de Dama: controle o centro, mantenha suas peças protegidas em duplas, " +
                "force trocas quando estiver com vantagem e tente transformar peões em damas.",
        )
    }

    // --- Funções auxiliares ---

    /** Verifica se um candidato é válido nas regras atuais do tabuleiro. */
    private fun isCandidateValid(board: SudokuBoard, index: Int, candidate: Int): Boolean {
        val row = index / 9
        val col = index % 9
        for (i in 0 until 9) {
            if (board.value(row * 9 + i) == candidate) return false
            if (board.value(i * 9 + col) == candidate) return false
        }
        val boxRow = (row / 3) * 3
        val boxCol = (col / 3) * 3
        for (r in boxRow until boxRow + 3) {
            for (c in boxCol until boxCol + 3) {
                if (board.value(r * 9 + c) == candidate) return false
            }
        }
        return true
    }

    /** Descreve onde o valor entra em conflito (linha, coluna ou bloco). */
    private fun ruleConflictReason(board: SudokuBoard, index: Int, value: Int): String {
        val row = index / 9
        val col = index % 9

        for (i in 0 until 9) {
            if (i != col && board.value(row * 9 + i) == value) {
                return "Ele já aparece nesta linha."
            }
        }
        for (i in 0 until 9) {
            if (i != row && board.value(i * 9 + col) == value) {
                return "Ele já aparece nesta coluna."
            }
        }
        val boxRow = (row / 3) * 3
        val boxCol = (col / 3) * 3
        for (r in boxRow until boxRow + 3) {
            for (c in boxCol until boxCol + 3) {
                val idx = r * 9 + c
                if (idx != index && board.value(idx) == value) {
                    return "Ele já aparece neste bloco 3x3."
                }
            }
        }
        return "Ele não corresponde à solução correta desta célula."
    }
}
