package com.github.fehinti.piece;

import com.github.fehinti.board.Board120;


public class Move {
    // .Move.encodeMove(from, to, newSquare, 0, com.github.fehinti.board.Move.FLAG_CAPTURE, index);
    // from = bits 0 - 7, to 8 - 15, 15 - 23, promotion (3),flag (3), index (4  bits)
    // origin, dest,

    // tactical move flags
    public static final int FLAG_QUIET = 0;
    public static final int FLAG_EN_PASSANT = 1;
    public static final int FLAG_CASTLE = 2;
    public static final int FLAG_DOUBLE_PAWN_PUSH = 3;
    public static final int FLAG_CAPTURE = 4;
    public static final int FLAG_PROMOTION = 5;
    public static final int FLAG_PROMOTION_CAPTURE = 6;

    // ? last flag bytes only needs 3 bytes to so bit 23 to 31 is still avilable
    // ? may need switch to 64 bytes encoding in the future
    // bits
    /**
     * @param from 0..6
     * @param to   7..12
     * @param promoted 13..14 {knight = 00, bishop = 01, rook = 10, queen = 11}
     * @param flag 15..17
     * @param index 18..21 index in the piece list to preserve move generation ordering.
     * @return
     */
    public static int encodeMove(int from, int to, int promoted, int flag, int index) {
        // if promotion flag is not set, ignore anything encoded in promoted
        return (from) | (to << 7) | (promoted << 14) | (flag << 16) | (index << 19);
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

    public static int mapBlackToUnsignedInt(int piece) {
        return switch (piece) {
            case -1 -> 9;
            case -2 -> 10;
            case -3 -> 11;
            case -4 -> 12;
            case -5 -> 13;
            case -6 -> 14;
            default -> throw new IllegalStateException("Unexpected value: " + piece);
        };
    }

    public static int mapCapturedPieceToBlack(int piece) {
        return switch (piece) {
            case 9 -> -1;
            case 10 -> -2;
            case 11 -> -3;
            case 12 -> -4;
            case 13 -> -5;
            case 14 -> -6;
            default -> throw new IllegalStateException("Unexpected value: " + piece);
        };
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
        return sb.toString();
    }

    private static String printFlag(int flag) {
        switch (flag) {
            case FLAG_QUIET : return "Quiet";
            case FLAG_EN_PASSANT:  return "Ep";
            case FLAG_CASTLE: return "Castle";
            case FLAG_CAPTURE: return "Capture";
            case FLAG_PROMOTION_CAPTURE: return "Promo";
            case FLAG_DOUBLE_PAWN_PUSH: return "DoublePawnPush";
            default: throw new IllegalStateException("Unexpected value: " + flag);
        }
    }

    public static String dbgMove(int move) {
        return printMove(move) + "\t" + printFlag(getFlag(move));
    }

}