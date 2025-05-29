package com.github.fehinti.board;

import java.util.function.Predicate;
import com.github.fehinti.piece.Piece;

public class BoardUtilities {

    // castling constants commonly used rank and files bounds
    public final static int A_1 = 0;
    public final static int B_1 = 1;
    public final static int C_1 = 2;
    public final static int D_1 = 3;
    public final static int E_1 = 4;
    public final static int F_1 = 5;
    public final static int G_1 = 6;
    public final static int H_1 = 7;
    public final static int A_2 = 8;
    public final static int A_7 = 48;
    public final static int H_2 = 15;
    public final static int H_7 = 55;
    public final static int A_8 = 56;
    public final static int B_8 = 57;
    public final static int C_8 = 58;
    public final static int D_8 = 59;
    public final static int E_8 = 60;
    public final static int F_8 = 61;
    public final static int G_8 = 62;
    public final static int H_8 = 63;

    // board size
    public final static int BOARD_SIZE     = 64;
    public final static int BOARD_SIZE_120 = 120;

    // side to move
    public final static boolean BLACK         = false;
    public final static boolean WHITE         = true;

    // rank
    public final static int EMPT_SQ = 0;
    public final static int RANK_1 = 0;
    public final static int RANK_8 = 8;

    // file
    public final static int FILE_A = 0;
    public final static int FILE_H = 8;

    // sentinel and blocking piece
    public final static int OFF_BOARD = -1;
    // castling rights
    public final static byte WHITE_KINGSIDE     = 1; // 0001
    public final static byte WHITE_QUEENSIDE    = 2; // 0010
    public final static byte BLACK_KINGSIDE     = 4; // 0100
    public final static byte BLACK_QUEENSIDE    = 8; // 1000

    // max possible amount of Piece (No of possible type from the null
    // starting position + ( 4 - 8 ) possible pawn promotions for all non-pawn Piece
    public final static byte MAX_KING   = 1; // 0 - 1
    public final static byte MAX_QUEEN  = 11; // 1 - 9
    public final static byte MAX_ROOKS  = 21; //
    public final static byte MAX_BISHOP = 31;
    public final static byte MAX_KNIGHT = 41;
    public final static byte MAX_PAWN   = 48;
    public final static byte MAX_MAX    = 49;

    // 16 length vector
    public final static byte MAX_LEN_16 = 16;

    // this 
    public static int getPieceListCeiling(Piece piece) {
        switch (piece) {
            case BLACK_ROOK, WHITE_ROOK -> { return MAX_ROOKS; }
            case BLACK_PAWN, WHITE_PAWN -> { return MAX_MAX; }
            case BLACK_BISHOP, WHITE_BISHOP -> { return MAX_BISHOP; }
            case BLACK_KNIGHT, WHITE_KNIGHT -> { return MAX_KNIGHT; }
            case BLACK_QUEEN, WHITE_QUEEN -> { return MAX_QUEEN; }
            case BLACK_KING, WHITE_KING -> { return MAX_KING; }
            default ->  throw new IllegalArgumentException("Invalid piece type");
        }
    }

    public static int getPieceListFloor(Piece piece) {
        switch (piece) {
            case BLACK_KING, WHITE_KING -> { return 0; }
            case BLACK_QUEEN, WHITE_QUEEN -> { return MAX_KING; }
            case BLACK_ROOK, WHITE_ROOK -> { return MAX_QUEEN; }
            case BLACK_BISHOP, WHITE_BISHOP -> { return MAX_ROOKS; }
            case BLACK_KNIGHT, WHITE_KNIGHT -> { return MAX_BISHOP; }
            case BLACK_PAWN, WHITE_PAWN -> { return MAX_KNIGHT; }
            default ->  throw new IllegalArgumentException("Invalid piece type");
        }
    }

    static Predicate<Byte> secondRank = i -> (i >= A_2  && i <= H_2);
    static Predicate<Byte> seventhRank = i -> (i >= A_7 && i <= H_7);

    public static boolean isOnSecondRank(byte square) {
        return secondRank.test(square);
    }

    public static boolean isOnSeventhRank(byte square) {
        return seventhRank.test(square);
    }
}
