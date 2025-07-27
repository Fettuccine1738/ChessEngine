package com.github.fehinti.engine;

import com.github.fehinti.board.Board120;


import static com.github.fehinti.board.Board120Utils.*;

/*
 The values for the pieces are taken from the CPW website
 https://www.chessprogramming.org/Simplified_Evaluation_Function
 */
public class SimpleEvaluator implements  Evaluator {
    static final SimpleEvaluator INSTANCE  = new SimpleEvaluator();
    public static SimpleEvaluator getInstance() {
        return INSTANCE;
    }

    private static final int PAWN_VAL   = 100;
    private static final int KNIGHT_VAL = 320;
    private static final int BISHOP_VAL = 330;
    private static final int ROOK_VAL   = 500;
    private static final int QUEEN_VAL  = 900;
    private static final int KING_VAL   = 20000;

    final static byte[] WHITE_PAWN = {
            0,  0,  0,  0,  0,  0,  0,  0,
            50, 50, 50, 50, 50, 50, 50, 50,
            10, 10, 20, 30, 30, 20, 10, 10,
            5,  5, 10, 25, 25, 10,  5,  5,
            0,  0,  0, 20, 20,  0,  0,  0,
            5, -5,-10,  0,  0,-10, -5,  5,
            5, 10, 10,-20,-20, 10, 10,  5,
            0,  0,  0,  0,  0,  0,  0,  0
    };

    final static byte[] WHITE_KNIGHT = {
            -50,-40,-30,-30,-30,-30,-40,-50,
            -40,-20,  0,  0,  0,  0,-20,-40,
            -30,  0, 10, 15, 15, 10,  0,-30,
            -30,  5, 15, 20, 20, 15,  5,-30,
            -30,  0, 15, 20, 20, 15,  0,-30,
            -30,  5, 10, 15, 15, 10,  5,-30,
            -40,-20,  0,  5,  5,  0,-20,-40,
            -50,-40,-30,-30,-30,-30,-40,-50,
    };

    final static byte[] WHITE_BISHOP = {
            -20,-10,-10,-10,-10,-10,-10,-20,
            -10,  0,  0,  0,  0,  0,  0,-10,
            -10,  0,  5, 10, 10,  5,  0,-10,
            -10,  5,  5, 10, 10,  5,  5,-10,
            -10,  0, 10, 10, 10, 10,  0,-10,
            -10, 10, 10, 10, 10, 10, 10,-10,
            -10,  5,  0,  0,  0,  0,  5,-10,
            -20,-10,-10,-10,-10,-10,-10,-20,
    };

    final static byte[] WHITE_ROOK = {
            0,  0,  0,  0,  0,  0,  0,  0,
            5, 10, 10, 10, 10, 10, 10,  5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            0,  0,  0,  5,  5,  0,  0,  0
    };

    final static byte[] WHITE_QUEEN = {
            -20,-10,-10, -5, -5,-10,-10,-20,
            -10,  0,  0,  0,  0,  0,  0,-10,
            -10,  0,  5,  5,  5,  5,  0,-10,
            -5,  0,  5,  5,  5,  5,  0, -5,
            0,  0,  5,  5,  5,  5,  0, -5,
            -10,  5,  5,  5,  5,  5,  0,-10,
            -10,  0,  5,  0,  0,  0,  0,-10,
            -20,-10,-10, -5, -5,-10,-10,-20
    };

    final static byte[] WHITE_KING_MIDDLE = {
            -30,-40,-40,-50,-50,-40,-40,-30,
            -30,-40,-40,-50,-50,-40,-40,-30,
            -30,-40,-40,-50,-50,-40,-40,-30,
            -30,-40,-40,-50,-50,-40,-40,-30,
            -20,-30,-30,-40,-40,-30,-30,-20,
            -10,-20,-20,-20,-20,-20,-20,-10,
            20, 20,  0,  0,  0,  0, 20, 20,
            20, 30, 10,  0,  0, 10, 30, 20
    };

    final static byte[] WHITE_KING_END = {
            -50,-40,-30,-20,-20,-30,-40,-50,
            -30,-20,-10,  0,  0,-10,-20,-30,
            -30,-10, 20, 30, 30, 20,-10,-30,
            -30,-10, 30, 40, 40, 30,-10,-30,
            -30,-10, 30, 40, 40, 30,-10,-30,
            -30,-10, 20, 30, 30, 20,-10,-30,
            -30,-30,  0,  0,  0,  0,-30,-30,
            -50,-30,-30,-30,-30,-30,-30,-50
    };

    public double evaluate(Board120 board120) {
       int sScore = 0;
       int xScore = 0;

       boolean b = board120.getSideToMove();
       int[] sSide = (b ? board120.getWhitePieceList() :  board120.getBlackPieceList());
       int count = (b) ? board120.getWhitePcCount() : board120.getBlackPcCount();

       int[] xSide = (b ? board120.getBlackPieceList() :  board120.getWhitePieceList());
       int xcount = (b) ? board120.getBlackPcCount() : board120.getWhitePcCount();

       for (int i = 0; i < MAX_LEN_16; i++) {
           if (sSide[i] != OFF_BOARD) {
               int wp = (sSide[i] >> RANK_8) & 0xff;
               sScore += getPieceValue(wp) + getPieceTableEntry(count, wp, b, sSide[i] & 0xff);
           }
           if (xSide[i] != OFF_BOARD) {
               int bp = (xSide[i] >> RANK_8) & 0xff;
               sScore += getPieceValue(bp) + getPieceTableEntry(xcount, bp, !b, xSide[i] & 0xff);
           }
       }
       return (sScore - xScore) / 10000.;
    }

    public static int getPieceTableEntry(int pCount, int piece, boolean side, int square) {
       if (square < A1 || square > H8) throw new RuntimeException("Entry for this piece is out of bounds" + square);
       square = Board120.getMailbox120Number(square);
       if (!side) square ^= 56; // mirror for black pieces
       return switch (piece) {
           case WPAWN, -BPAWN -> WHITE_PAWN[square];
           case WKNIGHT, -BKNIGHT -> WHITE_KNIGHT[square];
           case WBISHOP, -BBISHOP -> WHITE_BISHOP[square];
           case WROOK, -BROOK -> WHITE_ROOK[square];
           case WQUEEN, -BQUEEN -> WHITE_QUEEN[square];
           case WKING, -BKING -> {
               if (pCount < 8) yield WHITE_KING_END[square];
               else yield WHITE_KING_MIDDLE[square];
           }
           default -> 0;
       };
    }

    public static int getPieceValue(int piece) {
        return switch(piece) {
            case WPAWN, -BPAWN -> PAWN_VAL;
            case WKNIGHT, -BKNIGHT -> KNIGHT_VAL;
            case WBISHOP -BBISHOP -> BISHOP_VAL;
            case WROOK, -BROOK -> ROOK_VAL;
            case WQUEEN, -BQUEEN -> QUEEN_VAL;
            case WKING, -BKING -> KING_VAL;
            default -> 0;
        };
    }
}