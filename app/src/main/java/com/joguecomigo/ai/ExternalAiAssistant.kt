package com.joguecomigo.ai

import com.joguecomigo.data.GameType
import com.joguecomigo.domain.Difficulty
import com.joguecomigo.domain.checkers.CheckersBoard
import com.joguecomigo.domain.checkers.CheckersMove
import com.joguecomigo.domain.checkers.Side
import com.joguecomigo.domain.sudoku.SudokuBoard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

/**
 * Assistente que usa uma API externa de IA (compatível com o formato
 * "chat completions" da OpenAI) **somente** quando o usuário configura uma
 * chave de API válida.
 *
 * Pontos importantes:
 * - A chave NUNCA fica fixa no código; vem das configurações locais.
 * - Sem chave, sem Internet ou em caso de erro, recai automaticamente no
 *   [fallback] (assistente local), garantindo funcionamento offline.
 * - As jogadas concretas (sugestões) são calculadas localmente para serem
 *   sempre válidas; a IA externa apenas enriquece a explicação didática.
 */
class ExternalAiAssistant(
    private val apiKey: String,
    private val apiUrl: String,
    private val model: String = "gpt-4o-mini",
    private val fallback: AiAssistant = LocalMachineAssistant(),
) : AiAssistant {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class ChatMessage(val role: String, val content: String)

    @Serializable
    private data class ChatRequest(
        val model: String,
        val messages: List<ChatMessage>,
        val temperature: Double = 0.4,
    )

    @Serializable
    private data class ChatChoice(val message: ChatMessage)

    @Serializable
    private data class ChatResponse(val choices: List<ChatChoice> = emptyList())

    private val systemPrompt =
        "Você é um tutor gentil de jogos de tabuleiro (Sudoku e Dama). " +
            "Responda sempre em português do Brasil, de forma curta, simples e didática. " +
            "Explique o raciocínio, mas não resolva o jogo inteiro a menos que peçam."

    /**
     * Faz a chamada à API. Retorna null em qualquer falha (rede, chave, etc.),
     * sinalizando ao chamador para usar o fallback local.
     */
    private suspend fun chat(userPrompt: String): String? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null
        runCatching {
            val connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 20_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $apiKey")
            }

            val body = json.encodeToString(
                ChatRequest(
                    model = model,
                    messages = listOf(
                        ChatMessage("system", systemPrompt),
                        ChatMessage("user", userPrompt),
                    ),
                )
            )
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            if (connection.responseCode !in 200..299) {
                connection.disconnect()
                return@runCatching null
            }

            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val parsed = json.decodeFromString<ChatResponse>(responseText)
            parsed.choices.firstOrNull()?.message?.content?.trim()
        }.getOrNull()
    }

    override suspend fun analyzeSudokuMove(board: SudokuBoard, index: Int, value: Int): AiResponse {
        val row = SudokuBoard.rowOf(index) + 1
        val col = SudokuBoard.colOf(index) + 1
        val correct = board.solution[index]
        val isCorrect = value == correct
        val prompt = "No Sudoku, o jogador colocou o número $value na linha $row, coluna $col. " +
            (if (isCorrect) "Essa jogada está correta. " else "Essa jogada está incorreta (o valor certo é $correct). ") +
            "Explique em uma ou duas frases por quê, de forma simples."
        val text = chat(prompt) ?: return fallback.analyzeSudokuMove(board, index, value)
        return AiResponse(text, isPositive = isCorrect)
    }

    override suspend fun explainSudokuError(board: SudokuBoard, index: Int): AiResponse {
        val value = board.value(index)
        if (value == 0 || board.isCorrect(index)) {
            return fallback.explainSudokuError(board, index)
        }
        val row = SudokuBoard.rowOf(index) + 1
        val col = SudokuBoard.colOf(index) + 1
        val prompt = "No Sudoku, a célula da linha $row, coluna $col tem o número $value, que está " +
            "errado. O valor correto é ${board.solution[index]}. Explique de forma simples por que " +
            "o $value não pode ficar aí (linha, coluna ou bloco)."
        val text = chat(prompt) ?: return fallback.explainSudokuError(board, index)
        return AiResponse(text, isPositive = false)
    }

    override suspend fun suggestSudokuMove(board: SudokuBoard): SudokuSuggestion? {
        // A jogada concreta vem do assistente local (sempre válida).
        val local = fallback.suggestSudokuMove(board) ?: return null
        val row = SudokuBoard.rowOf(local.index) + 1
        val col = SudokuBoard.colOf(local.index) + 1
        val prompt = "No Sudoku, sugira ao jogador colocar o número ${local.value} na linha $row, " +
            "coluna $col. Explique o raciocínio em uma frase simples, como um tutor."
        val text = chat(prompt) ?: return local
        return local.copy(explanation = text)
    }

    override suspend fun analyzeCheckersMove(board: CheckersBoard, move: CheckersMove, side: Side): AiResponse {
        val local = fallback.analyzeCheckersMove(board, move, side)
        val prompt = "Na Dama, o jogador (${side.label}) fez um lance que captura " +
            "${move.captured.size} peça(s). Avalie se foi boa, ruim ou arriscada, em uma frase. " +
            "Contexto técnico: ${local.message}"
        val text = chat(prompt) ?: return local
        return AiResponse(text, isPositive = local.isPositive)
    }

    override suspend fun suggestCheckersMove(
        board: CheckersBoard,
        side: Side,
        difficulty: Difficulty,
    ): CheckersSuggestion? {
        // O lance concreto vem do motor local (sempre legal).
        val local = fallback.suggestCheckersMove(board, side, difficulty) ?: return null
        val prompt = "Na Dama, sugira ao jogador um lance que " +
            (if (local.move.isCapture) "captura ${local.move.captured.size} peça(s)." else "avança na diagonal.") +
            " Explique a vantagem estratégica em uma frase simples."
        val text = chat(prompt) ?: return local
        return local.copy(explanation = text)
    }

    override suspend fun explainStrategy(game: GameType): AiResponse {
        val prompt = "Dê uma dica estratégica curta e simples para o jogo de ${game.label}."
        val text = chat(prompt) ?: return fallback.explainStrategy(game)
        return AiResponse(text)
    }
}
