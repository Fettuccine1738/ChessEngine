package piece;

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

    private static final int[][] BISHOP_ATT_MAP = new int[BOARD_SIZE][];
    private static final int[][] ROOK_ATT_MAP = new int[BOARD_SIZE][];
    private static final int[][] QUEEN_ATT_MAP = new int[BOARD_SIZE][];
    private static final int[][] KNIGHT_ATT_MAP = new int[BOARD_SIZE][];

    static {
        for (int sq = 0; sq < BOARD_SIZE; sq++) {
            KNIGHT_ATT_MAP[sq] = computePieceMap(sq, KNIGHT);
            BISHOP_ATT_MAP[sq] = computePieceMap(sq, BISHOP);
            ROOK_ATT_MAP[sq] = computePieceMap(sq, ROOK);
            QUEEN_ATT_MAP[sq] = computePieceMap(sq, QUEEN);
        }
    }

    private static int[] computePieceMap(int sq, int piece) {
        int rank = sq / BOARD_SIZE;
        int file = sq % BOARD_SIZE;

        List<Integer> attacks = new ArrayList<>();
        int[] delta = PieceMove.VECTOR_COORDINATES[piece];
        int[] offset_delta = PieceMove.OFFSET_VECTOR_COORDINATES[piece];
        int DIR = PieceMove.DIRECTIONS[piece];
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
                // all sliding piece except knights
                if (piece > 1) from = newSq;
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
    }

    // attack maps for sliding pieces
    public static  void main(String[] args) {
        pprint();
        int attacked_square = 25;
        int attacking_square = 28;

        int index =  attacking_square - attacked_square  + 120;
        System.out.println(index);
        System.out.println(ATTACK_ARRAY[index]);

        int attacked = 74; // d4
        int attacking = 55; // g7
        System.out.println(AttackMap.isSquareAttacked(attacked, attacking)); // true for bishop or queen

    }
}
