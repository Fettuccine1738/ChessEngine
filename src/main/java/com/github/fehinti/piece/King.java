package com.github.fehinti.piece;

import java.util.List;

import com.github.fehinti.board.Board;
import com.github.fehinti.piece.Piece;
import static com.github.fehinti.board.BoardUtilities.*;
import static com.github.fehinti.piece.Piece.BLACK_KING;
import static com.github.fehinti.piece.Piece.WHITE_KING;

public class King {

    public static List<Integer> possibleMoves(Board board,  boolean sideToPlay) {
        if (board == null) throw new IllegalArgumentException("possible King moves invoked with null");
        int floor   = getPieceListFloor(WHITE_KING);
        int ceiling = getPieceListCeiling(BLACK_KING);
        if (sideToPlay == WHITE) {
            return generatePseudoLegal(board, sideToPlay, floor, ceiling, WHITE_KING);
        }
        else {
            return generatePseudoLegal(board, sideToPlay, floor, ceiling, BLACK_KING);
        }
    }

    private static List<Integer> generatePseudoLegal(Board board, boolean sideToPlay, int floor, int ceiling, Piece piece) {
        return PieceMove.generatePseudoLegal(board);
    }


}
