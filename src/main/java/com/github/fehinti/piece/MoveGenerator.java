package com.github.fehinti.piece;

import com.github.fehinti.board.Board120;
import com.github.fehinti.board.Board120Utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import static com.github.fehinti.board.Board120Utils.*;
import static com.github.fehinti.piece.Move.CASTLE;

public class MoveGenerator {
    //    index like so --------->      { PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING_;
    final static boolean[] IS_SLIDING = { false, false, true, true, true, false};
    final static int[]   DIRECTIONS   = { 0,     8,      4,     4,    8,   8 };

    // board coordinates for board 120
    final static int[][] VECTOR_COORDINATES = {
            {   0,   0,  0,  0, 0,  0,  0,  0 },
            { -21, -19,-12, -8, 8, 12, 19, 21 }, /* KNIGHT */
            { -11,  -9,  9, 11, 0,  0,  0,  0 }, /* BISHOP */
            { -10,  -1,  1, 10, 0,  0,  0,  0 }, /* ROOK */
            { -11, -10, -9, -1, 1,  9, 10, 11 }, /* QUEEN */
            { -11, -10, -9, -1, 1,  9, 10, 11 }  /* KING */  };

    final static int SINGLE_PUSH = 10; // single push
    final static int DOUBLE_PUSH = 20; // valid on the second rank only
    final static int LEFTCAP = 9;
    final static int RIGHTCAP = 11;

    private static final int[] MVA_LVA = { 1, 3, 3, 5, 9, 20};
    // MVA_LVA_SCORE[victim][attacker]
    private static final int[][] MVA_LVA_SCORE =  {
            {  100, 110, 110, 130, 140, 150 }, /* PAWN */
            {  70, 100, 100, 120, 140, 150 }, /* KNIGHT */
            {  70, 100, 100, 120, 140, 150 }, /* BISHOP */
            {  60, 65,  65, 100,  140, 150 }, /* ROOK */
            {  20, 30,  30,  40, 100, 150 }, /* QUEEN */
            {  10, 20, 20, 30, 140, 150 }  /* KING */
    };


    /**
     * @param board  current position
     * @return           a list of 32 bit ints encoding all move information
     */
    public static List<Integer> generatePseudoLegal(Board120 board) {
        if (board == null) throw new IllegalArgumentException("possible moves invoked with null board");
        boolean side = board.getSideToMove();

        List<Integer> moveList = new ArrayList<>();

        int[] piecelist = (side) ? board.getWhitePieceList() : board.getBlackPieceList();
        int isEmpty = OFF_BOARD;
        int piece, isPawn = 0;

        for (int index = 0; index < piecelist.length; index++) {
            int encoding = piecelist[index];
            if (encoding != OFF_BOARD) {
                piece    = (encoding >> 8) & 0xff;
                byte square = (byte) (encoding & 0xff);

                int val = (side) ? piece - 1 : (128 - piece - 1); // zero index

                // generate pawn moves separately
                if (val == isPawn) {
                    generatePseudoPawnMoves(board, moveList, index, square);
                } else if (val != OFF_BOARD) {
                    // use piece value to index into offset vector
                    int[]  coordinates = VECTOR_COORDINATES[val]; // board 120
                    boolean slides = IS_SLIDING[val];

                    // generate castles separately if available
                    if (val == WKING - 1 && board.canSideCastle(side)) {
                        generateCastle(board, side, moveList, index);
                    }
                    // what is the square mailbox 64's number
                    int N = DIRECTIONS[val]; // number of ray / knight piesible directions
                    for (int i = 0; i < N; i++) {
                        byte from = square;
                        byte newSquare = 0;
                        // if it is a sliding piece
                        while (true) {
                            int to = from + coordinates[i];
                            // newSquare is also used as piece on newSquare
                            newSquare = board.getPieceOnSquare(to);
                            if (newSquare == OFF_BOARD) break; // off board
                            if (newSquare == Board120Utils.EMPTY) {
                               moveList.add(Move.encodeMove(square, to, 0, Move.QUIET, index));
                            } else {
                                boolean xside = board.isPieceWhite(newSquare);
                                if (side != xside) {
                                    // 133 to map black pieces to 0..6 when white is capturing
                                    int score = (side) ? MVA_LVA_SCORE[val][(127 + newSquare)] :
                                            MVA_LVA_SCORE[val][newSquare - 1];
                                    moveList.add(Move.encodeMove(square, to, 0,
                                                 Move.CAPTURE,  index,  score));
                                    break;
                                } else break;
                            }
                            if (!slides) break;
                            from = (byte) to;
                        }
                    }
                }
            }
        }
        return moveList;
    }

    /**
     * @param board  current position.
     * @param side   side to move BLACK or WHITE.
     * @param moves  list of move information encoded into integers
     * @param index  index of piece in the piece list, needed to preserver move generation ordering
     */
    private static void generateCastle(Board120 board, boolean side, List<Integer> moves, int index) {
        if (!board.canSideCastle(side)) return;
        boolean longCastle = (side) ? board.canWhiteCastleQueenside() : board.canBlackCastleQueenside();
        boolean shortCastle = (side) ? board.canWhiteCastleKingside() : board.canBlackCastleKingside();

        // do not castle if king is under check
        if (VectorAttack120.isSquareChecked(board, side, (side) ? E1 : E8)
                && board.getPieceOnSquare((side) ? E1 : E8) == ((side) ? WKING : BKING)) {
            return;
        }

        if (longCastle) {
            if (side == WHITE) {
                if (board.getPieceOnSquare(A1) == WROOK
                        && board.getPieceOnSquare(B1) == 0
                        && board.getPieceOnSquare(C1) == 0
                        && board.getPieceOnSquare(D1) == 0
                        && !VectorAttack120.isSquareChecked(board, side, C1)
                        && !VectorAttack120.isSquareChecked(board, side, D1)) {
                    assert(board.getPieceOnSquare(E1) == WKING);
                    assert(!VectorAttack120.isKingInCheck(board));
                    // move king towards rook
                    moves.add(Move.encodeMove(E1, C1, 0, CASTLE, index));
                }
            } else { // black
                if (board.getPieceOnSquare(A8) == BROOK
                        && board.getPieceOnSquare(B8) == 0
                        && board.getPieceOnSquare(C8) == 0
                        && board.getPieceOnSquare(D8) == 0
                        && !VectorAttack120.isSquareChecked(board, side, C8)
                        && !VectorAttack120.isSquareChecked(board, side, D8)) {
                    assert(board.getPieceOnSquare(E8) == BKING);
                    assert(!VectorAttack120.isKingInCheck(board));
                    moves.add(Move.encodeMove(E8, C8, 0, Move.CASTLE, index));
                }
            }
        }

        if (shortCastle) {
            if (side == WHITE) {
                if (board.getPieceOnSquare(H1) == WROOK
                        && board.getPieceOnSquare(G1) == 0
                        && board.getPieceOnSquare(F1) == 0
                        && !VectorAttack120.isSquareChecked(board, side, G1)
                        && !VectorAttack120.isSquareChecked(board, side, F1)) {
                    assert(board.getPieceOnSquare(E1) == WKING);
                    assert(!VectorAttack120.isKingInCheck(board));
                    moves.add(Move.encodeMove(E1, G1, 0, Move.CASTLE, index));
                }
            } else {
                if (board.getPieceOnSquare(H8) == BROOK &&
                        board.getPieceOnSquare(G8) == 0
                        && board.getPieceOnSquare(F8) == 0
                        // cannot castle through checks
                        && !VectorAttack120.isSquareChecked(board, side, G8)
                        && !VectorAttack120.isSquareChecked(board, side, F8)) {
                    assert(!VectorAttack120.isKingInCheck(board));// does not make sense
                    assert(board.getPieceOnSquare(E8) == BKING);
                    moves.add(Move.encodeMove(E8, G8, 0, Move.CASTLE, index));
                }
            }
        }
    }

    private static void generatePseudoPawnMoves(Board120 board, List<Integer> moves, int index, int from) {
        boolean side = board.getSideToMove();
        int ep = board.getEnPassant();
        Predicate<Byte> isPromotingRank = (side) ?
                Board120Utils::isOnSeventhRank : Board120Utils::isOnSecondRank;

        generateQuietPawnMoves(board, moves, from, index, isPromotingRank);
        generatePawnCaptures(board, moves, from, ep, index, isPromotingRank);
    }

    private static void generateQuietPawnMoves(Board120 board, List<Integer> moves,
                                               int from, int index, Predicate<Byte> isPromotingRank) {
        boolean side = board.getSideToMove();
        int singlePush =  (side) ? SINGLE_PUSH : -SINGLE_PUSH;
        int doublePush =  (side) ? DOUBLE_PUSH : -DOUBLE_PUSH;
        int to = from + singlePush;
        int pc = board.getPieceOnSquare(to);

        if (pc == EMPT_SQ && !isPromotingRank.test((byte) from)) {
            int score = scoreQuietPawns(board, to);
            moves.add(Move.encodeMove(from, to, 0, Move.QUIET, index, score));
            if (isOnStartingRank(from, side)) {
                to = from + doublePush;
                if (board.getPieceOnSquare(to) == EMPT_SQ) {
                    moves.add(Move.encodeMove(from, to, 0, Move.DOUBLE_PAWN_PUSH, index, score));
                }
            }
        }
    }

    final static int[] WHITE_CAPTURES = {LEFTCAP, RIGHTCAP};
    final static int[] BLACK_CAPTURES = {-LEFTCAP, -RIGHTCAP};

    private static void generatePawnCaptures(Board120 board, List<Integer> moves, int from,
                                             int ep, int index, Predicate<Byte> isPromotingRank) {
        boolean side = board.getSideToMove();
        for (int c : (side) ? WHITE_CAPTURES : BLACK_CAPTURES) {
            int cap = from + c; // give index
            int piece = board.getPieceOnSquare(cap);
            if (piece != OFF_BOARD && !isPromotingRank.test((byte) from)) {
                // do not allow capture king
                if (isOpponentPiece(piece, side) && piece != ((side) ? BKING : WKING)) {
                    int score = (side) ? MVA_LVA_SCORE[0][piece + 127] :
                            MVA_LVA_SCORE[0][piece - 1];
                    moves.add(Move.encodeMove(from, cap, 0, Move.CAPTURE, index, score));
                }

                if (cap == ep) {
                    // prevent white from capturing "OWN" en Passant
                    if (side == WHITE && Board120Utils.isOnSecondRank((byte) from)) continue;
                    // prevent black from capturing OWN en Passant
                    if (side == BLACK && Board120Utils.isOnSeventhRank((byte) from)) continue;
                    moves.add(Move.encodeMove(from, ep,0, Move.EN_PASSANT,
                            index, MVA_LVA_SCORE[0][0])); // ep captures are always pawns
                }
            }
        }
        if (isOnPromoteRank(from, side)) generatePromotions(board, moves, from, index);
    }

    private static void generatePromotions(Board120 board, List<Integer> moves, int from, int index) {
        boolean side = board.getSideToMove();
        int sp = (side) ? SINGLE_PUSH : -SINGLE_PUSH;
        int to = from + sp;
        int promote = board.getPieceOnSquare(to);
        if (promote != OFF_BOARD) {
            if (promote == EMPT_SQ) addPromotionMoves(moves, from, to, index);
        }

        for (int offset : (side) ? WHITE_CAPTURES : BLACK_CAPTURES) {
            int cap = from + offset;
            byte piece = board.getPieceOnSquare(cap);
            if (piece != OFF_BOARD) {
                if (isOpponentPiece(piece, side) && piece != ((side) ? BKING : WKING)) {
                    int score = (side) ? MVA_LVA_SCORE[0][piece + 127] : MVA_LVA_SCORE[0][piece - 1];
                    addPromotionCaptureMoves(moves, from, cap, index, score);
                }
            }
        }
    }

    private static void addPromotionMoves(List<Integer> moves, int from, int to,  int index) {
        moves.add(Move.encodeMove(from, to, Q_PROMO,  Move.PROMOTION, index));
        moves.add(Move.encodeMove(from, to, R_PROMO,  Move.PROMOTION, index));
        moves.add(Move.encodeMove(from, to, B_PROMO,  Move.PROMOTION, index));
        moves.add(Move.encodeMove(from, to, KN_PROMO, Move.PROMOTION, index));
    }

    private static void addPromotionCaptureMoves(List<Integer> moves, int from, int to, int index, int score) {
        moves.add(Move.encodeMove(from, to,  Q_PROMO, Move.PROMOTION_CAPTURE, index, score+Q_PROMO));
        moves.add(Move.encodeMove(from, to,  R_PROMO, Move.PROMOTION_CAPTURE, index, score+R_PROMO));
        moves.add(Move.encodeMove(from, to,  B_PROMO, Move.PROMOTION_CAPTURE, index, score+B_PROMO));
        moves.add(Move.encodeMove(from, to, KN_PROMO, Move.PROMOTION_CAPTURE, index, score+KN_PROMO));
    }

    // orders by flag then score
    public static void sortMoves(List<Integer> unordered) {
        int sz = unordered.size();
        for (int i = 0; i < sz; i++) {
            int maxIndex = i;
            int flag = Move.getFlag(unordered.get(i));
            //int maxScore = (unordered.get(i) >> 24) & 0xff;
            int maxScore = Move.getScore(unordered.get(i));

            for (int j = i + 1; j < sz; j++) {
                int next = Move.getFlag(unordered.get(j));
                // int nextScore = (unordered.get(j) >>> 24) & 0xff;
                int nextScore = Move.getScore(unordered.get(j));
                if (next < flag) continue;
                if (next > flag) {
                    maxIndex = j;
                    flag = next;
                    maxScore = nextScore;
                } else if (nextScore > maxScore) {// equal flags
                    maxIndex = j;
                    maxScore = nextScore;
                }
            }

            if (i != maxIndex) {
                int temp = unordered.get(i);
                unordered.set(i, unordered.get(maxIndex));
                unordered.set(maxIndex, temp);
            }
        }
    }

    public static void sortGen(List<Integer> unordered) {
        unordered.sort((lhs, rhs) -> {
            int f1 = Move.getFlag(lhs);
            int f2 = Move.getFlag(rhs);
            if (f1 != f2) return -Integer.compare(f1, f2);

            int s1 = Move.getScore(lhs);
            int s2 = Move.getScore(rhs);
            return -Integer.compare(s1, s2);
        });
    }

    private static int scoreQuietPawns(Board120 board, int defSq) {
        int defenders = VectorAttack120.getDefenderCount(board, defSq);
        return (defenders != 0) ? 8 * defenders : 5;
    }

    private static void sortByFlag(List<Integer> unordered) {
        unordered.sort(Comparator.comparingInt(Move::getFlag));
    }

    private static boolean isOpponentPiece(int piece, boolean sideToPlay) {
        return sideToPlay ? piece < 0: piece > 0;
    }

    private static boolean isOnStartingRank(int square, boolean sideToPlay) {
        return sideToPlay ? Board120Utils.isOnSecondRank((byte) square) : Board120Utils.isOnSeventhRank((byte) square);
    }

    private static boolean isOnPromoteRank(int square, boolean sideToPlay) {
        return sideToPlay ?  Board120Utils.isOnSeventhRank((byte) square) : Board120Utils.isOnSecondRank((byte) square);
    }

}

