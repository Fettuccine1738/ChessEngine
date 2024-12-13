package piece;

import board.Board;
import board.Move;
import board.PieceType;

import java.util.Collection;
import java.util.List;

import static board.BoardUtilities.*;
import static board.PieceType.*;

public class Rook implements Piece{

    // rook coordinates on board 64
    public final static byte[] VECTOR_COORDINATE = { 15, 17, 10, 6, -10, -17, -15, -6};
    // rook coordinates on board 10 x 12
    public final static byte[] OFFSET_VECTOR_COORDINATE = { -10,  -1,  1, 10, 0,  0,  0,  0 };

    @Override
    public Collection<Move> possibleMoves(int file, int rank, Board p) {
        return List.of();
    }


    // iterate through disjoint list of board and generat possible moves
    public static Collection<Move> possibleMoves(Board board,  boolean sideToPlay) {
        if (board == null) throw new IllegalArgumentException("possible Knigt moves invoked with null");
        int floor   = getPieceListFloor(WHITE_ROOK);
        int ceiling = getPieceListSize(BLACK_ROOK);
        if (sideToPlay == WHITE) {
            return generatePseudoLegal(board, sideToPlay, floor, ceiling, WHITE_ROOK);
        }
        else {
            return generatePseudoLegal(board, sideToPlay, floor, ceiling, BLACK_ROOK);
        }
    }

    private static Collection<Move> generatePseudoLegal(Board board, boolean sideToPlay, int floor, int ceiling, PieceType piece) {
        return PieceMove.generatePseudoLegal(board, sideToPlay, floor, ceiling, piece);
    }
}
