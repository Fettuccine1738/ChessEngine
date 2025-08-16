package com.github.fehinti.piece;

import java.util.ArrayList;
import java.util.List;

import com.github.fehinti.board.Board120;

import static com.github.fehinti.board.Board120Utils.*;
import static com.github.fehinti.board.Board120Utils.BOARD_SIZE_120;

// TODO: profiler shows cpu time is spent tracing attack ray for every piece type to king
// * optimize
public class VectorAttack120 {

    public static final int ATTACK_NONE = 0; // no piece can attack the square
    public static final int ATTACK_KQR = 1; // King Rook and Queen can attack
    public static final int ATTACK_QR = 2; // Queen Rook
    public static final int ATTACK_KQBwP = 3; // King Queen Bishop and White Pawn
    public static final int ATTACK_KQBbP = 4; // King Queen Bishop and Black Pawn
    public static final int ATTACK_QB = 5;  // Queen Bishop
    public static final int ATTACK_N = 6;   // Knight

    // New attack array for a 10x12 board
    final static int[] ATTACK_ARRAY = new int[240];
    final static ArrayList<Integer>[][] BLOCKERS = new ArrayList[64][64];
    // knight omitted, blockers not available
    private final static int[] RAY_8 = { 9, 10 , 11, -9, -10, -11, 1, -1, };

    static {
        for (int diff = -119; diff <= 119; diff++) {
            int normalized = diff + 119;
            ATTACK_ARRAY[normalized] = (diff == 0) ? ATTACK_NONE : matchDiff(diff);
        }

        for (int i = 0; i < 64; i++) {
            for (int j = 0; j < 64; j++) {
                ArrayList<Integer> blockers  = new ArrayList<>();
                int diff = Board120.getMailbox64Number(j) - Board120.getMailbox64Number(i);
                // no knight blockers
                if ((diff < 2 && diff > -2) || isKnightMove(diff)) BLOCKERS[i][j] = blockers;
                else {
                    int ray = getRay(diff);
                    int st = Board120.getMailbox64Number(i);
                    int end = Board120.getMailbox64Number(j);
                    // stop at a unit ray before the destination
                    while (st + ray != end) {
                        blockers.add(st+=ray);
                    }
                    BLOCKERS[i][j] = blockers;
                }
            }
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

    private final static boolean WT = true; // white
    private final static boolean BK = false; // black

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

    private static int getRay(int diff) {
        for (int i : RAY_8) {
            if (i < 0 && diff > 0) continue;
            if (i > 0 && diff < 0) continue;
            if (diff % i == 0) return i;
        }
        return 0;
    }

    public static boolean isSquareAttacked(int attackedIndex, int attackingIndex) {
        int normalized = attackedIndex - attackingIndex + 119;
        return ATTACK_ARRAY[normalized] != ATTACK_NONE;
    }

    /**
     * @param board current position after a move has been played, verifying move legality
     *           depending on side to move we look at the opposite side because that is the side
     *          that played before this routine is called.
     * @return
     */
    public static boolean isKingInCheck(Board120 board) {
        int kingSquare = (board.getSideToMove()) ? board.getBlackKingSq()
                : board.getWhiteKingSq();
        assert(kingSquare < BOARD_SIZE_120);
        return isSquareAttacked(board, kingSquare); // this checks  and validates the move PLAYED on the board
    }

    public static boolean isSquareChecked(Board120 board, boolean color, int sq) {
        int[] opps = (color) ? board.getBlackPieceList() : board.getWhitePieceList();

        for (int i : opps) {
            if (i == OFF_BOARD) continue;
            int from = i & 0xff;
            int pie = (i >> 8) & 0xff;
            if (pie == WPAWN || pie == WKNIGHT || pie == -BPAWN || pie == -BKNIGHT) {
                // no possible blockers when attacked by a pawn or knight
                if (isSquareReachableByPiece(from, sq, pie)) return true;
            }
            else if (isSquareReachableByPiece(from, sq, pie)) {
                if (findBlocker(board, from, sq)) return true;
            }
        }
        return false;
    }

    /**
     * @param board current position to be evaluated
     * @param attackedIndex check if this board index is attacked
     * @return   true if the square can be attacked,  false if that square is not attacked
     */
    public static boolean isSquareAttacked(Board120 board, int attackedIndex) {
        if (attackedIndex < 0 || attackedIndex >= BOARD_SIZE_120) {
            throw new IllegalArgumentException("Invalid attacking index: " + attackedIndex);
        }
        if (board == null) throw new NullPointerException("Null board");
        // boolean checkSide = after != board.getSideToMove();
        int[] opps = (board.getSideToMove()) ? board.getWhitePieceList() : board.getBlackPieceList();
        for (int encoding : opps) {
            if (encoding == OFF_BOARD) continue; // captured piece is 'offboarded'
            int pos = encoding & 0xff;
            int piece = (encoding >> 8) & 0xff;
            int p = Math.abs(piece);
            if (p == WPAWN || p == WKNIGHT ||  p == -BPAWN || p == -BKNIGHT) {
                if (isSquareReachableByPiece(pos, attackedIndex, piece)) return true;
            } else if (isSquareReachableByPiece(pos, attackedIndex, p)) {
                if (findBlocker(board, pos, attackedIndex)) return true;
            }
        }
        return false;
    }

    // quick counts how many pieces are defending this square
    public static int getDefenderCount(Board120 board, int defendSq) {
        if (board == null) throw new NullPointerException("Null board");
        int defenders = 0;
        // boolean checkSide = after != board.getSideToMove();
        int[] opps = (board.getSideToMove()) ? board.getWhitePieceList() : board.getBlackPieceList();
        for (int encoding : opps) {
            if (encoding == OFF_BOARD) continue; // captured piece is 'offboarded'
            int pos = encoding & 0xff;
            int piece = (encoding >> 8) & 0xff;
            if (pos == defendSq) continue;
            int p = Math.abs(piece);
            if (p == WPAWN || p == WKNIGHT ||  p == -BPAWN || p == -BKNIGHT) {
                if (isSquareReachableByPiece(pos, defendSq, piece)) defenders++;
            } else if (isSquareReachableByPiece(pos, defendSq, p)) {
                defenders++;
                //if (findBlocker(board, pos, attackedIndex)) defenders++;
            }
        }
        return defenders;
    }

    private static boolean isSquareReachableByPiece(int from, int to, int piece) {
        int x = to - from + 119;
        if (x < 0 || x > 239) throw new RuntimeException(" Out of bounds from " + from +  " " + to + " == " + x);
        int r = ATTACK_ARRAY[x];
        if (r == ATTACK_NONE) return false;
        switch (piece) {
            case WKNIGHT, -BKNIGHT -> { return r == ATTACK_N; }
            case WQUEEN, -BQUEEN -> { return r == ATTACK_KQR || r == ATTACK_QR || r ==  ATTACK_QB || r == ATTACK_KQBwP
                    || r == ATTACK_KQBbP; }
            case WBISHOP,-BBISHOP -> { return r == ATTACK_KQBwP || r ==  ATTACK_KQBbP || r ==ATTACK_QB; }
            case WKING, -BKING -> { return r == ATTACK_KQR || r == ATTACK_KQBwP || r == ATTACK_KQBbP; }
            case WROOK, -BROOK -> { return r == ATTACK_KQR || r == ATTACK_QR; }
            case WPAWN  -> {
                int lcap = from + MoveGenerator.LEFTCAP;
                int rcap = from + MoveGenerator.RIGHTCAP;
                return (lcap == to) || (rcap == to);
            }
            case -BPAWN -> {
                int bl = from - MoveGenerator.LEFTCAP;
                int br = from - MoveGenerator.RIGHTCAP;
                return  (bl == to) || (br == to);
            }
        }
        return false;
    }

    private static boolean findBlocker(Board120 board, int from, int to) {
        List<Integer> blockers = BLOCKERS[Board120.getMailbox120Number(from)][Board120.getMailbox120Number(to)];
        for (int i : blockers) {
            if (board.getPieceOnSquare(i) != 0) return false;
        }
        return true;
    }

}

