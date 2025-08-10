package com.github.fehinti.piece;

import com.github.fehinti.board.Board120;


public class Move {
    // .Move.encodeMove(from, to, newSquare, 0, com.github.fehinti.board.Move.FLAG_CAPTURE, index);
    // from = bits 0 - 7, to 8 - 15, 15 - 23, promotion (3),flag (3), index (4  bits)
    // origin, dest,

    // tactical move flags
    public static final int QUIET = 0;
    public static final int EN_PASSANT = 1;
    public static final int CASTLE = 2;
    public static final int DOUBLE_PAWN_PUSH = 3;
    public static final int CAPTURE = 4;
    public static final int PROMOTION = 5;
    public static final int PROMOTION_CAPTURE = 6;

    // ? last flag bytes only needs 3 bytes to so bit 23 to 31 is still avilable
    // ? may need switch to 64 bytes encoding in the future
    // bits
    /**
     * variable (start_inclusive..end_inclusive)
     * @param from 0..6
     * @param to   7..12
     * @param promoted 14..15 {knight = 00, bishop = 01, rook = 10, queen = 11}
     * @param flag  16..18 (6 = 0b00000_110)
     * @param index 19..22 index in the piece list to preserve move generation ordering.
     *        scoring on the last byte 31
     * @return
     */
    public static int encodeMove(int from, int to, int promoted, int flag, int index) {
        // if promotion flag is not set, ignore anything encoded in promoted
        return (from) | (to << 7) | (promoted << 14) | (flag << 16) | (index << 19);
    }

    public static int encodeMove(int from, int to, int promoted, int flag, int index, int score) {
        // if promotion flag is not set, ignore anything encoded in promoted
        return (from) | (to << 7) | (promoted << 14) | (flag << 16) | (index << 19) | (score << 24);
    }

    public static int getIndex(int move) {
        return  (move >> 19) & 0xf; // 0x7 would be fine here, just 3 bits needed
    }

    public static int getFromSquare(int move) {
        return  move & 0x7f;
    }

    public static int getTargetSquare(int move) {
        return (move >> 7) & 0x7f;
    }

    public static int getPromotion(int move) {
        return (move >> 14) & 0x3;
    }

    public static int getFlag(int move) {
        return (move >> 16) & 0x7;
    }

    public static int getScore(int move) {
        return (move >>> 24) & 0xff;
    }

    public static String printMove(int move) {
        StringBuilder sb = new StringBuilder();
        int from = Board120.getMailbox120Number(getFromSquare(move));
        int to = Board120.getMailbox120Number(getTargetSquare(move));
        int fromRank = (from / 8) + 1; // adjust for zero based index
        int fromFile = from % 8;
        int toRank = 1 + (to / 8);
        int toFile = to % 8;
        sb.append((char) ('a' + fromFile))
                .append(fromRank)
                .append((char) ('a' + toFile))
                .append(toRank);
        if (getFlag(move) == PROMOTION ||  getFlag(move) == PROMOTION_CAPTURE) {
            int pr = getPromotion(move);
            if (pr == 0) sb.append('n');
            if (pr == 1) sb.append('b');
            if (pr == 2) sb.append('r');
            if (pr == 3) sb.append('q');
        }
        return sb.toString();
    }

    private static String printFlag(int flag) {
        return switch (flag) {
            case QUIET -> "Quiet";
            case EN_PASSANT -> "Ep";
            case CASTLE -> "Castle";
            case CAPTURE -> "Capture";
            case PROMOTION_CAPTURE -> "Promotion";
            case DOUBLE_PAWN_PUSH -> "DoublePawnPush";
            default -> throw new IllegalStateException("Unexpected value: " + flag);
        };
    }

    public static String dbgMove(int move) {
        return printMove(move) + "\t" + printFlag(getFlag(move));
    }

}