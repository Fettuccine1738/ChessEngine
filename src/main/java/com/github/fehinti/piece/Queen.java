package com.github.fehinti.piece;

import java.util.Collection;

import com.github.fehinti.board.Board;
import com.github.fehinti.board.PieceType;
import static com.github.fehinti.board.BoardUtilities.*;
import static com.github.fehinti.board.PieceType.BLACK_QUEEN;
import static com.github.fehinti.board.PieceType.WHITE_QUEEN;


public class Queen {

    // queen coordinates on board 64
    public final static byte[] VECTOR_COORDINATE = { 15, 17, 10, 6, -10, -17, -15, -6};
    // bishop coordinates on board 10 x 12
    public final static byte[] OFFSET_VECTOR_COORDINATE = { -11, -10, -9, -1, 1,  9, 10, 11 };


    // iterate through disjoint list of board and generat possible moves
    public static Collection<Integer> possibleMoves(Board board,  boolean sideToPlay) {
        if (board == null) throw new IllegalArgumentException("possible Knigt moves invoked with null");
        int floor   = getPieceListFloor(WHITE_QUEEN);
        int ceiling = getPieceListCeiling(BLACK_QUEEN);
        if (sideToPlay == WHITE) {
            return generatePseudoLegal(board, sideToPlay, floor, ceiling, WHITE_QUEEN);
        }
        else {
            return generatePseudoLegal(board, sideToPlay, floor, ceiling, BLACK_QUEEN);
        }
    }

    private static Collection<Integer> generatePseudoLegal(Board board, boolean sideToPlay, int floor, int ceiling, PieceType piece) {
        return PieceMove.generatePseudoLegal(board, sideToPlay, floor, ceiling, piece);
    }
}
