package board;

public class Move {


    // tactical move flags
    public static final int FLAG_QUIET = 0;
    public static final int FLAG_EN_PASSANT = 1;
    public static final int FLAG_CASTLE = 2;
    public static final int FLAG_DOUBLE_PAWN_PUSH = 3;
    public static final int FLAG_CAPTURE = 4;
    public static final int FLAG_PROMOTION = 5;

    public static int encodeMove(int from, int to, int captured, int promoted, int flag) {
        int cap = captured;
        int pro = promoted;
        if (captured < 0) { // adjust black piece value to an unsigned int
            cap = mapBlackToUnsignedInt(captured);
        }
        if (promoted < 0) pro = mapBlackToUnsignedInt(promoted);
        return (from) | (to << 6) | (cap << 12) | (pro << 16) | (flag << 20);
    }

    public static int getFromSquare(int move) {
        return  move & 0x3f; // extract bits 0 - 5
    }

    public static int getTargetSquare(int move) {
        return (move >> 6) & 0x3f; // extract bits 6 - 11
    }

    public static int getCapturedPiece(int move) {
        int piece =  (move >> 12) & 0xf; // extract bits 6 - 11
        if (piece > 8) return mapCapturedPieceToBlack(piece);
        else return piece;
    }

    public static int getPromotionPiece(int move) {
        int piece = (move >> 16) & 0xf;
        return (piece > 8)  ? mapCapturedPieceToBlack(piece) : piece; // bits 12 - 15
    }

    public static int getFlags(int move) {
        return (move >> 20) & 0xf; // bits 12 - 15
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


    public static void main(String[] args) {
        int move = encodeMove(12, 28,0, 0, FLAG_EN_PASSANT);

        System.out.println();
        System.out.println("From square: " + getFromSquare(move)); // Output: 12
        System.out.println("To square: " + getTargetSquare(move));     // Output: 28
        System.out.println("Captured: " + getCapturedPiece(move)); // Output: 0
        System.out.println("Promotion: " + getPromotionPiece(move)); // Output: 0
        System.out.println("Flags: " + getFlags(move));            // Output: 1



    }

}
