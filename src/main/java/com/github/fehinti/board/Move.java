package com.github.fehinti.board;

public class Move {

    // tactical move flags
    public static final int FLAG_QUIET = 0;
    public static final int FLAG_EN_PASSANT = 1;
    public static final int FLAG_CASTLE = 2;
    public static final int FLAG_DOUBLE_PAWN_PUSH = 3;
    public static final int FLAG_CAPTURE = 4;
    public static final int FLAG_PROMOTION = 5;

    // ? last flag bytes only needs 3 bytes to so bit 23 to 31 is still avilable
    // ? may need switch to 64 bytes encoding in the future
    // TODO: use 5 bits to store indexes and maintain move generation ordering
    /**
     * @param from
     * @param to
     * @param captured
     * @param promoted
     * @param flag
     * @param index  0..15 index in the piece list to preserve move generation ordering.
     * @return
     */
    public static int encodeMove(int from, int to, int captured, int promoted, int flag, int index) {
        int cap = captured;
        int pro = promoted;
        if (captured < 0) { // adjust black piece value to an unsigned int
            cap = mapBlackToUnsignedInt(captured);
        }
        if (promoted < 0) pro = mapBlackToUnsignedInt(promoted);
        return (from) | (to << 6) | (cap << 12) | (pro << 16) | (flag << 20) | (index << 23);
    }

    public static int getIndex(int move) {
        return  (move >> 23) & 0xf; // extract bits 23 - 26
    }

    public static int getFromSquare(int move) {
        return  move & 0x3f; // extract bits 0 - 5
    }

    public static int getTargetSquare(int move) {
        return (move >> 6) & 0x3f; // extract bits 6 - 11
    }

    public static int getCapturedPiece(int move) {
        int piece =  (move >> 12) & 0xf;
        if (piece > 8) return mapCapturedPieceToBlack(piece);
        else return piece;
    }

    public static int getPromotionPiece(int move) {
        int piece = (move >> 16) & 0xf;
        return (piece > 8)  ? mapCapturedPieceToBlack(piece) : piece; // bits 12 - 15
    }

    public static int getFlag(int move) {
        return (move >> 20) & 0x7;
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
        int from = getFromSquare(move);
        int to = getTargetSquare(move);
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

    static int convertsFileRankToIndex(String fileRank) {
        int file = fileRank.charAt(0) - 'a';
        int rank = Integer.parseInt(fileRank.charAt(1) + "") - 1;
        return rank * BoardUtilities.RANK_8 + file;
    }

    public static void main(String[] args) {

    }

}
