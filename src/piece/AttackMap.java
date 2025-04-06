package piece;

import board.Board;
import board.PieceType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static board.Board.getMailbox120Number;
import static board.Board.getMailbox64Number;
import static board.BoardUtilities.BOARD_SIZE;
import static board.BoardUtilities.OFF_BOARD;

/************************************************************************************
 * <a href="https://mediocrechess.blogspot.com/2006/12/guide-attacked-squares.html">...</a>
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 ***********************************************************************************/

public class AttackMap {


    public static final int ATTACK_NONE = 0; // no piece can attack the square
    public static final int ATTACK_KQR = 1; // King Rook and Queen can attack
    public static final int ATTACK_QR = 2; // Queen Rook
    public static final int ATTACK_KQBwP = 3; // King Queen Bishop and White Pawn
    public static final int ATTACK_KQBbP = 4; // King Queen Bishop and Black Pawn
    public static final int ATTACK_QB = 5;  // Queen Bishop
    public static final int ATTACK_N = 6;   // Knight


    private static final int KNIGHT = 1;
    private static final int BISHOP = 2;
    private static final int ROOK = 3;
    private static final int QUEEN = 4;
    private static final int KING = 5;
    private static final int PAWN = 6;

    // number of pawn directions
    private static final int PAWN_DIR = 2;

    private static final int[][] BISHOP_ATT_MAP = new int[BOARD_SIZE][];
    private static final int[][] ROOK_ATT_MAP = new int[BOARD_SIZE][];
    // queen map not needed
    private static final int[][] QUEEN_ATT_MAP = new int[BOARD_SIZE][];
    private static final int[][] KNIGHT_ATT_MAP = new int[BOARD_SIZE][];
    private static final int[][] KING_ATT_MAP = new int[BOARD_SIZE][];
    private static final int[][] PAWN_ATT_MAP = new int[BOARD_SIZE][];

    static {
        for (int sq = 0; sq < BOARD_SIZE; sq++) {
            KNIGHT_ATT_MAP[sq] = computePieceMap(sq, KNIGHT);
            BISHOP_ATT_MAP[sq] = computePieceMap(sq, BISHOP);
            ROOK_ATT_MAP[sq] = computePieceMap(sq, ROOK);
            QUEEN_ATT_MAP[sq] = computePieceMap(sq, QUEEN);
            KING_ATT_MAP[sq] = computePieceMap(sq, KING);
            PAWN_ATT_MAP[sq] = computePieceMap(sq, PAWN);
        }
    }

    private static int[] computePieceMap(int sq, int piece) {
        int rank = sq / BOARD_SIZE;
        int file = sq % BOARD_SIZE;

        List<Integer> attacks = new ArrayList<>();
        int[] delta = (piece <= KING) ? PieceMove.VECTOR_COORDINATES[piece] :
                new int[]{PieceMove.LEFTCAP_64, PieceMove.RIGHTCAP_64};
        int[] offset_delta = (piece <= KING) ? PieceMove.OFFSET_VECTOR_COORDINATES[piece] :
                            new int[]{PieceMove.LEFTCAP, PieceMove.RIGHTCAP};
        int DIR = (piece <= KING) ? PieceMove.DIRECTIONS[piece] : PAWN_DIR;
        int from, newSq = 0;

        for (int i = 0; i < DIR; i++) {
            from = sq;
            // slide until we hit another piece or get to edge
            while (true) {
                newSq = getMailbox120Number(getMailbox64Number(from) + offset_delta[i]); // check if on board
                if (newSq == OFF_BOARD) break;
                else {
                    attacks.add(newSq);
                }
                // all sliding piece except knights and kings
                if (piece > KNIGHT && piece < KING) from = newSq;
                else break;
            }

        }
        return attacks.stream().mapToInt(i -> i).toArray();
    }


    // New attack array for a 10x12 board
    public static final int[] ATTACK_ARRAY = new int[240];

    static {
        for (int diff = -119; diff <= 119; diff++) {
            int normalizedIndex = diff + 120;

            // Determine what pieces can attack based on the difference
            if (diff == 0) {
                ATTACK_ARRAY[normalizedIndex] = ATTACK_NONE; // Same square
            } else if (isKnightMove(diff)) {
                ATTACK_ARRAY[normalizedIndex] = ATTACK_N; // Knight move
            } else if (isDiagonal(diff)) {
                ATTACK_ARRAY[normalizedIndex] = ATTACK_QB; // Bishop or Queen
            } else if (isStraight(diff)) {
                ATTACK_ARRAY[normalizedIndex] = ATTACK_QR; // Rook or Queen
            } else {
                ATTACK_ARRAY[normalizedIndex] = ATTACK_NONE; // No valid attack
            }
        }
    }

    private static boolean isKnightMove(int diff) {
        return diff == -21 || diff == -19 || diff == -12 || diff == -8 ||
                diff == 8 || diff == 12 || diff == 19 || diff == 21;
    }

    private static boolean isDiagonal(int diff) {
        return Math.abs(diff) % 9 == 0 || Math.abs(diff) % 11 == 0;
    }

    private static boolean isStraight(int diff) {
        return diff % 10 == 0 || (diff >= -8 && diff <= 8);
    }

    public static boolean isSquareAttacked(int attackedIndex, int attackingIndex) {
        int normalizedIndex = attackedIndex - attackingIndex + 120;
        System.out.println(ATTACK_ARRAY[normalizedIndex]);
        return ATTACK_ARRAY[normalizedIndex] != ATTACK_NONE;
    }

    private static int[][] computedAttackMaps(int piece) {
        switch (piece) {
            case KNIGHT -> { return KNIGHT_ATT_MAP; }
            case BISHOP -> { return BISHOP_ATT_MAP; }
            case ROOK -> { return ROOK_ATT_MAP; }
            case QUEEN -> { return QUEEN_ATT_MAP; }
            case KING -> { return KING_ATT_MAP; }
            case PAWN -> { return PAWN_ATT_MAP; }
            default -> throw new IllegalArgumentException("Invalid piece: " + piece);
        }
    }

    public static boolean isKingInCheck(Board board) {
        // extract king data from board
        int kingPosition = (board.getSideToMove()) ? board.getWhitePieceList()[0]
                : board.getBlackPieceList()[0];
        assert(kingPosition != 0);
        int kingSquare = kingPosition & 0xff; // extract last bits  - index (0 .. 63)
        assert(0 <= kingSquare  && kingSquare < BOARD_SIZE);
        return isSquareAttacked(board, kingSquare);
    }

    /**
     * @param board current position to be evaluated
     * @param attackedIndex check if this board index is attacked
     * @return   true if the square can be attacked,  false if that square is not attacked
     */
    public static boolean isSquareAttacked(Board board, int attackedIndex) {
        if (attackedIndex < 0 || attackedIndex >= BOARD_SIZE) {
            throw new IllegalArgumentException("Invalid attacking index: " + attackedIndex);
        }
        if (board == null) {
            throw new NullPointerException("Null board");
        }
        // for each piece attack map , see if we can reach the king square
        for (int pc = KNIGHT; pc <= PAWN; pc++) {
            if (isInComputedAttackMap(board, attackedIndex, pc)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isInComputedAttackMap(Board board, int attackedIndex, int piece) {
        int[][] attackMap = computedAttackMaps(piece);
        int length = attackMap[attackedIndex].length;
        for (int sq = 0; sq < length; sq++) {
            if (canReachSquare(board, attackedIndex,
                    attackMap[attackedIndex][sq], board.getSideToMove(), piece))  {
                return true;
            }
        }
       return false;
    }

    /**
     * @param board current position
     * @param attacked index between 0..63 that is checked to see if we can reach this index
     * @param attacking  the origin index
     * @param side  side to play
     * @param pc    piece value , optimizes function  ignoring redundant lookups
     * @return   true / false if piece on attacking can reach attacked index.
     */
    public static boolean canReachSquare(Board board, int attacked, int attacking, boolean side, int pc) { // raytracing
        PieceType attackingPiece = board.getPieceOnBoard(attacking);
        // is the attacking square on the same side as attacked or empty?
        if (attackingPiece == PieceType.EMPTY) { return false; }
        else if (attackingPiece.isWhite() == side) return false;
        // if it is an opponent piece but cannot traverse through current searching piece ray's directions
        // then ignore e.g (if we find a rook instead of a bishop , no need looping back to attacked
        // since no ray directions shared.
        else if (!traverseRayDirections(pc, attackingPiece)) return false;
        else {
            int piece = Math.abs(attackingPiece.getValue()) - 1;
            int directions = PieceMove.DIRECTIONS[piece];
            // int[] vector120 = PieceMove.OFFSET_VECTOR_COORDINATES[directions];
            int start, square = attacking;
            boolean slides = PieceMove.IS_SLIDING[piece];
            int newSquare;
            for (int i = 0; i < directions; i++) {
                // ignore any coordinates that leads to index of pieces that may not contain the piece
                // if the attacked index is less than the attacking, we only need to look at index lower
                // than attacking, alternate for the opposite
                if (PieceMove.OFFSET_VECTOR_COORDINATES[piece][i] < 0 && attacking < attacked ) {
                    continue;
                }
                else if (PieceMove.OFFSET_VECTOR_COORDINATES[piece][i] > 0 && attacking > attacked) {
                    continue;
                }
                while (true) {
                    newSquare = getMailbox120Number(getMailbox64Number(square)
                    + PieceMove.OFFSET_VECTOR_COORDINATES[piece][i]);
                    if (newSquare == OFF_BOARD) break;
                    if (newSquare == attacked) return true; // attack square reached
                    PieceType currentPiece = board.getPieceOnBoard(newSquare);
                    if (currentPiece == PieceType.EMPTY) { // advance to next square
                        // if (newSquare == attacked) return true; // attack square reached
                        if (!slides) break; // it's not a sliding piece move to next direction
                        square = newSquare;
                    }
                   // if (newSquare == attacked) return true; // attack square reached
                    else { // there is a blocking piece
                        break;
                    }
                }
            }
        }
        return false;
    }

    // allow queen to travers rook rays and bishop rays (maybe vice versa)???
    private static boolean traverseRayDirections(int searcher, PieceType p) {
        // create pairs for bishop queen and rookqueen and maybe kingqueen
        int value = Math.abs(p.getValue());
        if (searcher == BISHOP && (value-1 == QUEEN || value-1 == BISHOP)) return true;
        else if (searcher == ROOK && (value-1 == QUEEN || value-1 == ROOK)) return true;
        if (value == 1 && searcher == PAWN) return true;
        return (searcher == --value);
    }

    public static void pprint() {
        int count = 0;
        for (int i = 0; i < 24; i++) {
            for (int j = 0; j < 10; j++) {
                System.out.printf("%d\t", ATTACK_ARRAY[10 * j + i]);
            }
            System.out.println();
        }

        System.out.println("\nKNIGHT\n");
        for (int[] arr : KNIGHT_ATT_MAP) {
            System.out.println(Arrays.toString(arr));
        }
        System.out.println("\nBISHOP\n");
        for (int[] arr : BISHOP_ATT_MAP) {
            System.out.println(Arrays.toString(arr));
        }
        System.out.println("\nROOK\n");
        for (int[] arr : ROOK_ATT_MAP) {
            System.out.println(Arrays.toString(arr));
        }
        System.out.println("\nQUEEN\n");
        for (int[] arr : QUEEN_ATT_MAP) {
            System.out.println(Arrays.toString(arr));
        }
        System.out.println("\nKING\n");
        for (int[] arr : KING_ATT_MAP) {
            System.out.println(Arrays.toString(arr));
        }
        System.out.println("\nPAwn\n");
        for (int[] arr : PAWN_ATT_MAP) {
            System.out.println(Arrays.toString(arr));
        }
    }

    // attack maps for sliding pieces
    public static  void main(String[] args) {
        pprint();
        int attacked_square = 25;
        int attacking_square = 28;
//
        int index =  attacking_square - attacked_square  + 120;
        System.out.println(index);
        System.out.println(ATTACK_ARRAY[index]);
//
        int attacked = 74; // d4
        int attacking = 55; // g7
        System.out.println(AttackMap.isSquareAttacked(attacked, attacking)); // true for bishop or queen

    }
}
