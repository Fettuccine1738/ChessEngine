package com.github.fehinti.piece;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;


import com.github.fehinti.board.Board;
import com.github.fehinti.board.BoardUtilities;
import com.github.fehinti.board.Move;
import com.github.fehinti.piece.Piece;

import static com.github.fehinti.piece.Piece.*;
import static com.github.fehinti.board.Board.getMailbox120Number;
import static com.github.fehinti.board.Board.getMailbox64Number;
import static com.github.fehinti.board.BoardUtilities.*;

public class PieceMove {

    // is piece type at this index a sliding piece (does the piece need to reset to its from
    // index to make the next move?/
    //    index like so --------->      { PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING_;
    final static boolean[] IS_SLIDING = { false, false, true, true, true, false};
    final static int[]   DIRECTIONS   = { 0,     8,      4,     4,    8,   8 };
    // board coordinates for board 120
    final static int[][] OFFSET_VECTOR_COORDINATES = {
            {   0,   0,  0,  0, 0,  0,  0,  0 },
            { -21, -19,-12, -8, 8, 12, 19, 21 }, /* KNIGHT */
            { -11,  -9,  9, 11, 0,  0,  0,  0 }, /* BISHOP */
            { -10,  -1,  1, 10, 0,  0,  0,  0 }, /* ROOK */
            { -11, -10, -9, -1, 1,  9, 10, 11 }, /* QUEEN */
            { -11, -10, -9, -1, 1,  9, 10, 11 }  /* KING */  };

    // board coordinates for board 64
    public final static int[][] VECTOR_COORDINATES = {
            {   0,   0,  0,  0, 0,  0,  0,  0 },
            { -17, -15, -10, -6,  6, 10, 15, 17}, /* KNIGHT */
            {  -9,  -7,  7,  9,   0, 0,  0,  0 }, /* BISHOP */
            {  -8,  -1,  1,  8, 0,  0,  0,   0 }, /* ROOK */
            {  -9,  -8, -7, -1,  1,  7,  8,   9}, /* QUEEN */
            {  -9,  -8, -7, -1, 1,  7,  8,   9 } /* KING */  };

    final static int SINGLE_PUSH64 = 8; // single push
    final static int SINGLE_PUSH = 10; // single push
    final static int DOUBLE_PUSH = 20; // valid on the second rank only
    final static int DOUBLE_PUSH64 = 16; // single push
    final static int LEFTCAP_64 = 9;
    final static int LEFTCAP = 11;
    final static int RIGHTCAP_64 = 7;
    final static int RIGHTCAP = 9;

    /**
     * @param board current position
     * @param moves list of integers encoding move information
     * @return pruned move list with moves that leave king in check (illegal moves ) removed. list contains only legal moves.
     */
    public static List<Integer> generateLegalMoves(Board board, List<Integer> moves) {
        if (moves.isEmpty() || board == null) throw new IllegalArgumentException("validate moves invoked with " +
                " null or empty board");
        int count = 0;
        int m;
        Iterator<Integer> iterator = moves.iterator();
        while (iterator.hasNext()) {
            m = iterator.next();
            board.make(m);
            if (AttackMap.isKingInCheck(board)) {
                iterator.remove(); // avoid concurrent Modification
            }
            board.unmake(m);
        }
        return moves;
    }

    final static int[] PIECES  = { 2, 3, 4, 5};
    private final static int IS_KING = 6;

    /**
     * @param board  current position
     * @return           a list of 32 bit ints encoding all move information
     */
    public static List<Integer> generatePseudoLegal(Board board) {
        if (board == null) throw new IllegalArgumentException("possible moves invoked with null board");
        boolean side = board.getSideToMove();

        List<Integer> moves = new ArrayList<>();

        int[] piecelist = (side) ? board.getWhitePieceList() : board.getBlackPieceList();
        int isEmpty = OFF_BOARD;
        int pie, square = 0, encoding = 0, isPawn = 0;

        for (int index = 0; index < piecelist.length; index++) {
            encoding = piecelist[index];
            if (encoding != isEmpty) {
                pie    = encoding >> 8; // equivalent to from Piece.Piece.getValue()
                square = encoding & 0xff;
                if (side) assert(pie > isEmpty);
                else assert(pie < isEmpty);

                int x = Math.abs(pie) - 1;

                // generate pawn moves separately
                if (x == isPawn) {
                    generatePseudoPawnMoves(board, moves, index, square);
                }
                else if (x != OFF_BOARD) {
                    // use piece value to index into offset vector
                    int[] vectorCoordinate120 = OFFSET_VECTOR_COORDINATES[x]; // board 120
                    boolean slides = IS_SLIDING[x];

                    // TODO:
                    // generate castles separately if available
                    if (Math.abs(pie) == IS_KING && board.canSideCastle(side)) {
                        generateCastle(board, side, moves, index);
                    }

                    int from = square;
                    // what is the square mailbox 64's number
                    int newSquare = 0;
                    int N = DIRECTIONS[x]; // number of ray / knight piesible directions
                    for (int i = 0; i < N; i++) {
                        square = from; // save copy to reset to every iteration
                        // if it is a sliding piece
                        while (true) {
                            newSquare = getMailbox120Number(getMailbox64Number(square) + vectorCoordinate120[i]);
                            if (newSquare == OFF_BOARD) break; // off board
                            Piece pieceOnBoard = board.getPieceOnBoard(newSquare);
                            if (pieceOnBoard != EMPTY) {
                                boolean sameSide = pieceOnBoard.isWhite();
                                // do not capture if it is a King (excluded from pseudo moves)
                                if (Math.abs(pieceOnBoard.getValue()) == IS_KING) break;
                                if (sameSide == !side) {
                                    moves.add(Move.encodeMove(from, newSquare,
                                            pieceOnBoard.getValue(), 0, Move.FLAG_CAPTURE, index));
                                }
                                break;
                            }
                            else {
                                moves.add(Move.encodeMove(from, newSquare,0,
                                        0, Move.FLAG_QUIET, index));
                            }
                            if(!slides) break;
                            square = newSquare; // advance square
                        }
                    }
                }
            }
        }
        return moves;
    }


    /**
     * @param board  current pieition.
     * @param side   side to move BLACK or WHITE.
     * @param moves  list of move information encoded into integers
     * @param index  index of piece in the piece list, needed to preserver move generation ordering
     */
    private static void generateCastle(Board board, boolean side, List<Integer> moves, int index) {
         if (!board.canSideCastle(side)) return;
         byte rights = board.getCastlingRights();
         boolean longCastle = (side) ? board.canWhiteCastleQueenside(rights) : board.canBlackCastleQueenside(rights);
         boolean shortCastle = (side) ? board.canWhiteCastleKingside(rights) : board.canBlackCastleKingside(rights);

         if (longCastle) {
             if (side == WHITE) {
                 if (board.getPieceOnBoard(A_1) == WHITE_ROOK
                         && board.getPieceOnBoard(B_1) == EMPTY
                         && board.getPieceOnBoard(C_1) == EMPTY
                         && board.getPieceOnBoard(D_1) == EMPTY
                   && !AttackMap.isSquareAttacked(board, C_1, AttackMap.BEFORE)
                   && !AttackMap.isSquareAttacked(board, D_1, AttackMap.BEFORE)) {
                     assert(Math.abs(board.getPieceOnBoard(E_1).getValue()) == IS_KING);
                     assert(!AttackMap.isKingInCheck(board));
                     // move king towards rook
                     moves.add(Move.encodeMove(E_1, C_1, 0, 0, Move.FLAG_CASTLE, index));
                 }
             }
             else { // black
                 if (board.getPieceOnBoard(A_8) == BLACK_ROOK
                         && board.getPieceOnBoard(B_8) == EMPTY
                         && board.getPieceOnBoard(C_8) == EMPTY
                         && board.getPieceOnBoard(D_8) == EMPTY
                         && !AttackMap.isSquareAttacked(board, D_8, AttackMap.BEFORE)
                         && !AttackMap.isSquareAttacked(board, C_8, AttackMap.BEFORE)) {
                     assert(Math.abs(board.getPieceOnBoard(E_8).getValue()) == IS_KING);
                     assert(!AttackMap.isKingInCheck(board));// does not make sense
                     moves.add(Move.encodeMove(E_8, C_8, 0, 0, Move.FLAG_CASTLE, index));
                 }
             }
         }

         if (shortCastle) {
             if (side == WHITE) {
                 if (board.getPieceOnBoard(H_1) == WHITE_ROOK
                         && board.getPieceOnBoard(G_1) == EMPTY
                         && board.getPieceOnBoard(F_1) == EMPTY
                         && !AttackMap.isSquareAttacked(board, G_1, AttackMap.BEFORE)
                         && !AttackMap.isSquareAttacked(board, F_1, AttackMap.BEFORE)) {
                     assert(Math.abs(board.getPieceOnBoard(E_1).getValue()) == IS_KING);
                     assert(!AttackMap.isKingInCheck(board));
                     moves.add(Move.encodeMove(E_1, G_1, 0, 0, Move.FLAG_CASTLE, index));
                 }
             }
             else {
                 if (board.getPieceOnBoard(H_8) == BLACK_ROOK &&
                         board.getPieceOnBoard(G_8) == EMPTY
                         && board.getPieceOnBoard(F_8) == EMPTY
                         // cannot castle through checks
                         && !AttackMap.isSquareAttacked(board, G_8, AttackMap.BEFORE)
                         && !AttackMap.isSquareAttacked(board, F_8, AttackMap.BEFORE)) {
                     assert(!AttackMap.isKingInCheck(board));// does not make sense
                     assert(Math.abs(board.getPieceOnBoard(E_8).getValue()) == IS_KING);
                     moves.add(Move.encodeMove(E_8, G_8, 0, 0, Move.FLAG_CASTLE, index));
                 }
             }
         }
    }

    private static void generatePseudoPawnMoves(Board board, List<Integer> moves, int index, int from) {
        boolean side = board.getSideToMove();
        int ep = board.getEnPassant();
        Predicate<Byte> isPromotingRank = (side) ?
                BoardUtilities::isOnSeventhRank : BoardUtilities::isOnSecondRank;

       generateQuietPawnMoves(board, moves, from, index, isPromotingRank);
       generatePawnCaptures(board, moves, from, ep, index, isPromotingRank);
    }

    private static void generateQuietPawnMoves(Board board, List<Integer> moves,
            int from, int index, Predicate<Byte> isPromotingRank) {

            boolean side = board.getSideToMove();
            int singlePush =  (side) ? SINGLE_PUSH : -SINGLE_PUSH;
            int doublePush =  (side) ? DOUBLE_PUSH : -DOUBLE_PUSH;
            int to = getMailbox120Number(getMailbox64Number(from) + singlePush);
            if (to != OFF_BOARD && !isPromotingRank.test((byte) from) && board.getPieceOnBoard(to) == EMPTY) {
            moves.add(Move.encodeMove(from, to, 0, 0, Move.FLAG_QUIET, index));

            if (isOnStartingRank(from, side)) {
                to = getMailbox120Number(getMailbox64Number(from) + doublePush);
                if (board.getPieceOnBoard(to) == EMPTY) {
                    moves.add(Move.encodeMove(from, to, 0, 0, Move.FLAG_DOUBLE_PAWN_PUSH, index));
                }
            }
        }
    }

    final static int[] WHITE_CAPTURES = {LEFTCAP, RIGHTCAP};
    final static int[] BLACK_CAPTURES = {-LEFTCAP, -RIGHTCAP};

    private static void generatePawnCaptures(Board board, List<Integer> moves, int from,
                                            int ep, int index, Predicate<Byte> isPromotingRank) {
        boolean side = board.getSideToMove();
        for (int c : (side) ? WHITE_CAPTURES : BLACK_CAPTURES) {
            int cap = getMailbox120Number(getMailbox64Number(from) + c);
            // generate non promotion captures
            if (cap != OFF_BOARD && !isPromotingRank.test((byte) from)) {
                Piece piece = board.getPieceOnBoard(cap);
                // do not capture king
                if (isOpponentPiece(piece, side) && Math.abs(piece.getValue()) != IS_KING) {
                    moves.add(Move.encodeMove(from, cap, piece.getValue(), 0, Move.FLAG_CAPTURE, index));
                }

                if (cap == ep) { // capture enPassant
                    // prevent white from capturing en Passant
                    if (side == WHITE && BoardUtilities.isOnSecondRank((byte) from)) continue;
                    // prevent black from capturing en Passant
                    if (side == BLACK && BoardUtilities.isOnSeventhRank((byte) from)) continue;
                    Piece epPiece;
                    if (side == WHITE) epPiece = board.getPieceOnBoard(ep - SINGLE_PUSH64);
                    else epPiece = board.getPieceOnBoard(ep + SINGLE_PUSH64);
                    assert(Math.abs(epPiece.getValue()) == 1);
                    moves.add(Move.encodeMove(from, ep, epPiece.getValue(), 0, Move.FLAG_EN_PASSANT, index));
                }
            }
        }
        if (isOnPromoteRank(from, side)) generatePromotions(board, moves, from, index);
    }

    private static void generatePromotions(Board board, List<Integer> moves, int from, int index) {
        boolean side = board.getSideToMove();
        int sp = (side) ? SINGLE_PUSH : -SINGLE_PUSH;
        int promote = getMailbox120Number(getMailbox64Number(from) + sp);
        if (promote != OFF_BOARD && board.getPieceOnBoard(promote) == EMPTY) {
            addPromotionMoves(moves, from, promote, 0, side, index);
        }

        for (int offset : (side) ? WHITE_CAPTURES : BLACK_CAPTURES) {
            int cap = getMailbox120Number(getMailbox64Number(from) + offset);
            if (cap != OFF_BOARD) {
                Piece piece = board.getPieceOnBoard(cap);
                if (isOpponentPiece(piece, side) && Math.abs(piece.getValue()) != IS_KING) {
                    addPromotionMoves(moves, from, cap, piece.getValue(), side, index);
                }
            }
        }

    }

    private static void addPromotionMoves(List<Integer> moves, int from, int to, int captured, boolean side, int index) {
        // promote to other pieces except pawn(0) and King (5)
        for (int i : PIECES) {
            moves.add(Move.encodeMove(from, to, captured, (side) ? i : -i, Move.FLAG_PROMOTION, index));
        }
        //Arrays.stream((side == WHITE) ? WHITE_PIECES : BLACK_PIECES)
                //.skip(1) // skip pawn
                //.limit(4) // limit to pieces less than KING
                //.map(Piece::getValue)
                //.forEach(value -> Move.encodeMove(from, to, captured, value, Move.FLAG_PROMOTION));
    }

    private static boolean isOpponentPiece(Piece piece, boolean sideToPlay) {
        return sideToPlay ? piece.isBlack() : piece.isWhite();
    }

    private static boolean isOnStartingRank(int square, boolean sideToPlay) {
        return sideToPlay ? isOnSecondRank((byte) square) : isOnSeventhRank((byte) square);
    }

    private static boolean isOnPromoteRank(int square, boolean sideToPlay) {
        return sideToPlay ?  isOnSeventhRank((byte) square) : isOnSecondRank((byte) square);
    }
}



