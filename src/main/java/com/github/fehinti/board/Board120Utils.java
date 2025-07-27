package com.github.fehinti.board;

import java.util.function.Predicate;

public class Board120Utils {

    public static final boolean  CWHITE = true;
    public static final boolean  CBLACK = false;

    // color coding of the board
    public static final boolean[] COLOR = {
            CWHITE, CBLACK, CWHITE, CBLACK, CWHITE, CBLACK, CWHITE, CBLACK, CWHITE, CBLACK,
            CBLACK, CWHITE, CBLACK, CWHITE, CBLACK, CWHITE, CBLACK, CWHITE, CBLACK, CWHITE,
            CBLACK, CWHITE, CBLACK, CWHITE, CBLACK, CWHITE, CBLACK, CWHITE, CBLACK, CWHITE,
            CWHITE, CBLACK, CWHITE, CBLACK, CWHITE, CBLACK, CWHITE, CBLACK, CWHITE, CBLACK,
            CBLACK, CWHITE, CBLACK, CWHITE, CBLACK, CWHITE, CBLACK, CWHITE, CBLACK, CWHITE,
            CWHITE, CBLACK, CWHITE, CBLACK, CWHITE, CBLACK, CWHITE, CBLACK, CWHITE, CBLACK,
            CBLACK, CWHITE, CBLACK, CWHITE, CBLACK, CWHITE, CBLACK, CWHITE, CBLACK, CWHITE,
            CWHITE, CBLACK, CWHITE, CBLACK, CWHITE, CBLACK, CWHITE, CBLACK, CWHITE, CBLACK,
            CBLACK, CWHITE, CBLACK, CWHITE, CBLACK, CWHITE, CBLACK, CWHITE, CBLACK, CWHITE,
            CWHITE, CBLACK, CWHITE, CBLACK, CWHITE, CBLACK, CWHITE, CBLACK, CWHITE, CBLACK,
            CWHITE, CBLACK, CWHITE, CBLACK, CWHITE, CBLACK, CWHITE, CBLACK, CWHITE, CBLACK,
            CBLACK, CWHITE, CBLACK, CWHITE, CBLACK, CWHITE, CBLACK, CWHITE, CBLACK, CWHITE, };


    public static final byte PIECE_TYPE_COUNT = 12;
    public static final byte A1= 21;
    public static final byte B1= 22;
    public static final byte C1= 23;
    public static final byte D1= 24;
    public static final byte E1= 25;
    public static final byte F1= 26;
    public static final byte G1= 27;
    public static final byte H1= 28;

    public static final byte A2 = 31;
    public static final byte B2 = 32;
    public static final byte C2 = 33;
    public static final byte D2 = 34;
    public static final byte E2 = 35;
    public static final byte F2 = 36;
    public static final byte G2 = 37;
    public static final byte H2 = 38;

    public static final byte A3 = 41;
    public static final byte B3 = 42;
    public static final byte C3 = 43;
    public static final byte D3 = 44;
    public static final byte E3 = 45;
    public static final byte F3 = 46;
    public static final byte G3 = 47;
    public static final byte H3 = 48;

    public static final byte A4 = 51;
    public static final byte B4 = 52;
    public static final byte C4 = 53;
    public static final byte D4 = 54;
    public static final byte E4 = 55;
    public static final byte F4 = 56;
    public static final byte G4 = 57;
    public static final byte H4 = 58;

    public static final byte A5 = 61;
    public static final byte B5 = 62;
    public static final byte C5 = 63;
    public static final byte D5 = 64;
    public static final byte E5 = 65;
    public static final byte F5 = 66;
    public static final byte G5 = 67;
    public static final byte H5 = 68;

    public static final byte A6 = 71;
    public static final byte B6 = 72;
    public static final byte C6 = 73;
    public static final byte D6 = 74;
    public static final byte E6 = 75;
    public static final byte F6 = 76;
    public static final byte G6 = 77;
    public static final byte H6 = 78;

    public static final byte A7 = 81;
    public static final byte B7 = 82;
    public static final byte C7 = 83;
    public static final byte D7 = 84;
    public static final byte E7 = 85;
    public static final byte F7 = 86;
    public static final byte G7 = 87;
    public static final byte H7 = 88;

    public static final byte A8 = 91;
    public static final byte B8 = 92;
    public static final byte C8 = 93;
    public static final byte D8 = 94;
    public static final byte E8 = 95;
    public static final byte F8 = 96;
    public static final byte G8 = 97;
    public static final byte H8 = 98;

    public static final byte EMPTY = 0;
    public static final byte WPAWN = 1;
    public static final byte WKNIGHT = 2;
    public static final byte WBISHOP = 3;
    public static final byte WROOK = 4;
    public static final byte WQUEEN = 5;
    public static final byte WKING = 6;

    public static final byte BPAWN = -127;
    public static final byte BKNIGHT = -126;
    public static final byte BBISHOP = -125;
    public static final byte BROOK = -124;
    public static final byte BQUEEN = -123;
    public static final byte BKING = -122;

    public final static int BOARD_SIZE     = 64;
    public final static int BOARD_SIZE_120 = 120;

    public final static int QUEEN_PROMO = 3;
    public final static int ROOK_PROMO = 2;
    public final static int BISHOP_PROMO = 1;
    public final static int KNIGHT_PROMO = 0;


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

    public final static byte MAX_LEN_16 = 16;

    static Predicate<Byte> secondRank = i -> (i >= A2  && i <= H2);
    static Predicate<Byte> seventhRank = i -> (i >= A7 && i <= H7);


    public static boolean isOnSecondRank(byte square) {
        return secondRank.test(square);
    }

    public static boolean isOnSeventhRank(byte square) {
        return seventhRank.test(square);
    }

    // maps index of an 8 x8 board to 10 x 12 board
    public static int mapIndex64To120(int rank, int file) {
        return (rank * 10) + 21 + file;
    }
}

