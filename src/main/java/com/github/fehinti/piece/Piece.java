package com.github.fehinti.piece;

public enum Piece {
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

    Piece(int value, char name) {
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
    public static Piece getPiece(char value) {
        for (Piece Piece : Piece.values()) {
            if (Piece.getName() == value) return Piece;
        }
        throw new IllegalArgumentException("get Piece in Enum Class invoked with " + value);
    }

    public static Piece getPiece(int value) {
        for (Piece Piece : Piece.values()) {
            if (Piece.getValue() == value) return Piece;
        }
        throw new IllegalArgumentException("get Piece in Enum Class invoked with " + value);
    }
}
