package com.github.fehinti.piece;

import com.github.fehinti.board.BoardUtilities;
import com.github.fehinti.board.Board;

import java.util.Arrays;

/***************************************************************************
 *  As described by Ed Schröder in Evaluation in REBEL [6] ,
 *  Rebel uses two board tables for both sides, one byte entry each, the three lower bits contain an attack counter,
 *  while the five upper bits indicate the presence of least one pawn or piece attacking/defending:
 * +------+------+------+------+------+------+------+------+
 * | BIT0 | BIT1 | BIT2 | BIT3 | BIT4 | BIT5 | BIT6 | BIT7 |
 * +------+------+------+------+------+------+------+------+
 * |      Number of     | PAWN |KNIGHT| ROOK | QUEEN| KING |
 * |      ATTACKERS     |      |BISHOP|      |      |      |
 * +------+------+------+------+------+------+------+------+
 **********************************************************************/
public class EdSchroederLookup {

    private static final short[] whiteSide = new short[BoardUtilities.BOARD_SIZE];
    private static final short[] blackSide = new short[BoardUtilities.BOARD_SIZE];

    static {
        Arrays.fill(whiteSide, (short)0);
        Arrays.fill(blackSide, (short)0);
    }

    // position at the start of a game
    public static void setInitialAttackMap(Board board , boolean side) {
        int left =  side ? PieceMove.LEFTCAP_64 : (-1) * PieceMove.LEFTCAP_64;
        int right = side ? PieceMove.RIGHTCAP_64 : (-1) * PieceMove.RIGHTCAP_64;
        int offset_left = side ? PieceMove.LEFTCAP : - PieceMove.LEFTCAP;
        int offset_right = side ? PieceMove.RIGHTCAP : - PieceMove.RIGHTCAP;
        int[] piecelist = side ? board.getWhitePieceList() : board.getBlackPieceList();
        int N = BoardUtilities.MAX_MAX;

        int data, pos, pce;
        for (int i = 0; i < N; i++) {
            data = piecelist[i]; //
            pos = (data >> 8);
            pce = (data & 0xff);



        }

    }



}
