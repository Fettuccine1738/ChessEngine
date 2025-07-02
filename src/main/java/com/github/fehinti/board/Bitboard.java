package com.github.fehinti.board;

import java.util.Arrays;

import static com.github.fehinti.board.Board120Utils.*;
import static com.github.fehinti.board.BoardUtilities.*;

import  com.github.fehinti.piece.Piece;

public class Bitboard {

    Long WP, WN, WB, WR, WQ, WK, BP, BN, BB, BR, BQ, BK;

    public Bitboard() {
        WP = WN = WB = WR = WQ = WK = 0L;
        BP = BN = BB = BR = BQ = BK = 0L;
    }

    public Bitboard(Board120 b) {
        initBitBoards(b);
    }

    private static final long SHIFT_BY = 1L;

    private long mapPieceToBitBoard(byte piece) {
        switch (piece) {
            case WKING-> { return WK;}
            case WQUEEN-> { return WQ;}
            case WROOK-> { return WR;}
            case WBISHOP-> { return WB;}
            case WKNIGHT-> { return WN;}
            case WPAWN-> { return WP;}
            case BKING-> { return BK;}
            case BQUEEN-> { return BQ;}
            case BROOK-> { return BR;}
            case BBISHOP-> { return BB;}
            case BKNIGHT-> { return BN;}
            case BPAWN-> { return BP;}
            default -> { return 0L;}
        }
    }

    public void initBitBoards(Board120 board) {
        int rank = RANK_8;
        long current = 0L;
        WP = WN = WB = WR = WQ = WK = 0L;
        BP = BN = BB = BR = BQ = BK = 0L;
        int sq120, sq64;

        for (int i = RANK_8 - 1; i >= 0; i--) {
            for (int j = FILE_A; j < FILE_H; j++) {
                sq120 = Board120Utils.mapIndex64To120(i, j);
                sq64 = Board.getMailbox120Number(sq120);
                byte p = board.getPieceOnSquare((byte) sq120);
                if (p ==EMPT_SQ) { continue; }
                switch (p) {
                    case WKING -> WK |= (SHIFT_BY << sq64);
                    case WQUEEN -> WQ |= (SHIFT_BY << sq64);
                    case WROOK ->  WR |= (SHIFT_BY << sq64);
                    case WBISHOP -> WB |= (SHIFT_BY << sq64);
                    case WKNIGHT -> WN |= (SHIFT_BY << sq64);
                    case WPAWN ->   WP |= (SHIFT_BY << sq64);
                    case BKING ->   BK |= (SHIFT_BY << sq64);
                    case BQUEEN ->  BQ |= (SHIFT_BY << sq64);
                    case BROOK ->   BR |= (SHIFT_BY << sq64);
                    case BBISHOP -> BB |= (SHIFT_BY << sq64);
                    case BKNIGHT -> BN |= (SHIFT_BY << sq64);
                    case BPAWN ->   BP |= (SHIFT_BY << sq64);
                    default -> throw new IllegalArgumentException(" illegal piece found ");
                }
            }
        }
        // checkBoardBitCount();
    }

    private void printUnionBitboards() {
        long NOT_EMPTY= WP | WN | WB | WR | WQ | WK |
                        BP | BN | BB | BR | BQ | BK  ;
        int start = RANK_8 - 1;
        int sq120, sq64;
        for (int i = start; i >= 0; i--) {
            for (int j = FILE_A; j < FILE_H ; j++) {
                sq120 = Board.mapIndex64To120(i, j);
                sq64 = Board.getMailbox120Number(sq120);
                if (((SHIFT_BY << sq64) & NOT_EMPTY) == 0) {
                    System.out.print("_");
                }
                else {
                    System.out.print("X");
                }
            }
            System.out.println();
        }
        System.out.println("\n\n");
    }

    private void printBitboards(byte piece) {
        long toPrint = mapPieceToBitBoard(piece);
        int start = RANK_8 - 1;
        int sq120, sq64;
        for (int i = start; i >= 0; i--) {
            for (int j = FILE_A; j < FILE_H ; j++) {
                sq120 = Board.mapIndex64To120(i, j);
                sq64 = Board.getMailbox120Number(sq120);
                if (((SHIFT_BY << sq64) & toPrint) == 0) {
                    System.out.print("_");
                }
                else {
                    System.out.print("X");
                }
            }
            System.out.println();
        }
        System.out.println("\n\n");
    }
}
