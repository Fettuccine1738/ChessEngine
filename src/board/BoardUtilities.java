package board;

import java.util.function.Predicate;

import static board.Square.*;

public class BoardUtilities {

    static Predicate<Byte> secondRank = i -> (i >= A2.getIndex() && i <= H2.getIndex());
    static Predicate<Byte> seventhRank = i -> (i >= A7.getIndex() && i <= H7.getIndex());
    // board size
    public final static int BOARD_SIZE     = 64;
    public final static int BOARD_SIZE_120 = 120;

    // side to move
    public final static boolean BLACK         = false;
    public final static boolean WHITE         = true;

    // rank
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

    // max possible amount of PieceType (No of possible type from the null
    // starting position + ( 4 - 8 ) possible pawn promotions for all non-pawn Piece
    public final static byte MAX_KING   = 1; // 0 - 1
    public final static byte MAX_QUEEN  = 11; // 1 - 9
    public final static byte MAX_ROOKS  = 21; //
    public final static byte MAX_BISHOP = 31;
    public final static byte MAX_KNIGHT = 41;
    public final static byte MAX_PAWN   = 48;
    public final static byte MAX_MAX    = 49;


    public static int getPieceListSize(PieceType piece) {
        switch (piece) {
            case BLACK_ROOK, WHITE_ROOK -> { return MAX_ROOKS; }
            case BLACK_PAWN, WHITE_PAWN -> { return MAX_PAWN; }
            case BLACK_BISHOP, WHITE_BISHOP -> { return MAX_BISHOP; }
            case BLACK_KNIGHT, WHITE_KNIGHT -> { return MAX_KNIGHT; }
            case BLACK_QUEEN, WHITE_QUEEN -> { return MAX_QUEEN; }
            case BLACK_KING, WHITE_KING -> { return MAX_KING; }
            default ->  throw new IllegalArgumentException("Invalid piece type");
        }
    }

    public static int getPieceListFloor(PieceType piece) {
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

    public static boolean isOnSecondRank(byte square) {
        return secondRank.test(square);
    }

    public static boolean isOnSeventhRank(byte square) {
        return seventhRank.test(square);
    }
}
