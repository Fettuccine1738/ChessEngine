package piece;

import board.Board;
import board.Move;
import board.MoveType;
import board.PieceType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static board.Board.getMailbox120Number;
import static board.Board.getMailbox64Number;
import static board.BoardUtilities.*;
import static board.PieceType.*;

public class Knight  {

    // knight coordinates on a 1 d array(board)
    public final static byte[] OFFSET_VECTOR_COORDINATE = { -21, -19, -12, -8, 8, 12, 19, 21};
    public final static byte[] VECTOR_COORDINATE        = { -17, -15, -10, -6,  6, 10, 15, 17};


    private final static int[][] ATTACK_MAP = new int[BOARD_SIZE][];
    private final static int SIZE = 8; // direction size


    // iterate through disjoint list of board and generat possible moves
    public static Collection<Integer> possibleMoves(Board board,  boolean sideToPlay) {
        if (board == null) throw new IllegalArgumentException("possible Knigt moves invoked with null");
        int floor   = getPieceListFloor(WHITE_KNIGHT);
        int ceiling = getPieceListSize(BLACK_KNIGHT);
        if (sideToPlay == WHITE) {
            return generatePseudoLegal(board, sideToPlay, floor, ceiling, WHITE_KNIGHT);
        }
        else {
            return generatePseudoLegal(board, sideToPlay, floor, ceiling, BLACK_KNIGHT);
        }
    }

    // direct testing
    private static Collection<Integer> generatePseudoLegal(Board board, boolean sideToPlay, int floor, int ceiling, PieceType piece) {
        return PieceMove.generatePseudoLegal(board, sideToPlay, floor, ceiling, piece);
    }

}

