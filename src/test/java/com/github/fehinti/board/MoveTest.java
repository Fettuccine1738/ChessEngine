package com.github.fehinti.board;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MoveTest {

    private final int mv = Move.encodeMove(0, 1, 2, 3, 4, 5);

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void encodeMove() {
        int e = Move.encodeMove(0, 1, 2, 3, 4, 5);
        assertEquals((1 << 6 | 2 << 12 | 3 << 16 | 4 << 20 | 5 << 23), e);
    }

    //int from, int to, int captured, int promoted, int flag, int index) {
    @Test
    void getIndex() {
        assertEquals(5, Move.getIndex(mv));
    }

    @Test
    void getFromSquare() {
        assertEquals(0, Move.getFromSquare(mv));
    }

    @Test
    void getTargetSquare() {
        assertEquals(1, Move.getTargetSquare(mv));
    }

    @Test
    void getCapturedPiece() {
        assertEquals(2, Move.getCapturedPiece(mv));
    }

    @Test
    void getPromotionPiece() {
        assertEquals(3, Move.getPromotionPiece(mv));
    }

    @Test
    void getFlag() {
        assertEquals(4, Move.getFlag(mv));
    }

    @Test
    void convertsFileRankToIndex() {
    }
}