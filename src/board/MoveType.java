package board;

public enum MoveType {
    NULLMOVE(0),
    NORMAL(1),
    CAPTURE(2),
    CASTLE(3),
    PROMOTION(4),
    PROMOTION_CAPTURE(5),
    ENPASSANT(6);

    private final int value;

    MoveType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
