package com.github.fehinti.piece;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.fehinti.board.FENParser;

import static com.github.fehinti.board.Board120.getMailbox120Number;
import static com.github.fehinti.board.Board120.getMailbox64Number;
import static com.github.fehinti.board.Board120Utils.BOARD_SIZE;
import static com.github.fehinti.board.Board120Utils.OFF_BOARD;

// TODO: profiler shows cpu time is spent tracing attack ray for every piece type to king
// * optimize
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

    public static final int[][] BISHOP_ATT_MAP = new int[BOARD_SIZE][];
    public static final int[][] ROOK_ATT_MAP = new int[BOARD_SIZE][];
    // queen map not needed
    public static final int[][] QUEEN_ATT_MAP = new int[BOARD_SIZE][];
    public static final int[][] KNIGHT_ATT_MAP = new int[BOARD_SIZE][];
    public static final int[][] KING_ATT_MAP = new int[BOARD_SIZE][];

    final static ArrayList<Integer>[][] BLOCKERS = new ArrayList[64][64];

    static {
        for (int sq = 0; sq < BOARD_SIZE; sq++) {
            KNIGHT_ATT_MAP[sq] = computePieceMap(sq, KNIGHT);
            BISHOP_ATT_MAP[sq] = computePieceMap(sq, BISHOP);
            ROOK_ATT_MAP[sq] = computePieceMap(sq, ROOK);
            QUEEN_ATT_MAP[sq] = computePieceMap(sq, QUEEN);
            KING_ATT_MAP[sq] = computePieceMap(sq, KING);
        }
    }

    private static int[] computePieceMap(int sq, int piece) {

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
            int normalized = diff + 120;
            ATTACK_ARRAY[normalized] = (diff == 0) ? ATTACK_NONE : matchDiff(diff);
        }

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 24; j++) {
                System.out.print(ATTACK_ARRAY[24 * i + j] + " ");
            }
            System.out.println();
        }
    }

    static int matchDiff(int vector) {
        if (isKnightMove(vector))            return ATTACK_N;
        else if (isPawnMove(vector, WT))     return ATTACK_KQBwP;
        else if (isPawnMove(vector, BK))     return ATTACK_KQBbP;
        else if (isKingMove(vector) && isRookMove(vector)) return ATTACK_KQR;
        else if (isQueenMove(vector) && isRookMove(vector))  return ATTACK_QR;
        else if (isQueenMove(vector) && isBishopMove(vector))  return ATTACK_QB;
        return ATTACK_NONE;
    }

    static boolean WT = true; // white
    static boolean BK = false; // black

    static boolean isQueenMove(int diff) {
        return isRookMove(diff) || isBishopMove(diff);
    }

    static boolean isPawnMove(int diff, boolean color) {
        if (color) return diff == 9 || diff == 11;
        else return diff == -9 || diff == -11;
    }

    static boolean isKingMove(int diff) {
        int r = Math.abs(diff);
        return  r ==  1 || r == 9 || r ==  10 || r == 11;
    }

    private static boolean isKnightMove(int diff) {
        int r = Math.abs(diff);
        return r == 8 || r == 12 || r == 19 || r == 21;
    }

    private static boolean isBishopMove(int diff) {
        return Math.abs(diff) % 9 == 0 || Math.abs(diff) % 11 == 0;
    }

    private static boolean isRookMove(int diff) {
        return diff % 10 == 0 || (diff >= -8 && diff <= 8);
    }

    public static boolean isSquareAttacked(int attackedIndex, int attackingIndex) {
        int normalized = attackedIndex - attackingIndex + 120;
        System.out.println(ATTACK_ARRAY[normalized]);
        return ATTACK_ARRAY[normalized] != ATTACK_NONE;
    }

    public static int[][] computedAttackMaps(int piece) {
        System.out.println(piece);
        switch (piece) {
            case KNIGHT -> { return KNIGHT_ATT_MAP; }
            case BISHOP -> { return BISHOP_ATT_MAP; }
            case ROOK -> { return ROOK_ATT_MAP; }
            case QUEEN -> { return QUEEN_ATT_MAP; }
            case KING -> { return KING_ATT_MAP; }
            default -> throw new IllegalArgumentException("Invalid piece: " + piece);
        }
    }

    public static final boolean  AFTER = true; // checks if own king is attacked, does this move leave own king in check
    public static final boolean  BEFORE = false; // also checks if own king is in check
    /**
     * @param board current position after a move has been played, verifying move legality
     *           depending on side to move we look at the opposite side because that is the side
     *          that played before this routine is called.
     * @return
     */
    public static boolean isKingInCheck(Board board) {
        // extract king data from board
        // if black 's turn to play get the white king because that's the
        // last move played
        int kingPosition = (board.getSideToMove()) ? board.getBlackPieceList()[0]
                : board.getWhitePieceList()[0];
        assert(kingPosition != 0);
        int kingSquare = kingPosition & 0xff; // extract last bits  - index (0 .. 63)
        assert(kingSquare < BOARD_SIZE);
        return isSquareAttacked(board, kingSquare, AFTER); // this checks  and validates the move PLAYED on the board
    }


    /**
     * @param board current position to be evaluated
     * @param attackedIndex check if this board index is attacked
     * @param after determines checking if the side to play's or side that played square is being attacked
     * @return   true if the square can be attacked,  false if that square is not attacked
     */
    public static boolean isSquareAttacked(Board board, int attackedIndex, boolean after) {
        if (attackedIndex < 0 || attackedIndex >= BOARD_SIZE) {
            throw new IllegalArgumentException("Invalid attacking index: " + attackedIndex);
        }
        if (board == null) {
            throw new NullPointerException("Null board");
        }
        // for each piece attack map , check if we can reach the king square
        for (int pc = KNIGHT; pc <= PAWN; pc++) {
            if (isInComputedAttackMap(board, attackedIndex, pc, after)) {

                return true;
            }
        }
        return false;
    }

    private static boolean isInComputedAttackMap(Board board, int attackedIndex, int piece, boolean after) {
        boolean checkSide = after != board.getSideToMove();
        if (piece == PAWN) {
            return isSquareAttackedByPawn(board, attackedIndex, checkSide);
        }
        int[][] attackMap = computedAttackMaps(piece);
        int length = attackMap[attackedIndex].length;
        for (int sq = 0; sq < length; sq++) {
            // reverse board.getSideTomove() to correctly check the side that "MOVED" not side
            // to play
                if (traceRayToSquare(board, attackedIndex,
                        attackMap[attackedIndex][sq],
                        checkSide,
                        piece))  {
                    return true;
                }
        }
       return false;
    }

    // pawn checks are handled on the fly no need for the precomputed map
    private static boolean isSquareAttackedByPawn(Board board, int attackedIndex, boolean side) {
        // black is being checked
        int[] square = (side) ?  PieceMove.WHITE_CAPTURES : PieceMove.BLACK_CAPTURES;
        for (int s : square) {
            int cap = getMailbox120Number(getMailbox64Number(attackedIndex) + s);
            if (cap != OFF_BOARD) {
                Piece xpawn = board.getPieceOnBoard(cap);
                if (xpawn == Piece.EMPTY || Math.abs(xpawn.getValue()) != 1) continue;
                if (xpawn.isWhite() != side) return true;
            }
        }
        return false;
    }

   /**
     * @param board current position
     * @param attacked index between 0..63 that is checked to see if we can reach this index
     * @param attacking  the origin index
     * @param side  side to play
     * @param piece  piece value, optimizes function  ignoring redundant lookups
     * @return      true / false if piece on attacking can reach attacked index.
     */
    public static boolean traceRayToSquare(Board board, int attacked,  int attacking, boolean side, int piece) {
        Piece attackingPc = board.getPieceOnBoard(attacking);
        if (attackingPc == Piece.EMPTY || attackingPc.isWhite() == side) return false;
        int index = Math.abs(attackingPc.getValue()) - 1;
        int ray = calculateRay(getMailbox64Number(attacking), getMailbox64Number(attacked),
                PieceMove.OFFSET_VECTOR_COORDINATES[index]);
        if (ray == 0) return false;
        else if (!canTraverseRay(piece, attackingPc)) return false;
        else {
            boolean slides = PieceMove.IS_SLIDING[index];
            int square = attacking;
            while (true) {
                square = getMailbox120Number(getMailbox64Number(square) + ray);
                if (square == OFF_BOARD) return false;
                if (square == attacked) return true;
                Piece currentPiece = board.getPieceOnBoard(square);
                if (currentPiece == Piece.EMPTY) {
                    if (!slides) break;
                }
                else return false; // there is a piece blocking us
            }
        }
        return false;
    }

    // allow queen to travers rook rays and bishop rays (maybe vice versa)???
    private static boolean canTraverseRay(int searcher, Piece p) {
        // create pairs for bishop queen and rookqueen and maybe kingqueen
        int value = Math.abs(p.getValue());
        if (searcher == BISHOP && (value-1 == QUEEN || value-1 == BISHOP)) return true;
        else if (searcher == ROOK && (value-1 == QUEEN || value-1 == ROOK)) return true;
        if (value == 1 && searcher == PAWN) return true;
        return (searcher == --value);
    }

    // calculate ray that can reach attackedsquare from attacking square
    // 0 if it cannot
    private static int calculateRay(final int attacking, final int attacked, int[] rays) {
        assert(attacking != attacked);
        int distance = attacked - attacking;
        int sz = rays.length;
        int current = 0;
        int steps = 65; // helps decide which ray is faster to follow, 65 because 64 squares
        for (int i = 0; i < sz; i++) {
            int ray = rays[i];
            if (ray == 0) break;
            // look for a -ve ray, if attacked is < attacking
            if (ray > 0 && attacking > attacked) continue;
            // look for a +ve ray if attacked is > attacking
            if (ray < 0  && attacking < attacked) continue;
            else if (distance % ray == 0 && (distance / ray) < steps) {
                current = ray;
                steps = distance / ray;
            }
        }
        return current;
    }

    public static void pprint() {
        int count = 0;
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
    }

    // attack maps for sliding pieces
    public static  void main(String[] args) {
        // boolean reachable = traceRayToSquare(b, 60, 12, true, 3);
        Board b = FENParser.parseFENotation("rnbq1bnr/ppp1pppp/2k5/3P4/8/N7/PPPP1PPP/R1BQKBNR b KQ - 0 3");
        System.out.println(b.print());
        long now = System.currentTimeMillis();
        boolean reachable = isKingInCheck(b);
        boolean a = isSquareAttacked(getMailbox64Number(1), getMailbox64Number(9));
        System.out.println("a  " + a);
        boolean ba = isSquareAttacked(getMailbox64Number(2), getMailbox64Number(18));
        System.out.println(ba);
        boolean c = isSquareAttacked(2, 10);
        System.out.println(c);
        System.out.println();
        System.out.println(reachable + "\t " + (System.currentTimeMillis() - now));
        for (int[] arr : KNIGHT_ATT_MAP) {
            System.out.println(Arrays.toString(arr));
        }
    }
}
