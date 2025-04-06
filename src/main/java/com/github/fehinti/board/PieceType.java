package com.github.fehinti.board;

public enum PieceType {
    EMPTY(0, '.'),
    WHITE_PAWN(1, 'P'),
    WHITE_KNIGHT(2, 'N'),
    WHITE_BISHOP(3, 'B'),
    WHITE_ROOK(4, 'R'),
    WHITE_QUEEN(5, 'Q'),
    WHITE_KING(6, 'K'),
    BLACK_PAWN(-1, 'p'),
    BLACK_KNIGHT(-2, 'n'),
    BLACK_BISHOP(-3, 'b'),
    BLACK_ROOK(-4, 'r'),
    BLACK_QUEEN(-5, 'q'),
    BLACK_KING(-6, 'k');

    private final int value;
    private final char name;

    PieceType(int value, char name) {
        this.value = value;
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public boolean isWhite() {
        return this.value > 0;
    }

    public boolean isBlack() {
        return this.value < 0;
    }

    public char getName() {
        return name;
    }

    // utility method to check if piece is valid get piece type from char representation of piece
    public static PieceType getPieceType(char value) {
        for (PieceType pieceType : PieceType.values()) {
            if (pieceType.getName() == value) return pieceType;
        }
        throw new IllegalArgumentException("get PieceType in Enum Class invoked with " + value);
    }

    public static PieceType getPieceType(int value) {
        for (PieceType pieceType : PieceType.values()) {
            if (pieceType.getValue() == value) return pieceType;
        }
        throw new IllegalArgumentException("get PieceType in Enum Class invoked with " + value);
    }
}
