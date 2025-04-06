package com.github.fehinti.board;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoardTest {
    static String FEN_1 = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    static String FEN_2 = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1";
    static String FEN_3 = "rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq c6 0 2";
    static String FEN_4 = "rnbqkbnr/pp1ppppp/8/2p5/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2";
    static String FEN_K2 =  "8/8/8/3K4/8/8/8/8 w - - 0 1"; // single king 8 coordinates test
    static String FEN_K1 =  "8/8/8/8/8/8/8/K7 w - - 0 1"; // single king offboard test
    static String FEN_B =  "8/8/3p4/2p1B3/3b1P2/4P3/8/8 w - - 0 1"; // bishop off board test
    static String FEN_R =  "8/8/8/3p4/2r1R3/4P3/8/8 w - - 0 1"; // rook move generation test
    static String FEN_Q =  "8/8/8/3p1q2/2Q3P1/4P3/8/8 w - - 0 1"; // queen move generation test
    static String FEN_P =  "8/8/3p4/2P1p3/8/3P4/4P3/8 w - - 0 1"; // pawn move testing
    static String FEN_N = "8/3p4/8/4N3/3n1P2/8/4P3/8 w - - 0 1"; // knight move test


    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void getBoard64() {
    }

    @Test
    void getWhitePieceList() {
    }

    @Test
    void getBlackPieceList() {
    }

    @Test
    void getPlayHistory() {
    }

    @Test
    void getIrreversibleAspect() {
    }

    @Test
    void getSideToMove() {
    }

    @Test
    void setFullMoveCounter() {
    }

    @Test
    void setHalfMoveClock() {
    }

    @Test
    void setCastlingRights() {
    }

    @Test
    void setEnPassant() {
    }

    @Test
    void getEnPassant() {
    }

    @Test
    void getCastlingRights() {
    }

    @Test
    void getHalfMoveClock() {
    }

    @Test
    void getFullMoveCounter() {
    }

    @Test
    void encodeCastlingRights() {
    }

    @Test
    void canSideCastle() {
    }

    @Test
    void canWhiteCastleKingside() {
    }

    @Test
    void canWhiteCastleQueenside() {
    }

    @Test
    void canBlackCastleKingside() {
    }

    @Test
    void canBlackCastleQueenside() {
    }

    @Test
    void getPieceOnBoard() {
    }

    @Test
    void getMailbox64Number() {
    }

    @Test
    void getMailbox120Number() {
    }
}