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

public class Knight implements Piece {

    // knight coordinates on a 1 d array(board)
    public final static byte[] OFFSET_VECTOR_COORDINATE = { -21, -19, -12, -8, 8, 12, 19, 21};
    public final static byte[] VECTOR_COORDINATE        = { -17, -15, -10, -6,  6, 10, 15, 17};


    private final static int[][] ATTACK_MAP = new int[BOARD_SIZE][];
    private final static int SIZE = 8; // direction size

    static {
        for (int sq = 0; sq < BOARD_SIZE; sq++) {
            ATTACK_MAP[sq] = computeKnightAttacks(sq);
        }
    }

    private static int[] computeKnightAttacks(int sq) {
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

    @Override
    public Collection<Move> possibleMoves(int file, int rank, Board p) {
        return List.of();
    }

    // iterate through disjoint list of board and generat possible moves
    public static Collection<Move> possibleMoves(Board board,  boolean sideToPlay) {
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

    private static Collection<Move> generatePseudoLegal(Board board, boolean sideToPlay, int floor, int ceiling, PieceType piece) {
        int[] knights = (sideToPlay)  ? board.getWhitePieceList() : board.getBlackPieceList();
        Collection<Move> moves = new ArrayList<>();
        int pie = 0, pos = 0;
        int square = 0;
        int encoding;
        int mailbox64, mailbox120;
        // null move, piece does not need to move
        for (int index = floor; index < ceiling; index++) {
            encoding = knights[index];
            if (encoding != 0) {
                pie = encoding >> 8;
                square = encoding & 0xff;
                moves.add(new Move((byte) (square), (byte) (square), sideToPlay, piece, MoveType.NULLMOVE));
                if (sideToPlay) assert(pie == WHITE_KNIGHT.getValue());
                else assert(pie == BLACK_KNIGHT.getValue());

                // what is the square mailbox 64's number
                mailbox64 = getMailbox64Number(square);
                int b;
                for (int i = 0; i < VECTOR_COORDINATE.length; i++) {
                    b = 0;
                    // calculate board index of  move
                    b = VECTOR_COORDINATE[i];
                    square += b;
                    // what is mailbox 120 of the move
                    // mailbox64 = getMailbox64Number(mailbox64 + OFFSET_VECTOR_COORDINATE[i]);
                    mailbox120 = getMailbox120Number(mailbox64 + OFFSET_VECTOR_COORDINATE[i]);
                    if (mailbox120 != OFF_BOARD)  {
                        if (board.getPieceOnBoard(square) == EMPTY) {
                            moves.add(new Move((byte) (square - b), (byte) (square), sideToPlay, piece, MoveType.NORMAL));
                        }
                        else if (board.getPieceOnBoard(square).getValue() < 0) {
                            moves.add(new Move((byte) (square - b), (byte) (square), sideToPlay, piece, MoveType.CAPTURE));
                        }
                    }
                    square -= b; // reset to from square
                }

            }
        }
        System.out.println("Moves sz : " + moves.size());
        return moves;
    }

}

