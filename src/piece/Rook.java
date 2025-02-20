package piece;

import board.Board;
import board.PieceType;

import java.util.Collection;

import static board.BoardUtilities.*;
import static board.PieceType.*;

public class Rook {

    // rook coordinates on board 64
    public final static byte[] VECTOR_COORDINATE = { 15, 17, 10, 6, -10, -17, -15, -6};
    // rook coordinates on board 10 x 12
    public final static byte[] OFFSET_VECTOR_COORDINATE = { -10,  -1,  1, 10, 0,  0,  0,  0 };


    // iterate through disjoint list of board and generat possible moves
    public static Collection<Integer> possibleMoves(Board board,  boolean sideToPlay) {
        if (board == null) throw new IllegalArgumentException("possible Knigt moves invoked with null");
        int floor   = getPieceListFloor(WHITE_ROOK);
        int ceiling = getPieceListCeiling(BLACK_ROOK);
        if (sideToPlay == WHITE) {
            return generatePseudoLegal(board, sideToPlay, floor, ceiling, WHITE_ROOK);
        }
        else {
            return generatePseudoLegal(board, sideToPlay, floor, ceiling, BLACK_ROOK);
        }
    }

    private static Collection<Integer> generatePseudoLegal(Board board, boolean sideToPlay, int floor, int ceiling, PieceType piece) {
        return PieceMove.generatePseudoLegal(board, sideToPlay, floor, ceiling, piece);
    }
}
