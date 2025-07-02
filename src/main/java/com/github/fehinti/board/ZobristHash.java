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
    private static final long BLACK_BIT_STRING = random.nextLong();

    static {
        for (int i = 0; i < 64; i++) {
            for (int j = 0; j < 12; j++) {
                table[i][j] = random.nextLong();
            }
        }
    }

    private static int mapBlackToPositiveInt(int blackValue) {
        return switch(blackValue) { // blackPiece.getValue() - 1 to adjust to zero and non -ve index
            case -1, -127 ->  6;
            case -2, -126 ->  7;
            case -3, -125 ->  8;
            case -4, -124 ->  9;
            case -5, -123 ->  10;
            case -6, -122 ->  11;
            default -> throw new IllegalArgumentException("Invalid black value: " + blackValue);
        };
    }

    public static long hash(Board120 board) {
        long result = (board.getSideToMove()) ? 0L : BLACK_BIT_STRING;
        for (int i = 0; i < 64; i++) {
            int index120 = Board120.getMailbox64Number(i);
            int piece = board.getPieceOnSquare(index120);
            if (piece != 0) {
                int index = (piece > 0) ? piece - 1 : mapBlackToPositiveInt(piece);
                result ^= table[i][index];
            }
        }
        return result;
    }

    public static long zobristKey(int square, int pieceVal) {
        if (pieceVal == 0) throw new IllegalArgumentException();
        return table[square][(pieceVal > 0) ? pieceVal - 1 : mapBlackToPositiveInt(pieceVal)];
    }

    public static long zobristKey(int square, byte pieceVal) {
        return zobristKey(square, pieceVal);
    }

}
