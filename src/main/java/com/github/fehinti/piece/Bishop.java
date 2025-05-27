package com.github.fehinti.piece;

import java.util.List;

import com.github.fehinti.board.Board;
import com.github.fehinti.board.PieceType;
import com.github.fehinti.piece.PieceMove;
import static com.github.fehinti.board.BoardUtilities.*;
import static com.github.fehinti.board.PieceType.BLACK_BISHOP;
import static com.github.fehinti.board.PieceType.WHITE_BISHOP;

public class Bishop {

    // iterate through disjoint list of board and generat possible moves
    public static List<Integer> possibleMoves(Board board,  boolean sideToPlay) {
        if (board == null) throw new IllegalArgumentException("possible Knigt moves invoked with null");
        int floor   = getPieceListFloor(WHITE_BISHOP);
        int ceiling = getPieceListCeiling(BLACK_BISHOP);
        if (sideToPlay == WHITE) {
            return generatePseudoLegal(board, sideToPlay, floor, ceiling, WHITE_BISHOP);
        }
        else {
            return generatePseudoLegal(board, sideToPlay, floor, ceiling, BLACK_BISHOP);
        }
    }

    private static List<Integer> generatePseudoLegal(Board board, boolean sideToPlay, int floor, int ceiling, PieceType piece) {
        return PieceMove.generatePseudoLegal(board, sideToPlay, floor, ceiling, piece);
    }

}
