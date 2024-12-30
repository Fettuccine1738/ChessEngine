package piece;

import board.Board;
import board.Move;
import board.MoveType;
import board.PieceType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static board.Board.getMailbox120Number;
import static board.Board.getMailbox64Number;
import static board.BoardUtilities.*;
import static board.PieceType.*;

public class PieceMove {
    // is piecetype at this index a sliding piece (does the piece need to reset to its from
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
            ceiling = getPieceListSize(piece);
            moves.addAll(Objects.requireNonNull(generatePseudoLegal(board, sideToPlay, floor, ceiling, piece)));
        }
         System.out.println("SZ " + moves.size());
        return moves;
    }


    // legal moves ?? play move then look at !side (opponent), if current square is in move list of
    // !side remove move.

    // castling ??
    public static List<Integer> generatePseudoLegal(Board board, boolean sideToPlay, int floor, int ceiling, PieceType piece) {
        if (board == null) throw new IllegalArgumentException("possible moves invoked with null board");
        if (piece == null) throw new IllegalArgumentException("pseudolega genearate with null piece");

        List<Integer> moves = new ArrayList<>();
        // generate pawn moves separately
        if (Math.abs(piece.getValue()) == 1) moves.addAll(generatePseudoPawnMoves(board, sideToPlay, floor, ceiling, piece, moves));

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
                            // moves.add(new Move((byte) (from), (byte) (square + vectorCoordinate64[i]), sideToPlay, piece, MoveType.NORMAL));
                            moves.add(Move.encodeMove(from, newSquare, pieceOnBoard.getValue(), 0, Move.FLAG_QUIET));
                        }
                        if(!slides) break;
                        square = newSquare; // advance square
                    }
                }
            }
        }
        // System.out.println(moves.size());
        return moves;
    }


    public static List<Integer> generatePseudoPawnMoves(Board board, boolean sideToPlay, int floor, int ceiling, PieceType piece, List<Integer> moves) {
        if (board == null) throw new IllegalArgumentException("possible pawn moves invoked with null board");
        if (piece == null) throw new IllegalArgumentException("pawn pseudolegal invoked with null piece");

        int[] piecelist = (sideToPlay) ? board.getWhitePieceList() : board.getBlackPieceList();
        int from, to;
        int captureLeft = 0;
        int captureRight = 0;
        boolean whitePush = false;
        boolean blackPush = false;

        for (int index = floor; index < ceiling; index++) {
            if (piecelist[index] == EMPTY.getValue()) continue; // skip if there is no piece in index
            from = piecelist[index] & 0xff; // get current square
            assert(board.getPieceOnBoard(from) != EMPTY);
            generateAllPawnMoves(board, from, sideToPlay, moves);
        }
        return moves;
    }


    private static void generateAllPawnMoves(Board board, int from, boolean side, List<Integer> moves) {
         if (WHITE) {
             generateWhitePawnCaptures(board, from, side, moves);
             generateQuietWPawnMoves(board, from, side, moves);
             generateEnPassant(board, from, WHITE, moves);
         }
         else {
             generateBlackPawnCaptures(board, from, side, moves);
             generateQuietBPawnMoves(board, from, side, moves);
             generateEnPassant(board, from, BLACK, moves);
         }
    }

    private static void generateEnPassant(Board b, int from, boolean side, List<Integer> moves) {
         int enPassant = b.getEnPassant();
         int wP = WHITE_PAWN.getValue();
         int bP = BLACK_PAWN.getValue();
         if (WHITE) {
             if (enPassant != OFF_BOARD && (from + LEFTCAP_64) == enPassant) {
                 // moves.add(new Move((byte) from, (byte) (from + LEFTCAP_64), WHITE, WHITE_PAWN, MoveType.ENPASSANT));
                 moves.add(Move.encodeMove(from, from + LEFTCAP_64, bP , 0, Move.FLAG_EN_PASSANT));
             }
             if (enPassant != OFF_BOARD && (from + (byte) (from +  RIGHTCAP_64) == enPassant)) {
                 // moves.add(new Move((byte) from, (byte) (from + LEFTCAP_64), WHITE, WHITE_PAWN, MoveType.ENPASSANT));
                 moves.add(Move.encodeMove(from, from + LEFTCAP_64, bP, 0, Move.FLAG_EN_PASSANT));
             }
         } else {
             if (enPassant != OFF_BOARD && (from - LEFTCAP_64) == enPassant) {
                 // moves.add(new Move((byte) from, (byte) (from - LEFTCAP_64), BLACK, WHITE_PAWN, MoveType.ENPASSANT));
                 moves.add(Move.encodeMove(from, from - LEFTCAP_64, wP, 0, Move.FLAG_EN_PASSANT));
             }
             if (enPassant != OFF_BOARD && (from + (byte) (from - RIGHTCAP_64) == enPassant)) {
                 // moves.add(new Move((byte) from, (byte) (from + LEFTCAP_64), BLACK, WHITE_PAWN, MoveType.ENPASSANT));
                 moves.add(Move.encodeMove(from, from - RIGHTCAP_64, wP, 0, Move.FLAG_EN_PASSANT));
             }
         }
    }

    private static void generateQuietWPawnMoves(Board b, int from, boolean side, List<Integer> moves) {
         // add enpassant
        int offset = getMailbox120Number(from + SINGLE_PUSH);
        if (offset != OFF_BOARD) {
            PieceType p = b.getPieceOnBoard(from + SINGLE_PUSH64);
            int to = from + SINGLE_PUSH64;
            // if immediate square isn't empty none of these should be possible
            if (p == EMPTY) {
                // double pawn push from second rank
                if (isOnSecondRank((byte) from) && b.getPieceOnBoard(from + DOUBLE_PUSH64) == EMPTY) {
                    to = from + DOUBLE_PUSH64;
                    // moves.add(new Move((byte) from, (byte) to, WHITE, WHITE_PAWN, MoveType.NORMAL));
                    moves.add(Move.encodeMove(from, to, 0, 0, Move.FLAG_DOUBLE_PAWN_PUSH));
                }
                else if (isOnSeventhRank((byte) from)) { // if on 7th rank move to 8th for promotion
                    // moves.add(new Move((byte) from, (byte) to, WHITE, WHITE_PAWN, MoveType.PROMOTION));
                    // promote to major white pieces
                    moves.add(Move.encodeMove(from, to, 0, WHITE_QUEEN.getValue(), Move.FLAG_PROMOTION));
                    moves.add(Move.encodeMove(from, to, 0, WHITE_ROOK.getValue(), Move.FLAG_PROMOTION));
                    moves.add(Move.encodeMove(from, to, 0, WHITE_KNIGHT.getValue(), Move.FLAG_PROMOTION));
                    moves.add(Move.encodeMove(from, to, 0, WHITE_BISHOP.getValue(), Move.FLAG_PROMOTION));
                    return;
                }
                // moves.add(new Move((byte) from, (byte) to, WHITE, WHITE_PAWN, MoveType.NORMAL));
                moves.add(Move.encodeMove(from, to, 0, 0, Move.FLAG_QUIET));
            }
        }
    }


    private static void generateQuietBPawnMoves(Board b, int from, boolean side, List<Integer> moves) {
        int offset = getMailbox120Number(from - SINGLE_PUSH);
        if (offset != OFF_BOARD) {
            PieceType p = b.getPieceOnBoard(from - SINGLE_PUSH64);
            int to = from - SINGLE_PUSH64;
            if (p == EMPTY) {
                if (isOnSeventhRank((byte) from) && b.getPieceOnBoard(from - DOUBLE_PUSH64) == EMPTY) {
                    to = from - DOUBLE_PUSH64;
                    // moves.add(new Move((byte) from, (byte) to, BLACK, BLACK_PAWN, MoveType.NORMAL));
                    moves.add(Move.encodeMove(from, to, 0, 0, Move.FLAG_DOUBLE_PAWN_PUSH));
                }
                else if (isOnSecondRank((byte) from)) {
                    // moves.add(new Move((byte) from, (byte) to, BLACK, BLACK_PAWN, MoveType.PROMOTION));
                    // promote to major black pieces
                    moves.add(Move.encodeMove(from, to, 0, BLACK_QUEEN.getValue(), Move.FLAG_PROMOTION));
                    moves.add(Move.encodeMove(from, to, 0, BLACK_ROOK.getValue(), Move.FLAG_PROMOTION));
                    moves.add(Move.encodeMove(from, to, 0, BLACK_KNIGHT.getValue(), Move.FLAG_PROMOTION));
                    moves.add(Move.encodeMove(from, to, 0, BLACK_BISHOP.getValue(), Move.FLAG_PROMOTION));
                    return;
                }
                // moves.add(new Move((byte) from, (byte) to, BLACK, BLACK_PAWN, MoveType.NORMAL));
                moves.add(Move.encodeMove(from, to, 0, 0, Move.FLAG_QUIET));
            }
        }
    }

    private static void generateWhitePawnCaptures(Board b, int from, boolean side, List<Integer> moves) {
         if (moves == null) throw new IllegalArgumentException("Generate WPawn invoked with null list");
         int left = getMailbox120Number(from + LEFTCAP);
         int right = getMailbox120Number(from + RIGHTCAP);
         PieceType p;
         if (left != OFF_BOARD) {
             p = b.getPieceOnBoard(left + LEFTCAP_64);
             if (p.isBlack()) {
                 if (isOnSeventhRank((byte) from)) {
                     int to = left + LEFTCAP_64;
                     // moves.add(new Move((byte) from, (byte) (from + LEFTCAP_64), WHITE, WHITE_PAWN, MoveType.PROMOTION_CAPTURE));
                     // moves.add(Move.encodeMove(from, from + LEFTCAP_64, 0, 0, Move.FLAG_QUIET));
                     moves.add(Move.encodeMove(from, to, p.getValue(), WHITE_QUEEN.getValue(), Move.FLAG_PROMOTION));
                     moves.add(Move.encodeMove(from, to, p.getValue(), WHITE_ROOK.getValue(), Move.FLAG_PROMOTION));
                     moves.add(Move.encodeMove(from, to, p.getValue(), WHITE_KNIGHT.getValue(), Move.FLAG_PROMOTION));
                     moves.add(Move.encodeMove(from, to, p.getValue(), WHITE_BISHOP.getValue(), Move.FLAG_PROMOTION));
                 }
                 else {
                     // moves.add(new Move((byte) from, (byte) (from + LEFTCAP_64), WHITE, WHITE_PAWN, MoveType.CAPTURE));
                     moves.add(Move.encodeMove(from, from + LEFTCAP_64, 0, 0, Move.FLAG_CAPTURE));
                 }
             }
         }

         if (right != OFF_BOARD) {
             p = b.getPieceOnBoard(left + RIGHTCAP_64);
             if (p.isBlack()) {
                 if (isOnSeventhRank((byte) from)) {
                     int to = from + RIGHTCAP_64;
                     moves.add(Move.encodeMove(from, to, p.getValue(), WHITE_QUEEN.getValue(), Move.FLAG_PROMOTION));
                     moves.add(Move.encodeMove(from, to, p.getValue(), WHITE_ROOK.getValue(), Move.FLAG_PROMOTION));
                     moves.add(Move.encodeMove(from, to, p.getValue(), WHITE_KNIGHT.getValue(), Move.FLAG_PROMOTION));
                     moves.add(Move.encodeMove(from, to, p.getValue(), WHITE_BISHOP.getValue(), Move.FLAG_PROMOTION));
                 } else {
                     // moves.add(new Move((byte) from, (byte) (from + RIGHTCAP_64), WHITE, WHITE_PAWN, MoveType.CAPTURE));
                     moves.add(Move.encodeMove(from, from + RIGHTCAP_64, p.getValue(), 0, Move.FLAG_QUIET));
                 }
             }
         }
    }

    private static void generateBlackPawnCaptures(Board b, int from, boolean side, List<Integer> moves) {
        if (moves == null) throw new IllegalArgumentException("Generate WPawn invoked with null list");
        int left = getMailbox120Number(from - LEFTCAP);
        int right = getMailbox120Number(from - RIGHTCAP);
        PieceType p;
        if (left != OFF_BOARD) {
            p = b.getPieceOnBoard(left - LEFTCAP_64);
            if (p.isWhite()) {
                if (isOnSecondRank((byte) from)) {// promote to a capture
                    // moves.add(new Move((byte) from, (byte) (from - LEFTCAP_64), BLACK, BLACK_PAWN, MoveType.PROMOTION_CAPTURE));
                    int to = from - LEFTCAP_64;
                    moves.add(Move.encodeMove(from, to, p.getValue(), BLACK_QUEEN.getValue(), Move.FLAG_PROMOTION));
                    moves.add(Move.encodeMove(from, to, p.getValue(), BLACK_ROOK.getValue(), Move.FLAG_PROMOTION));
                    moves.add(Move.encodeMove(from, to, p.getValue(), BLACK_KNIGHT.getValue(), Move.FLAG_PROMOTION));
                    moves.add(Move.encodeMove(from, to, p.getValue(), BLACK_BISHOP.getValue(), Move.FLAG_PROMOTION));
                }
                else {
                    // moves.add(new Move((byte) from, (byte) (from - LEFTCAP_64), BLACK, BLACK_PAWN, MoveType.CAPTURE));
                    moves.add(Move.encodeMove(from, from - LEFTCAP_64, p.getValue(),0, Move.FLAG_QUIET));
                }
            }
        }

        if (right != OFF_BOARD) {
            p = b.getPieceOnBoard(left - RIGHTCAP_64);
            if (p.isWhite()) {
                if (isOnSecondRank((byte) from)) {
                    // moves.add(new Move((byte) from, (byte) (from - RIGHTCAP_64), WHITE, WHITE_PAWN, MoveType.PROMOTION_CAPTURE));
                    int to = from - RIGHTCAP_64;
                    moves.add(Move.encodeMove(from, to, p.getValue(), BLACK_QUEEN.getValue(), Move.FLAG_PROMOTION));
                    moves.add(Move.encodeMove(from, to, p.getValue(), BLACK_ROOK.getValue(), Move.FLAG_PROMOTION));
                    moves.add(Move.encodeMove(from, to, p.getValue(), BLACK_KNIGHT.getValue(), Move.FLAG_PROMOTION));
                    moves.add(Move.encodeMove(from, to, p.getValue(), BLACK_BISHOP.getValue(), Move.FLAG_PROMOTION));
                } else {
                    // moves.add(new Move((byte) from, (byte) (from - RIGHTCAP_64), WHITE, WHITE_PAWN, MoveType.CAPTURE));
                    moves.add(Move.encodeMove(from, from - RIGHTCAP_64, p.getValue(),0, Move.FLAG_QUIET));
                }
            }
        }
    }

}



