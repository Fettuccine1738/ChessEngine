package test;


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

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
    }

    @org.junit.jupiter.api.Test
    void getWhitePieceList() {
    }

    @org.junit.jupiter.api.Test
    void getBlackPieceList() {
    }

    @org.junit.jupiter.api.Test
    void getSideToMove() {
    }

    @org.junit.jupiter.api.Test
    void setFullMoveCounter() {
    }

    @org.junit.jupiter.api.Test
    void setHalfMoveClock() {
    }

    @org.junit.jupiter.api.Test
    void setCastlingRights() {
    }

    @org.junit.jupiter.api.Test
    void setEnPassant() {
    }

    @org.junit.jupiter.api.Test
    void getEnPassant() {
    }

    @org.junit.jupiter.api.Test
    void getCastlingRights() {
    }

    @org.junit.jupiter.api.Test
    void getHalfMoveClock() {
    }

    @org.junit.jupiter.api.Test
    void getFullMoveCounter() {
    }

    @org.junit.jupiter.api.Test
    void encodeCastlingRights() {
    }

    @org.junit.jupiter.api.Test
    void canWhiteCastleKingside() {
    }

    @org.junit.jupiter.api.Test
    void canWhiteCastleQueenside() {
    }

    @org.junit.jupiter.api.Test
    void canBlackCastleKingside() {
    }

    @org.junit.jupiter.api.Test
    void canBlackCastleQueenside() {
    }

    @org.junit.jupiter.api.Test
    void getPieceOnBoard() {
    }

    @org.junit.jupiter.api.Test
    void getMailbox64Number() {
    }

    @org.junit.jupiter.api.Test
    void getMailbox120Number() {
    }

    @org.junit.jupiter.api.Test
    void make() {
    }

    @org.junit.jupiter.api.Test
    void unmake() {
    }
}