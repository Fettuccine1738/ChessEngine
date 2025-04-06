package com.github.fehinti.piece;

import java.util.Collection;

import com.github.fehinti.board.Board;
import com.github.fehinti.board.PieceType;
import static com.github.fehinti.board.BoardUtilities.*;
import static com.github.fehinti.board.PieceType.BLACK_PAWN;
import static com.github.fehinti.board.PieceType.WHITE_PAWN;

public class Pawn {

    public static Collection<Integer> possibleMoves(Board board,  boolean sideToPlay) {
        if (board == null) throw new IllegalArgumentException("possible King moves invoked with null");
        int floor   = getPieceListFloor(WHITE_PAWN);
        int ceiling = getPieceListCeiling(BLACK_PAWN);
        if (sideToPlay == WHITE) {
            return generatePseudoLegal(board, sideToPlay, floor, ceiling, WHITE_PAWN);
        }
        else {
            return generatePseudoLegal(board, sideToPlay, floor, ceiling, BLACK_PAWN);
        }
    }

    // legal moves ?? play move then look at !side (opponent), if current square is in move list of
    // !side remove move.

    // castling ??

    // direct testing
    private static Collection<Integer> generatePseudoLegal(Board board, boolean sideToPlay, int floor, int ceiling, PieceType piece) {
        return PieceMove.generatePseudoLegal(board, sideToPlay, floor, ceiling, piece);
    }

}
