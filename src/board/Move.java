package board;

public class Move {

    private final byte from; // origin square
    private final byte to; // target square
    private final PieceType piece;
    private final MoveType moveType;
    private final boolean sideToPlay;

    public Move(byte from, byte to, boolean side, PieceType piece, MoveType type) {
        this.from = from;
        this.to = to;
        this.piece = piece;
        moveType = type;
        sideToPlay = side;
    }

    public boolean getSideToPlay() {
        return sideToPlay;
    }

    public byte getFrom() {
        return from;
    }

    public byte getTo() {
        return to;
    }

    public PieceType getPieceType() {
        return piece;
    }

    public MoveType getMoveType() {
        return moveType;
    }
}
