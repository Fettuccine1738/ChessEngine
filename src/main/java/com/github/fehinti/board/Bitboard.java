package com.github.fehinti.board;

import java.util.Arrays;
import static com.github.fehinti.board.BoardUtilities.*;

public class Bitboard {

    Long WP, WN, WB, WR, WQ, WK, BP, BN, BB, BR, BQ, BK;

    public Bitboard() {
        WP = WN = WB = WR = WQ = WK = 0L;
        BP = BN = BB = BR = BQ = BK = 0L;
    }

    public Bitboard(Board b) {
        initBitBoards(b);
    }

    private static final long SHIFT_BY = 1L;

    private long mapPieceToBitBoard(PieceType piece) {
        switch (piece) {
            case WHITE_KING-> { return WK;}
            case WHITE_QUEEN-> { return WQ;}
            case WHITE_ROOK-> { return WR;}
            case WHITE_BISHOP-> { return WB;}
            case WHITE_KNIGHT-> { return WN;}
            case WHITE_PAWN-> { return WP;}
            case BLACK_KING-> { return BK;}
            case BLACK_QUEEN-> { return BQ;}
            case BLACK_ROOK-> { return BR;}
            case BLACK_BISHOP-> { return BB;}
            case BLACK_KNIGHT-> { return BN;}
            case BLACK_PAWN-> { return BP;}
            default -> { return 0L;}
        }
    }

    public void initBitBoards(Board board) {
        int rank = RANK_8;
        long current = 0L;
        WP = WN = WB = WR = WQ = WK = 0L;
        BP = BN = BB = BR = BQ = BK = 0L;
        int sq120, sq64;

        for (int i = RANK_8 - 1; i >= 0; i--) {
            for (int j = FILE_A; j < FILE_H; j++) {
                sq120 = Board.mapIndex64To120(i, j);
                sq64 = Board.getMailbox120Number(sq120);
                PieceType p = board.getPieceOnBoard(sq64);
                if (p == PieceType.EMPTY) {continue; }
                switch (p) {
                    case WHITE_KING -> WK |= (SHIFT_BY << sq64);
                    case WHITE_QUEEN -> WQ |= (SHIFT_BY << sq64);
                    case WHITE_ROOK ->  WR |= (SHIFT_BY << sq64);
                    case WHITE_BISHOP -> WB |= (SHIFT_BY << sq64);
                    case WHITE_KNIGHT -> WN |= (SHIFT_BY << sq64);
                    case WHITE_PAWN ->   WP |= (SHIFT_BY << sq64);
                    case BLACK_KING ->   BK |= (SHIFT_BY << sq64);
                    case BLACK_QUEEN ->  BQ |= (SHIFT_BY << sq64);
                    case BLACK_ROOK ->   BR |= (SHIFT_BY << sq64);
                    case BLACK_BISHOP -> BB |= (SHIFT_BY << sq64);
                    case BLACK_KNIGHT -> BN |= (SHIFT_BY << sq64);
                    case BLACK_PAWN ->   BP |= (SHIFT_BY << sq64);
                }
            }
        }
        // checkBoardBitCount();
    }

    // checks that the number of set bits (1s) in a start position
    // is equal to number of pieces in a initial chess position
    private void checkBoardBitCount() {
        //assertEquals(1, Long.bitCount(this.WK));
        //assertEquals(1, Long.bitCount(this.WQ));
        //assertEquals(2, Long.bitCount(this.WR));
        //assertEquals(2, Long.bitCount(this.WB));
        //assertEquals(2, Long.bitCount(this.WN));
        //assertEquals(8, Long.bitCount(this.WP));
        //assertEquals(1, Long.bitCount(this.BK));
        //assertEquals(1, Long.bitCount(this.BQ));
        //assertEquals(2, Long.bitCount(this.BR));
        //assertEquals(2, Long.bitCount(this.BB));
        //assertEquals(2, Long.bitCount(this.BN));
        //assertEquals(8, Long.bitCount(this.BP));
    }

    public void printBitboards() {
        Arrays.stream(PieceType.values())
                .filter(p -> p.getValue() != 0) // ignore empty for now
                .forEach(this::printBitboards);
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

    private void printBitboards(PieceType piece) {
        long toPrint = mapPieceToBitBoard(piece);
        int start = RANK_8 - 1;
        System.out.println(piece.getName());
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

    static String FEN_1 = "rnbqkbnp/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    public static void main(String[] args) {
        Bitboard b = new Bitboard();
        Board board = FENParser.parseFENotation(Board.FEN_ONE_PIN);
        System.out.println(board + "\n");

        b.initBitBoards(board);
        b.printUnionBitboards();
        //b.printBitboards();
        //System.out.println(Long.bitCount(b.BB));


    }
}
