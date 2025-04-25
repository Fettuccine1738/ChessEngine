package com.github.fehinti.board;

import java.util.Random;

public class ZobristHash {

    private static final Random random = new Random();

    private static final long[][] table = new long[64][12];
    private static final long BLACK_BIT_STRING;


    static {
        BLACK_BIT_STRING = random.nextLong();
        for (int i = 0; i < 64; i++) {
            for (int j = 0; j < 12; j++) {
                table[i][j] = random.nextLong();
            }
        }
    }

    private static int mapBlackToPositiveInt(int blackValue) {
        return switch(blackValue) { // blackPiece.getValue() - 1 to adjust to zero and non -ve index
            case -1 ->  6;
            case -2 ->  7;
            case -3 ->  8;
            case -4 ->  9;
            case -5 ->  10;
            case -6 ->  11;
            default -> throw new IllegalArgumentException("Invalid black value: " + blackValue);
        };
    }

    public static long hash(Board board) {
        long h = 0L;
        for (int i = 0; i < 64; i++) {
            PieceType p = board.getPieceOnBoard(i);
            if (p != PieceType.EMPTY) {
                int v = p.getValue();
                int index = (v > 0) ? v - 1 : mapBlackToPositiveInt(v);
                h ^= table[i][index];
            }
        }
        return h;
    }

    public static long zobrist(int square, int pieceVal) {
        if (pieceVal == 0) throw new IllegalArgumentException();
        if (pieceVal < 0) mapBlackToPositiveInt(pieceVal);
        return table[square][pieceVal];
    }

}
