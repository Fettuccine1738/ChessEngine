package piece;

import board.Board;
import board.Move;
import board.PieceType;

import java.util.Collection;
import java.util.List;

import static board.BoardUtilities.*;
import static board.PieceType.BLACK_KING;
import static board.PieceType.WHITE_KING;

public class King implements Piece {
    // king coordinates on board 64
    //public final static byte[] VECTOR_COORDINATE        = { 7, 8 , 9, 1, 0, -1, -7, -8, -9 };
    public final static byte[] VECTOR_COORDINATE        = { 9, 8, 7,  1, -1, -7, -8, -9 };
    //public final static byte[] VECTOR_COORDINATE        = { 9, 8, 7,  1, 0, -1, -7, -8, -9 };
    // king coordinates on board 10 x 12
    public final static byte[] OFFSET_VECTOR_COORDINATE = { 11, 10, 9, 1, -1, -9, -10, -11 };
    //public final static byte[] OFFSET_VECTOR_COORDINATE = { 11, 10, 9, 1, 0, -1, -9, -10, -11 };
    // public final static byte[] OFFSET_VECTOR_COORDINATE = { -11, -10, -9, -1, 1,  9, 10, 11 };

    @Override
    public Collection<Move> possibleMoves(int file, int rank, Board p) {
        return List.of();
    }

    public static Collection<Move> possibleMoves(Board board,  boolean sideToPlay) {
        if (board == null) throw new IllegalArgumentException("possible King moves invoked with null");
        int floor   = getPieceListFloor(WHITE_KING);
        int ceiling = getPieceListSize(BLACK_KING);
        if (sideToPlay == WHITE) {
            return generatePseudoLegal(board, sideToPlay, floor, ceiling, WHITE_KING);
        }
        else {
            return generatePseudoLegal(board, sideToPlay, floor, ceiling, BLACK_KING);
        }
    }

    // legal moves ?? play move then look at !side (opponent), if current square is in move list of
    // !side remove move.

    // castling ??

    // direct testing
    private static Collection<Move> generatePseudoLegal(Board board, boolean sideToPlay, int floor, int ceiling, PieceType piece) {
        return PieceMove.generatePseudoLegal(board, sideToPlay, floor, ceiling, piece);
    }


}
