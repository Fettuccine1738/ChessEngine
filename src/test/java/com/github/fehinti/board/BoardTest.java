package com.github.fehinti.board;

import com.github.fehinti.piece.PieceMove;
import edu.princeton.cs.algs4.In;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static com.github.fehinti.board.PieceType.*;
import static com.github.fehinti.piece.PieceMove.pseudoLegal;
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

    private Board board;
    private static Random random;

    static int convertsFileRankToIndex(String fileRank) {
        int file = fileRank.charAt(0) - 'a';
        int rank = Integer.parseInt(fileRank.charAt(1) + "") - 1;
        return rank * BoardUtilities.RANK_8 + file;
    }

    @BeforeAll
    static void initAll() {
        random = new Random();
    }

    @AfterAll
    static void tearDownAll() {
        System.out.println("Test completed");
    }


    @BeforeEach
    void setUp() {
        board = new Board();
        assertEquals(WHITE_KING, board.getPieceOnBoard(convertsFileRankToIndex("e1")));
        assertEquals(WHITE_QUEEN, board.getPieceOnBoard(convertsFileRankToIndex("d1")));
        assertEquals(BLACK_KING, board.getPieceOnBoard(convertsFileRankToIndex("e8")));
        assertEquals(BLACK_QUEEN, board.getPieceOnBoard(convertsFileRankToIndex("d8")));
    }

    @AfterEach
    void tearDown() {
    }

    // getboard64() returns a copy, disallowing direct modification
    // of boards own 8x8 board
    @Test
    void testsGetBoard64ReturnsACopy() {
        PieceType[] original = board.getBoard64();
        PieceType[] copy = board.getBoard64();
        //swap king and queen
        PieceType temp = copy[5];
        copy[5] = copy[4];
        copy[4] = temp;
        assertFalse(Arrays.equals(board.getBoard64(), copy));
        // assert move is not made on board's own 8x8 array
        assertArrayEquals(original, board.getBoard64());
        // check we are not making a shallow copy
        assertNotSame(board.getBoard64(), board.getBoard64());
    }

    @Test
    void testWhitePieceListIsCorrectlyInit() {
        int count = 0;
        int[] whitePieceList = board.getWhitePieceList();
        int[] actualWhiteSquares = new int[16];
        int index = 0;
        int[] expectedWhiteSquares = {
                0, 1, 2, 3, 4, 5, 6, 7, // back rank
                8, 9, 10, 11, 12, 13, 14, 15 // pawns
        };
        for (int i : whitePieceList) {
            if (i != 0) {
                count++;
                actualWhiteSquares[index++] = (i & 0xff);
            }
        }
        // sorting required because array is ordered in terms of Piece
        // precedence
        Arrays.sort(actualWhiteSquares);
        assertEquals(16, count);
        assertArrayEquals(expectedWhiteSquares, actualWhiteSquares);
    }

    @Test
    void testsBlackPieceListIsCorrectlyInit() {
        int count = 0;
        int[] blackPieceList = board.getBlackPieceList();
        int[] actualBlackSquares = new int[16];
        int index = 0;
        int[] expectedBlackSquares = {
                48, 49, 50, 51, 52, 53, 54, 55, // pawns
                56, 57, 58, 59, 60, 61, 62, 63 // back rank
        };
        for (int i : blackPieceList) {
            if (i != 0) {
                count++;
                actualBlackSquares[index++] = (i & 0xff);
            }
        }
        // sorting required because array is ordered in terms of Piece
        // precedence
        Arrays.sort(actualBlackSquares);
        assertEquals(16, count);
        assertArrayEquals(expectedBlackSquares, actualBlackSquares);
    }

    @Test
    void testsPlayHistoryUpdatesCorrectly() {
        Board b = new Board();
        assertEquals(0, b.getHalfMoveClock());

        int[] history = b.getPlayHistory();
        int histCount = 0;
        for (int i : history) {
            if (i != 0) histCount++;
        }
        assertEquals(0, histCount, "initial history should be empty");
        // first move by white
        List<Integer> pseudoMoves = pseudoLegal(b);
        int mv = pseudoMoves.get(random.nextInt(pseudoMoves.size()));
        b.make(mv); // white moves for the first time
        System.out.println(b + "\n");

        int[] historyAfterWhite = b.getPlayHistory();
        assertEquals(mv, historyAfterWhite[0]);

        // generate black moves
        List<Integer> blackMoves = pseudoLegal(b);
        int blackMv = blackMoves.get(random.nextInt(blackMoves.size()));
        b.make(blackMv);
        System.out.println(b + "\n");
        int[] historyAfterBlack = b.getPlayHistory();
        assertEquals(blackMv, historyAfterBlack[1]);
    }

    // test that game state that are modified are saved correctly
    // before modification and allows returning to the state before
    // a move is made
    @Test
    void testsIrreversibleAspectsIsValid() {
        int count = 0;
        for (int i : board.getIrreversibleAspect()) {
            if (i != 0) count++;
        }
        // assert that irreversible apects is initialized correctly
        //  = no move has been made yet.
        int[] expectedIrreversibleAspect = board.getIrreversibleAspect();
        assertEquals(0, count);
        assertEquals(0, expectedIrreversibleAspect[0]);
        assertEquals(0, expectedIrreversibleAspect[1]);

        List<Integer> whiteMoves = pseudoLegal(board);
        int white = whiteMoves.get(random.nextInt(whiteMoves.size()));
        int ep = board.getEnPassant() & 0x3f;
        int cR = (board.getCastlingRights() & 0xF) << 6; // Shift and mask to 4 bits
        int hM = (board.getHalfMoveClock() & 0x3F) << 10; // Shift and mask to 6 bits
        board.make(white);
        int reverse = ep | cR | hM;
        assertEquals(reverse, board.getIrreversibleAspect()[0]);


        List<Integer> blackMoves = pseudoLegal(board);
        int black = blackMoves.get(random.nextInt(blackMoves.size()));
        int bEp = board.getEnPassant() & 0x3f;
        int bCr = (board.getCastlingRights() & 0xF) << 6; // Shift and mask to 4 bits
        int bHm = (board.getHalfMoveClock() & 0x3F) << 10; // Shift and mask to 6 bits
        int irrv = bEp | bCr | bHm;
        board.make(black);
        assertEquals(irrv, board.getIrreversibleAspect()[1]);
    }

    @Test
    void testsSideToMoveAlternatesCorrectly() {
        // initial side to move
        boolean whiteToMove = board.getSideToMove();
        assertTrue(whiteToMove);
        List<Integer> whiteMoves = pseudoLegal(board);
        int whiteMove = whiteMoves.get(random.nextInt(whiteMoves.size()));
        board.make(whiteMove);

        // assert it's black turn to move
        assertFalse(board.getSideToMove());

        // undo whites move
        board.unmake(whiteMove);
        assertTrue(board.getSideToMove()); // still white to play
        // choose a move again and play
        whiteMove = whiteMoves.get(random.nextInt(whiteMoves.size()));
        board.make(whiteMove);

        // assert it's black turn to move
        assertFalse(board.getSideToMove());

        List<Integer> blackMoves = pseudoLegal(board);
        int blackMove = blackMoves.get(random.nextInt(blackMoves.size()));
        board.make(blackMove);

        // whites turn to move
        assertTrue(board.getSideToMove());

        // undo moves and assert it is still black to move
        // Undoing a move reverts the board to the exact previous state.
        // So if Black just moved, undoing that should return the turn to Black.
        board.unmake(blackMove);
        assertFalse(board.getSideToMove());
    }

    // Fullmove number: The number of the full moves.
    // It starts at 1 and is incremented after Black's move.
    @Test
    void testsFullMoveUpdatesAfterBlack() {
        List<Integer> whiteMoves = pseudoLegal(board);
        int whiteMove = whiteMoves.get(random.nextInt(whiteMoves.size()));
        board.make(whiteMove);
        assertEquals(1, board.getFullMoveCounter());

        board.unmake(whiteMove);
        assertEquals(1, board.getFullMoveCounter());

        board.make(whiteMoves.get(random.nextInt(whiteMoves.size())));
        assertEquals(1, board.getFullMoveCounter());

        List<Integer> blackMoves = pseudoLegal(board);
        int blackMove = blackMoves.get(random.nextInt(blackMoves.size()));
        board.make(blackMove);
        assertEquals(2, board.getFullMoveCounter(), "After black moves, full move count is now 2");
        board.unmake(blackMove);
        assertEquals(1, board.getFullMoveCounter());
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