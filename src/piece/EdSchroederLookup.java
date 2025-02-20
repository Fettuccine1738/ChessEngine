package piece;

import board.BoardUtilities;

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
 *
 **********************************************************************/
public class EdSchroederLookup {

    private static final byte[] whiteSide = new byte[BoardUtilities.BOARD_SIZE];
    private static final byte[] blackSide = new byte[BoardUtilities.BOARD_SIZE];


}
