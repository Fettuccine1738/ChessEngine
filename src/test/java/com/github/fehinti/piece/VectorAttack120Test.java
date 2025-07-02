package com.github.fehinti.piece;

import com.github.fehinti.board.Board;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.github.fehinti.piece.VectorAttack120.*;
import static com.github.fehinti.piece.VectorAttack120Test.BoardPiece.*;
import static org.junit.jupiter.api.Assertions.*;

class VectorAttack120Test {

    // this record holds for every 0..63 index on the board, which
    // squares are reachable
    record PieceFromToSquares(int from, int[]  reachables, BoardPiece bp) { }
    enum BoardPiece {
        PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING
    }


    // *NOTE: this method does not test pawn moves/ they are tested on the fly.
    @ParameterizedTest
    @MethodSource("generatePieceFromToSquares")
    void testsIfToCanBeReachedByPieceOnFrom(PieceFromToSquares kn) {
        int fr = Board.getMailbox64Number(kn.from);
        for (int i = 0; i < kn.reachables.length; i++) {
            int to = Board.getMailbox64Number(kn.reachables[i]);
            assertTrue(VectorAttack120.isSquareAttacked(fr, to));
        }
    }

    @ParameterizedTest
    @MethodSource("generatePieceFromToSquares")
    void testsFalseFlags(PieceFromToSquares kn) {
        int fr = Board.getMailbox64Number(kn.from);
        for (int i = 0; i < kn.reachables.length; i++) {
            int to = Board.getMailbox64Number(kn.reachables[i]);
            int result = VectorAttack120.ATTACK_ARRAY[to - fr + 119];
            assertFalse(getPiecePredicates(BISHOP).test(result), "Mismatch in " + fr + " to " + to);
        }
    }

    @ParameterizedTest
    @MethodSource("generatePieceFromToSquares")
    void testsIfAttacksAreFlaggedCorrectly(PieceFromToSquares kn) {
        int fr = Board.getMailbox64Number(kn.from);
        for (int i = 0; i < kn.reachables.length; i++) {
            int to = Board.getMailbox64Number(kn.reachables[i]);
            int result = VectorAttack120.ATTACK_ARRAY[to - fr + 119];
            assertTrue(getPiecePredicates(kn.bp).test(result), "Mismatch in " + fr + " to " + to);
        }
    }



    static Predicate<Integer> getPiecePredicates(BoardPiece piece) {
        Predicate<Integer> knight = Set.of(ATTACK_N)::contains;
        Predicate<Integer> bishop = Set.of(ATTACK_KQBwP, ATTACK_KQBbP, ATTACK_QB)::contains;
        Predicate<Integer> rook =   Set.of(ATTACK_KQR, ATTACK_QR)::contains;
        Predicate<Integer> king =   Set.of(ATTACK_KQR, ATTACK_KQBwP, ATTACK_KQBbP)::contains;
        Predicate<Integer> queen =  Set.of(ATTACK_KQR, ATTACK_QR, ATTACK_QB, ATTACK_KQBwP, ATTACK_KQBbP)::contains;

        switch (piece) {
            case  KNIGHT -> {return knight; }
            case  BISHOP -> { return bishop; }
            case  ROOK -> { return rook; }
            case  QUEEN -> { return queen; }
            case  KING -> { return king; }
        }
        throw new IllegalArgumentException("No precomputed map for this piece");
    }

    // return from square and possible to square assert that to is reachable by from
    static Stream<PieceFromToSquares> generatePieceFromToSquares() {
        List<PieceFromToSquares> pieceMovesList = new ArrayList<>();
        for (BoardPiece piece : BoardPiece.values()) {
            if (piece != ROOK) continue;
            // inverse tests if rook and bishop not traversing the same ray.
            // if (piece != VectorAttackTest.BoardPiece.BISHOP) continue;
            int[][] precomputedPieceMaps = AttackMap.computedAttackMaps(piece.ordinal());
            for (int i = 0; i < precomputedPieceMaps.length; i++) {
                if (precomputedPieceMaps[i].length != 0) {
                    pieceMovesList.add(new PieceFromToSquares(i, precomputedPieceMaps[i], piece));
                }
            }
        }
        return pieceMovesList.stream();
    }
}