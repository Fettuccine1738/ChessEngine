package piece;

import board.Board;
import board.BoardUtilities;
import board.Move;
import board.PieceType;
import edu.princeton.cs.algs4.In;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import static board.Board.getMailbox120Number;
import static board.Board.getMailbox64Number;
import static board.BoardUtilities.*;
import static board.PieceType.*;

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
    final static int[][] VECTOR_COORDINATES = {
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
    public static List<Integer> validateMoves(Board board, List<Integer> moves) {
       //  Board copyBoard = new Board(board);
        if (moves.isEmpty() || board == null) throw new IllegalArgumentException("validate moves invoked with " +
                " null or empty board");
        int count = 0;
        int m;
        Iterator<Integer> iterator = moves.iterator();
        while (iterator.hasNext()) {
            m = iterator.next();
            board.make(m);
            //System.out.println(board + "\n");
            if (AttackMap.isKingInCheck(board)) {
                iterator.remove(); // avoid concurrent Modification
              //   System.out.println("\n" + m + " leaves king in check");
            }
            board.unmake(m);
            // System.out.println(board + "\n");
        }
        return moves;
    }

     public static List<Integer> possibleMoves(Board board, boolean sideToPlay) {
        List<Integer> moves = new ArrayList<>();
        if (board == null) throw new IllegalArgumentException("possible King moves invoked with null");
        PieceType[] pieces;
        int floor, ceiling;

        if (sideToPlay) { // if white's turn
            pieces = new PieceType[] {WHITE_PAWN, WHITE_KNIGHT, WHITE_BISHOP, WHITE_ROOK, WHITE_QUEEN, WHITE_KING};
        }
        else {
            pieces = new PieceType[] {BLACK_PAWN, BLACK_KNIGHT, BLACK_BISHOP, BLACK_ROOK, BLACK_QUEEN, BLACK_KING};
        }

        for (PieceType piece : pieces) {
            floor   = getPieceListFloor(piece);
            ceiling = getPieceListCeiling(piece);
            moves.addAll(Objects.requireNonNull(generatePseudoLegal(board, sideToPlay, floor, ceiling, piece)));
        }
         System.out.println("SZ " + moves.size());
        return moves;
    }


    /**
     * @param board  current position
     * @param sideToPlay WHITE (true) or BLACK (false) represents sides to play
     * @param floor      avoids scanning the entire board, lowerbound index in piece-list
     * @param ceiling    upperbound index piece-list
     * @param piece      Piece to move
     * @return           a list of 32 bit ints encoding all move information
     */
    public static List<Integer> generatePseudoLegal(Board board, boolean sideToPlay, int floor, int ceiling, PieceType piece) {
        if (board == null) throw new IllegalArgumentException("possible moves invoked with null board");
        if (piece == null) throw new IllegalArgumentException("pseudolega genearate with null piece");

        List<Integer> moves = new ArrayList<>();
        // generate pawn moves separately
        if (Math.abs(piece.getValue()) == 1) {
            generatePseudoPawnMoves(board, moves, sideToPlay);
            return moves;
        }
        // generate castles
        generateCastle(board, sideToPlay, moves);

        int[] piecelist = (sideToPlay) ? board.getWhitePieceList() : board.getBlackPieceList();
        // use piece value to index into offset vector
        int x = Math.abs(piece.getValue()) - 1;
        int[] vectorCoordinate120 = OFFSET_VECTOR_COORDINATES[x]; // board 120
        int[] vectorCoordinate64  = VECTOR_COORDINATES[x];
        boolean slides = IS_SLIDING[x];
        int empty = 0;

        int pos, square = 0, encoding, mailbox64 = 0, mailbox120 = 0;
        // null move, add current square as a move
        for (int index = floor; index < ceiling; index++) { //iterate through piece list
            encoding = piecelist[index];
            if (encoding != empty) {
                pos    = encoding >> 8; // equivalent to from PieceType.Piece.getValue()
                square = encoding & 0xff;
                if (sideToPlay) assert(pos > empty);
                else assert(pos < empty);

                int from = square;
                // what is the square mailbox 64's number
                int newSquare = 0;
                int N = DIRECTIONS[x]; // number of ray / knight possible directions
                for (int i = 0; i < N; i++) {
                    square = from; // save copy to reset to every iteration
                    // if it is a sliding piece
                    while (true) {
                        newSquare = getMailbox120Number(getMailbox64Number(square) + vectorCoordinate120[i]);
                        if (newSquare == OFF_BOARD) break; // off board
                        PieceType pieceOnBoard = board.getPieceOnBoard(newSquare);
                        if (pieceOnBoard != EMPTY) {
                            boolean sameSide = pieceOnBoard.isWhite();
                            if (sameSide == !sideToPlay) {
                                moves.add(Move.encodeMove(from, newSquare, pieceOnBoard.getValue(), 0, Move.FLAG_CAPTURE));
                            }
                            break;
                        }
                        else {
                            moves.add(Move.encodeMove(from, newSquare, pieceOnBoard.getValue(), 0, Move.FLAG_QUIET));
                        }
                        if(!slides) break;
                        square = newSquare; // advance square
                    }
                }
            }
        }
        return moves;
    }


    /**
     * @param board  current position.
     * @param side   side to move BLACK or WHITE.
     * @param moves  list of Ints to put moves in.
     */
    public static void generateCastle(Board board, boolean side, List<Integer> moves) {
         byte rights = board.getCastlingRights();
         boolean longCastle = (side) ? board.canWhiteCastleQueenside(rights) : board.canBlackCastleQueenside(rights);
         boolean shortCastle = (side) ? board.canWhiteCastleKingside(rights) : board.canBlackCastleKingside(rights);

         if (longCastle) {
             if (side == WHITE) {
                 if (board.getPieceOnBoard(0) == WHITE_ROOK && board.getPieceOnBoard(1) == EMPTY
                         && board.getPieceOnBoard(2) == EMPTY && board.getPieceOnBoard(3) == EMPTY) {
                     moves.add(Move.encodeMove(4, 2, 0, 0, Move.FLAG_CASTLE));
                 }
             }
             else {
                 if (board.getPieceOnBoard(63) == BLACK_ROOK && board.getPieceOnBoard(62) == EMPTY
                         && board.getPieceOnBoard(61) == EMPTY && board.getPieceOnBoard(60) == EMPTY) {
                     moves.add(Move.encodeMove(59, 61, 0, 0, Move.FLAG_CASTLE));
                 }
             }
         }
         if (shortCastle) {
             if (side == WHITE) {
                 if (board.getPieceOnBoard(7) == WHITE_ROOK && board.getPieceOnBoard(6) == EMPTY
                         && board.getPieceOnBoard(5) == EMPTY) {
                     moves.add(Move.encodeMove(4, 6, 0, 0, Move.FLAG_CASTLE));
                 }
             }
             else {
                 if (board.getPieceOnBoard(58) == BLACK_ROOK && board.getPieceOnBoard(57) == EMPTY
                         && board.getPieceOnBoard(56) == EMPTY) {
                     moves.add(Move.encodeMove(59, 57, 0, 0, Move.FLAG_CASTLE));
                 }
             }
         }
    }

    // validate pseudo legal castle moves to see if  an opponent's piece
    // attacks a square between the from and to square
    private void validateCastle(int from, int to, boolean side) {
         if (side == WHITE) {
             assert(from == 4);
             assert(to == 6 || to == 2);
         }
         if (side == BLACK) {
             assert(from == 59);
             assert(to == 57 || to == 61);
         }
         int start = from;
         int end = to;
         for (; start < end; start++) {
             // boolean sqrAttacked = AttackMap.isSquareAttacked();
         }
    }



    public static void generatePseudoPawnMoves(Board board, List<Integer> moves, boolean sideToPlay) {
        int[] piecelist = (sideToPlay) ? board.getWhitePieceList() : board.getBlackPieceList();
        int start = getPieceListFloor(WHITE_PAWN);
        int end = getPieceListCeiling(WHITE_PAWN); // same index and range for both black and white
        int ep = board.getEnPassant();
        int skip = EMPTY.getValue();
        int singlePush =  sideToPlay == WHITE ? SINGLE_PUSH : -SINGLE_PUSH;
        int doublePush =  sideToPlay == WHITE ? DOUBLE_PUSH : -DOUBLE_PUSH;
        int leftCapture =  sideToPlay == WHITE ? LEFTCAP : -LEFTCAP;
        int rightCapture = sideToPlay == WHITE ? RIGHTCAP : -RIGHTCAP;
        Predicate<Byte> isPromotingRank = (sideToPlay == WHITE) ?
                BoardUtilities::isOnSeventhRank : BoardUtilities::isOnSecondRank;
        int empty = 0;

        for (; start < end; start++) {
            if (piecelist[start] == empty) continue;
            int from = piecelist[start] & 0xff; // extract from square
            assert(board.getPieceOnBoard(from) != EMPTY);

            generateQuietPawnMoves(board, moves, from, singlePush, doublePush, sideToPlay, isPromotingRank);
            generatePawnCaptures(board, moves, from, leftCapture, rightCapture, ep, sideToPlay, isPromotingRank);
        }
    }

    private static void generateQuietPawnMoves(Board board, List<Integer> moves, int sq, int singlePush, int doublePush, boolean sideToPlay, Predicate<Byte> isPromotingRank) {
        int to = getMailbox120Number(getMailbox64Number(sq) + singlePush);
        if (to != OFF_BOARD && !isPromotingRank.test((byte) sq) && board.getPieceOnBoard(to) == EMPTY) {
            moves.add(Move.encodeMove(sq, to, 0, 0, Move.FLAG_QUIET));

            if (isOnStartingRank(sq, sideToPlay)) {
                to = getMailbox120Number(getMailbox64Number(sq) + doublePush);
                if (board.getPieceOnBoard(to) == EMPTY) {
                    moves.add(Move.encodeMove(sq, to, 0, 0, Move.FLAG_DOUBLE_PAWN_PUSH));
                }
            }
        }
    }

    private static void generatePawnCaptures(Board board, List<Integer> moves, int from,
                                             int leftCapture, int rightCapture, int ep, boolean side, Predicate<Byte> isPromotingRank) {
        int[] captures = (side) ? new int[]{LEFTCAP, RIGHTCAP} : new int[]{ -LEFTCAP, -RIGHTCAP};
        for (int c : captures) {
            int cap = getMailbox120Number(getMailbox64Number(from) + c);
            // generate non promotion captures
            if (cap != OFF_BOARD && !isPromotingRank.test((byte) from)) {
                PieceType piece = board.getPieceOnBoard(cap);
                if (isOpponentPiece(piece, side)) {
                    moves.add(Move.encodeMove(from, cap, piece.getValue(), 0, Move.FLAG_CAPTURE));
                }
                if (cap == ep) { // capture enPassant
                    PieceType epPiece;
                    if (side == WHITE) epPiece = board.getPieceOnBoard(cap - SINGLE_PUSH64);
                    else epPiece = board.getPieceOnBoard(cap + SINGLE_PUSH64);
                    moves.add(Move.encodeMove(from, cap, epPiece.getValue(), 0, Move.FLAG_EN_PASSANT));
                }
            }
        }
        if (isOnPromoteRank(from, side)) generatePromotions(board, moves, from, side);
    }

    private static void generatePromotions(Board board, List<Integer> moves, int from, boolean side) {
        int[] offsets = (side) ? new int[]{LEFTCAP, RIGHTCAP} : new int[]{ -LEFTCAP, -RIGHTCAP};
        int sp = (side) ? SINGLE_PUSH : -SINGLE_PUSH;
        int promote = getMailbox120Number(getMailbox64Number(from) + sp);
        for (int offset : offsets) {
            int cap = getMailbox120Number(getMailbox64Number(from) + offset);
            if (cap != OFF_BOARD) {
                PieceType piece = board.getPieceOnBoard(cap);
                if (isOpponentPiece(piece, side)) {
                    addPromotionMoves(moves, from, cap, piece.getValue(), side);
                }
            }
        }
        if (promote != OFF_BOARD && board.getPieceOnBoard(promote) == EMPTY) {
            addPromotionMoves(moves, from, promote, 0, side);
        }
    }

    private static void addPromotionMoves(List<Integer> moves, int from, int to, int captured, boolean side) {
        PieceType[] promotionPieces = (side == WHITE) ? new PieceType[]{WHITE_KNIGHT, WHITE_BISHOP, WHITE_ROOK, WHITE_QUEEN}
                : new PieceType[]{BLACK_KNIGHT, BLACK_BISHOP, BLACK_ROOK, BLACK_QUEEN};

        for (PieceType promotion : promotionPieces) {
            moves.add(Move.encodeMove(from, to, captured, promotion.getValue(), Move.FLAG_PROMOTION));
        }
    }

    private static boolean isOpponentPiece(PieceType piece, boolean sideToPlay) {
        return sideToPlay ? piece.isBlack() : piece.isWhite();
    }

    private static boolean isOnStartingRank(int square, boolean sideToPlay) {
        return sideToPlay ? isOnSecondRank((byte) square) : isOnSeventhRank((byte) square);
    }

    private static boolean isOnPromoteRank(int square, boolean sideToPlay) {
        return sideToPlay ?  isOnSeventhRank((byte) square) : isOnSecondRank((byte) square);
    }
}



