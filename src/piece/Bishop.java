package piece;

import board.Board;
import board.Move;
import board.PieceType;

import java.util.Collection;
import java.util.List;

import static board.BoardUtilities.*;
import static board.PieceType.BLACK_BISHOP;
import static board.PieceType.WHITE_BISHOP;

public class Bishop {

    public static final boolean  IS_SLIDING = true;

    // bishop coordinates on board 64
    public final static byte[] VECTOR_COORDINATE        = {  -9,  -7,  7,  9, 0, 0,  0,  0 };
    // bishop coordinates on board 10 x 12
    public final static byte[] OFFSET_VECTOR_COORDINATE = { -11,  -9,  9, 11, 0,  0,  0,  0 };


    // iterate through disjoint list of board and generat possible moves
    public static Collection<Integer> possibleMoves(Board board,  boolean sideToPlay) {
        if (board == null) throw new IllegalArgumentException("possible Knigt moves invoked with null");
        int floor   = getPieceListFloor(WHITE_BISHOP);
        int ceiling = getPieceListSize(BLACK_BISHOP);
        if (sideToPlay == WHITE) {
            return generatePseudoLegal(board, sideToPlay, floor, ceiling, WHITE_BISHOP);
        }
        else {
            return generatePseudoLegal(board, sideToPlay, floor, ceiling, BLACK_BISHOP);
        }
    }

    private static Collection<Integer> generatePseudoLegal(Board board, boolean sideToPlay, int floor, int ceiling, PieceType piece) {
        return PieceMove.generatePseudoLegal(board, sideToPlay, floor, ceiling, piece);
    }

}
