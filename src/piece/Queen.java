package piece;

import board.Board;
import board.Move;
import board.PieceType;

import java.util.Collection;
import java.util.List;

import static board.BoardUtilities.*;
import static board.PieceType.BLACK_QUEEN;
import static board.PieceType.WHITE_QUEEN;

public class Queen implements Piece {

    // queen coordinates on board 64
    public final static byte[] VECTOR_COORDINATE = { 15, 17, 10, 6, -10, -17, -15, -6};
    // bishop coordinates on board 10 x 12
    public final static byte[] OFFSET_VECTOR_COORDINATE = { -11, -10, -9, -1, 1,  9, 10, 11 };


    @Override
    public Collection<Move> possibleMoves(int file, int rank, Board p) {
        return List.of();
    }

    // iterate through disjoint list of board and generat possible moves
    public static Collection<Move> possibleMoves(Board board,  boolean sideToPlay) {
        if (board == null) throw new IllegalArgumentException("possible Knigt moves invoked with null");
        int floor   = getPieceListFloor(WHITE_QUEEN);
        int ceiling = getPieceListSize(BLACK_QUEEN);
        if (sideToPlay == WHITE) {
            return generatePseudoLegal(board, sideToPlay, floor, ceiling, WHITE_QUEEN);
        }
        else {
            return generatePseudoLegal(board, sideToPlay, floor, ceiling, BLACK_QUEEN);
        }
    }

    private static Collection<Move> generatePseudoLegal(Board board, boolean sideToPlay, int floor, int ceiling, PieceType piece) {
        return PieceMove.generatePseudoLegal(board, sideToPlay, floor, ceiling, piece);
    }
}
