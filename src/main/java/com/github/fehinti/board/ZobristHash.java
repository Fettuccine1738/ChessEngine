package com.github.fehinti.board;


import java.util.Random;

import static com.github.fehinti.board.Board120Utils.*;

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

    // black piece value on board is in range -127 (bpawn) to -122(bking)
    // 133 maps this index to 6 (bpawn) to 11(bking) to index the zobrist table
    static final int ADJUST_BLACK_INDEX = 133;
    private static final Random random = new Random();

    // each square (0..63) and piece combination (K,Q,R,B,N,P,k,q,r,b,n,p)
    private static final long[][] table = new long[BOARD_SIZE][PIECE_TYPE_COUNT];
    private static final long BLACK_TO_MOVE = Math.abs(random.nextLong());
    // * For future use
    private static final long BLACK_KING_SIDE_CASTLE =  random.nextLong();
    private static final long BLACK_QUEEN_SIDE_CASTLE =  random.nextLong();
    private static final long WHITE_KING_SIDE_CASTLE =  random.nextLong();
    private static final long WHITE_QUEEN_SIDE_CASTLE =  random.nextLong();

    static {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < PIECE_TYPE_COUNT; j++) {
                table[i][j] = Math.abs(random.nextLong());
            }
        }
    }

    // * this is only used at initialization
    public static long hashAtInit(Board120 board) {
        long result = (board.getSideToMove()) ? 0L : BLACK_TO_MOVE;
        for (int i = 0; i < 64; i++) {
            int index120 = Board120.getMailbox64Number(i);
            int piece = board.getPieceOnSquare(index120);
            if (piece != 0) {
                int index = (piece > 0) ? piece - 1 : (ADJUST_BLACK_INDEX + piece);
                result ^= table[i][index];
            }
        }
        return result;
    }

    // * this is used to incrementally update the hashvalue of the board
    // * we take advantage of the fact that the changes on a board are only local
    // * e.g a white pawn push from a2 to a4 requires XOR out the current hash with
    // * [a2 = 31][0 (wp(1) - 1], then XOR in the new hash value with
    // * [a4 = 51][0]
    public static long zobristKey(int square, byte pieceVal) {
        if (pieceVal == 0) throw new IllegalArgumentException("square " + square + " piece val " + pieceVal);
        return table[square][(pieceVal > 0) ? pieceVal - 1 : ADJUST_BLACK_INDEX + pieceVal];
    }
}
