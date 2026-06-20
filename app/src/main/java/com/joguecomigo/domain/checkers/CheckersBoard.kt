package com.joguecomigo.domain.checkers

/**
 * Estado do tabuleiro de Dama (8x8) e regras de geração de movimentos.
 *
 * Convenção de orientação:
 * - Brancas ocupam as linhas de baixo (5, 6, 7) e avançam para cima
 *   (linha decrescente).
 * - Pretas ocupam as linhas de cima (0, 1, 2) e avançam para baixo
 *   (linha crescente).
 */
class CheckersBoard private constructor(val squares: IntArray) {

    fun pieceAt(index: Int): Int = squares[index]

    /** Cria uma cópia independente do tabuleiro. */
    fun copy(): CheckersBoard = CheckersBoard(squares.copyOf())

    /** Conta as peças de um lado (peão = 1 ponto, dama = ~1.6 no engine). */
    fun pieceCount(side: Side): Int = squares.count {
        if (side == Side.WHITE) Cell.isWhite(it) else Cell.isBlack(it)
    }

    /** Aplica um lance retornando um NOVO tabuleiro (imutável para o engine). */
    fun applyMove(move: CheckersMove): CheckersBoard {
        val next = squares.copyOf()
        var piece = next[move.from]
        next[move.from] = Cell.EMPTY
        for (captured in move.captured) {
            next[captured] = Cell.EMPTY
        }
        // Promoção a dama ao alcançar a última linha do adversário.
        val destRow = move.to / 8
        if (piece == Cell.WHITE_MAN && destRow == 0) piece = Cell.WHITE_KING
        if (piece == Cell.BLACK_MAN && destRow == 7) piece = Cell.BLACK_KING
        next[move.to] = piece
        return CheckersBoard(next)
    }

    /**
     * Retorna todos os lances legais para [side].
     * Se existir qualquer captura, apenas capturas são retornadas
     * (captura obrigatória).
     */
    fun legalMoves(side: Side): List<CheckersMove> {
        val captures = mutableListOf<CheckersMove>()
        val simples = mutableListOf<CheckersMove>()

        for (index in 0 until 64) {
            val piece = squares[index]
            if (piece == Cell.EMPTY) continue
            if (side == Side.WHITE && !Cell.isWhite(piece)) continue
            if (side == Side.BLACK && !Cell.isBlack(piece)) continue

            collectCaptures(index, piece, index, emptyList(), emptyList(), squares.copyOf(), captures)
            if (captures.isEmpty()) {
                collectSimpleMoves(index, piece, simples)
            }
        }

        return if (captures.isNotEmpty()) captures else simples
    }

    /** Movimentos simples (sem captura) de uma peça. */
    private fun collectSimpleMoves(origin: Int, piece: Int, out: MutableList<CheckersMove>) {
        val row = origin / 8
        val col = origin % 8
        for ((dr, dc) in movementDirections(piece)) {
            val nr = row + dr
            val nc = col + dc
            if (nr in 0..7 && nc in 0..7) {
                val target = nr * 8 + nc
                if (squares[target] == Cell.EMPTY) {
                    out.add(CheckersMove(from = origin, to = target))
                }
            }
        }
    }

    /**
     * Busca recursiva por sequências de captura (capturas múltiplas).
     * Trabalha sobre uma cópia mutável [work] do tabuleiro.
     */
    private fun collectCaptures(
        origin: Int,
        piece: Int,
        current: Int,
        captured: List<Int>,
        path: List<Int>,
        work: IntArray,
        out: MutableList<CheckersMove>,
    ) {
        val row = current / 8
        val col = current % 8
        var extended = false

        for ((dr, dc) in captureDirections(piece)) {
            val midR = row + dr
            val midC = col + dc
            val landR = row + 2 * dr
            val landC = col + 2 * dc
            if (landR !in 0..7 || landC !in 0..7) continue

            val midIndex = midR * 8 + midC
            val landIndex = landR * 8 + landC
            val midPiece = work[midIndex]

            val isEnemy = if (Cell.isWhite(piece)) Cell.isBlack(midPiece) else Cell.isWhite(midPiece)
            if (isEnemy && work[landIndex] == Cell.EMPTY && midIndex !in captured) {
                // Executa a captura na cópia e continua a sequência.
                val next = work.copyOf()
                next[current] = Cell.EMPTY
                next[midIndex] = Cell.EMPTY
                next[landIndex] = piece

                extended = true
                collectCaptures(
                    origin = origin,
                    piece = piece,
                    current = landIndex,
                    captured = captured + midIndex,
                    path = path + landIndex,
                    work = next,
                    out = out,
                )
            }
        }

        // Sem mais capturas possíveis e já houve ao menos uma: lance completo.
        if (!extended && captured.isNotEmpty()) {
            out.add(CheckersMove(from = origin, to = current, captured = captured, path = path))
        }
    }

    /** Direções de movimento simples conforme o tipo de peça. */
    private fun movementDirections(piece: Int): List<Pair<Int, Int>> = when (piece) {
        Cell.WHITE_MAN -> listOf(-1 to -1, -1 to 1)               // brancas sobem
        Cell.BLACK_MAN -> listOf(1 to -1, 1 to 1)                 // pretas descem
        else -> KING_DIRECTIONS                                    // damas: 4 diagonais
    }

    /** Direções de captura. Peões capturam apenas para frente; damas em todas. */
    private fun captureDirections(piece: Int): List<Pair<Int, Int>> = when (piece) {
        Cell.WHITE_MAN -> listOf(-1 to -1, -1 to 1)
        Cell.BLACK_MAN -> listOf(1 to -1, 1 to 1)
        else -> KING_DIRECTIONS
    }

    companion object {
        private val KING_DIRECTIONS = listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)

        /** Casa jogável (escura) quando (linha + coluna) é ímpar. */
        fun isPlayable(index: Int): Boolean {
            val row = index / 8
            val col = index % 8
            return (row + col) % 2 == 1
        }

        /** Cria o tabuleiro na posição inicial padrão (12 peças por lado). */
        fun initial(): CheckersBoard {
            val squares = IntArray(64) { Cell.EMPTY }
            for (index in 0 until 64) {
                if (!isPlayable(index)) continue
                val row = index / 8
                when (row) {
                    0, 1, 2 -> squares[index] = Cell.BLACK_MAN
                    5, 6, 7 -> squares[index] = Cell.WHITE_MAN
                }
            }
            return CheckersBoard(squares)
        }

        /** Constrói um tabuleiro a partir de um array existente (cópia defensiva). */
        fun from(squares: IntArray): CheckersBoard = CheckersBoard(squares.copyOf())
    }
}
