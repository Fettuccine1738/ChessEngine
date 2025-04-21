package com.github.fehinti;

import com.github.fehinti.board.Board;
import com.github.fehinti.board.FENParser;
import com.github.fehinti.perft.Perft;
import com.github.fehinti.piece.*;

import static com.github.fehinti.board.BoardUtilities.*;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.

public class Main {

    private static final long SHIFT_BY = 1L;

    static void printLongAsBitboard(Long toPrint) {
        int start = RANK_8 - 1;
        // System.out.println(piece.getName());
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

    public static void main(String[] args) {
        long KING_ON_B2=
                        1L       | 1L << 1  | 1L << 2  |
                        1L << 7  |            1L << 9  |
                        1L << 15 | 1L << 16 | 1L << 17;
        System.out.println(10 % -1);

        //printLongAsBitboard(KING_ON_B2);
        //// long board = 0b0000_0001;
        //long board = 1L;
        //// board &= (0L);
        //// System.out.println(Long.toBinaryString(board));
        //board |= (1L << 10);// place board at c2
        //// System.out.println(Long.toBinaryString(board));
        //System.out.println(board);
        //printLongAsBitboard(board);
        //boolean isOcc = (board & (1L << 11)) != 0;
        //System.out.println(isOcc);
        //// knight maskso
        //long knightOn27 = 0L;
        //printLongAsBitboard(knightOn27);
        //for(int i : PieceMove.VECTOR_COORDINATES[1]) {
            //knightOn27 |= (1L << i + 27);
            //printLongAsBitboard(knightOn27);
        //}
        //printLongAsBitboard(knightOn27);
//
        //Perft.main(args);
//
//


    }
}