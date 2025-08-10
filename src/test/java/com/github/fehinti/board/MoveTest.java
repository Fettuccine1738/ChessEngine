package com.github.fehinti.board;

import com.github.fehinti.piece.Move;
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


    @Test
    void convertsFileRankToIndex() {
    }
}