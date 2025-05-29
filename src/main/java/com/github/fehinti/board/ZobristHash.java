package com.github.fehinti.board;

import com.github.fehinti.piece.Piece;

import java.util.Random;
/*
* Zobrist hashing starts by randomly generating bitstrings for each possible
*  element of a board game,
* i.e. for each combination of a piece and a position (in the game of chess,
* that's 6 pieces × 2 colors × 64 board positions, with a constant number
* of additional bitstrings for castling rights, pawns that may
* capture en passant, and which player moves next).
* Now any board configuration can be broken up into independent piece/position components,
* which are mapped to the random bitstrings generated earlier. The final Zobrist hash is
*  computed by combining those bitstrings using bitwise XOR.
 */
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
        long h = (board.getSideToMove()) ? 0L : BLACK_BIT_STRING;
        for (int i = 0; i < 64; i++) {
            Piece p = board.getPieceOnBoard(i);
            if (p != Piece.EMPTY) {
                int v = p.getValue();
                int index = (v > 0) ? v - 1 : mapBlackToPositiveInt(v);
                h ^= table[i][index];
            }
        }
        return h;
    }

    public static long zobristKey(int square, int pieceVal) {
        if (pieceVal == 0) throw new IllegalArgumentException();
        return table[square][(pieceVal > 0) ? pieceVal - 1 : mapBlackToPositiveInt(pieceVal)];
    }

}
