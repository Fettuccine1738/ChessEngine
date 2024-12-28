package piece;

import board.Board;
import board.Move;
import board.PieceType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static board.Board.getMailbox120Number;
import static board.BoardUtilities.*;
import static board.PieceType.BLACK_KING;
import static board.PieceType.WHITE_KING;

public class King {
    // king coordinates on board 64
    //public final static byte[] VECTOR_COORDINATE        = { 7, 8 , 9, 1, 0, -1, -7, -8, -9 };
    public final static byte[] VECTOR_COORDINATE        = { 9, 8, 7,  1, -1, -7, -8, -9 };
    //public final static byte[] VECTOR_COORDINATE        = { 9, 8, 7,  1, 0, -1, -7, -8, -9 };
    // king coordinates on board 10 x 12
    public final static byte[] OFFSET_VECTOR_COORDINATE = { 11, 10, 9, 1, -1, -9, -10, -11 };
    //public final static byte[] OFFSET_VECTOR_COORDINATE = { 11, 10, 9, 1, 0, -1, -9, -10, -11 };
    // public final static byte[] OFFSET_VECTOR_COORDINATE = { -11, -10, -9, -1, 1,  9, 10, 11 };


    private final static int[][] ATTACK_MAP = new int[BOARD_SIZE][];
    private final static int SIZE = 8; // direction size

    static {
        for (int sq = 0; sq < BOARD_SIZE; sq++) {
            ATTACK_MAP[sq] = computeKingAttacks(sq);
        }
    }

    private static int[] computeKingAttacks(int sq) {
        int rank = sq / RANK_8;
        int file = sq % RANK_8;

        List<Integer> attacks = new ArrayList<>();
        int mailbox;
        for (int i = 0; i < SIZE; i++) {
            int target = sq + VECTOR_COORDINATE[i];
            int targetRank = target / RANK_8;
            int targetFile = target % RANK_8;

            mailbox = getMailbox120Number(sq + OFFSET_VECTOR_COORDINATE[i]);
            if (mailbox != OFF_BOARD) {
                attacks.add(target);
            }
        }
        return attacks.stream().mapToInt(i -> i).toArray();
    }


    public static Collection<Integer> possibleMoves(Board board,  boolean sideToPlay) {
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
    private static Collection<Integer> generatePseudoLegal(Board board, boolean sideToPlay, int floor, int ceiling, PieceType piece) {
        return PieceMove.generatePseudoLegal(board, sideToPlay, floor, ceiling, piece);
    }


}
