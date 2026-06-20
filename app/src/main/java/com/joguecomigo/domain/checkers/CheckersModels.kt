package com.joguecomigo.domain.checkers

/**
 * Modelos básicos do jogo de Dama.
 *
 * Regras adotadas (estilo damas clássicas / inglesas), bem definidas:
 * - Peças simples (peões) movem-se uma casa na diagonal para frente.
 * - Capturas são obrigatórias quando existem.
 * - Capturas múltiplas (em sequência) são executadas no mesmo lance.
 * - Um peão vira "dama" ao alcançar a última linha do lado adversário.
 * - A dama move-se e captura uma casa em qualquer das quatro diagonais.
 * - Vence quem capturar todas as peças do adversário ou deixá-lo sem
 *   movimentos válidos.
 *
 * O tabuleiro é 8x8 e apenas as casas escuras (onde (linha+coluna) é ímpar)
 * são utilizadas. As peças são armazenadas em um [IntArray] de 64 posições.
 */
object Cell {
    const val EMPTY = 0
    const val WHITE_MAN = 1
    const val WHITE_KING = 2
    const val BLACK_MAN = -1
    const val BLACK_KING = -2

    fun isWhite(piece: Int) = piece > 0
    fun isBlack(piece: Int) = piece < 0
    fun isKing(piece: Int) = piece == WHITE_KING || piece == BLACK_KING
    fun isEmpty(piece: Int) = piece == EMPTY
}

/**
 * Representa um lance de Dama.
 *
 * @param from Índice de origem (0..63).
 * @param to Índice de destino final.
 * @param captured Índices das peças capturadas (vazio em movimento simples).
 * @param path Sequência de casas visitadas em capturas múltiplas (sem a origem).
 */
data class CheckersMove(
    val from: Int,
    val to: Int,
    val captured: List<Int> = emptyList(),
    val path: List<Int> = emptyList(),
) {
    val isCapture: Boolean get() = captured.isNotEmpty()
}

/** Identifica de quem é a vez / qual lado uma peça pertence. */
enum class Side(val label: String) {
    WHITE("Brancas"),
    BLACK("Pretas");

    fun opponent(): Side = if (this == WHITE) BLACK else WHITE
}
