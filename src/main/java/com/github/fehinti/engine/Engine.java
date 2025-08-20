package com.github.fehinti.engine;

import com.github.fehinti.board.Board120;
import com.github.fehinti.board.Board120Utils;
import com.github.fehinti.board.FENParser;
import com.github.fehinti.piece.MoveGenerator;
import com.github.fehinti.piece.VectorAttack120;
import com.github.fehinti.piece.Move;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Stack;
import java.util.stream.Collectors;

import static com.github.fehinti.board.Board120.KING_SQ;
import static com.github.fehinti.board.Board120Utils.*;

public class Engine {

    record LMove(boolean isMate,  List<Integer> legalMoves) { }
    record TranspositionEntry(double value, byte depthFound, byte nodeType) {}

    private static final double INIT_ALPHA = Double.NEGATIVE_INFINITY;
    private static final double INIT_BETA  = Double.POSITIVE_INFINITY;
    private static final byte  NODE_TYPE_EXACT = 0;
    private static final byte  NODE_TYPE_LOWER = 1;
    private static final byte  NODE_TYPE_UPPER = 2;
    private static final int DRAW_BY_50 = 50;
    private static final int COLOR_WH = 1;
    private static final int MAX_DEPTH = 16;
    private static final boolean MAX_PLAYER = true;
    private static final boolean MIN_PLAYER = false;
    private static final byte SIMPLE = 1;
    private static final byte ADV = 0;

    private final Board120 board;
    private final Board120 gameBoard;
    private final HashMap<Long, TranspositionEntry> transpositionTable;
    private final Evaluator evaluator;
    private int bestMove;
    private final int[][] principalVariation;
    private final int[] pvLength;
    private int ply;
    private int nodecount;

    public Engine(String fen, Evaluator ev) {
        this.board = FENParser.parseFENotation120(fen);
        transpositionTable = new HashMap<>();
        this.evaluator = ev;
        gameBoard = new Board120(board);
        principalVariation = new int[MAX_DEPTH][MAX_DEPTH];
        pvLength = new int[MAX_DEPTH];
        clearPV();
        nodecount = 0;
    }

    public int think() {
        clearPV();
        int bestMove = 0;
        for (int i = 1; i <= 7; i++){
            bestMove = iterativeDeepening(i);
            printPVLine(i);
        }
        return bestMove;
    }

    private void clearPV() {
        for (int i = 0; i < MAX_DEPTH; i++) {
            pvLength[i] = 0;
            for (int j = 0; j < MAX_DEPTH; j++) {
                principalVariation[i][j] = 0;
            }
        }
    }

    private int iterativeDeepening(int depth) {
        boolean side = board.getSideToMove();
        List<Integer> pseudoLegal = MoveGenerator.generatePseudoLegal(board);
        ply = 0;

        for (int i = 0; i < MAX_DEPTH; i++) {
            pvLength[i] = 0;
        }

        if (depth == 1) {
            pseudoLegal = pseudoLegal.stream().filter(mv -> {
                board.make(mv);
                boolean legal = !VectorAttack120.isKingInCheck(board);
                board.unmake(mv);
                return legal;
            }).collect(Collectors.toList());
            MoveGenerator.sortGen(pseudoLegal);
        } else {
            // sort using best move from previous iter then flag(desc) then score,
            // MoveGenerator.sortGen(pseudoLegal);
            final int pvMv = (pvLength[0] > 0) ? principalVariation[0][0] : -1;
            orderMoves(pseudoLegal, pvMv);
           // pseudoLegal.sort((lhs, rhs) -> { // retrieve best move from previous iteration
           //     if (Objects.equals(lhs, pvMv)) return -1;
           //     if (Objects.equals(rhs, pvMv)) return 1;
           //     else {
           //        int lFlag = Move.getFlag(lhs);
           //        int rFlag = Move.getFlag(rhs);
           //        if (lFlag != rFlag) return Integer.compare(rFlag, lFlag);
           //        else {
           //            int lScore = Move.getScore(lhs);
           //            int rScore = Move.getScore(rhs);
           //            return -Integer.compare(rScore, lScore);
           //        }
           //     }
           // });
        }

        int bestMove = pseudoLegal.isEmpty() ? 0: pseudoLegal.get(0);
        double bestEval = Double.NEGATIVE_INFINITY;

        for (int move: pseudoLegal) {
            board.make(move);
            double eval = Double.NEGATIVE_INFINITY;
            nodecount++;
            if (!VectorAttack120.isKingInCheck(board)) {
               ply = 1;
               eval = -negamax(depth - 1, -INIT_BETA, -INIT_ALPHA, side ? -COLOR_WH : COLOR_WH);
                if (eval > bestEval) {
                    bestMove = move;
                    bestEval = eval;
                    principalVariation[0][0] = move;
                    for (int i = 0; i < pvLength[1]; i++) {
                        principalVariation[0][i + 1] = principalVariation[1][i];
                    }
                    pvLength[0] = pvLength[1] + 1;
                }
            }
            board.unmake(move);
        }
        return bestMove;
    }

    private double negamax(int depth, double alpha, double beta, int color) {
        double alphaOrig = alpha;
        pvLength[ply] = 0;

        TranspositionEntry tEntry = transpositionTable.get(board.getZobristHash());
        if (tEntry != null && tEntry.depthFound >= depth) {
            double value = tEntry.value;
            if (tEntry.nodeType == NODE_TYPE_EXACT) return value;
            else if (tEntry.nodeType == NODE_TYPE_LOWER && value >= beta ) {
                return value;
            } else if (tEntry.nodeType == NODE_TYPE_UPPER && value <= alpha ) {
                return value;
            }
        }

        if (depth == 0) return  color * evaluator.evaluate(board);
        List<Integer> child = MoveGenerator.generatePseudoLegal(board);

        // check for check / stalemate
        boolean legal = false;
        for (int m : child) {
            board.make(m);
            if (!VectorAttack120.isKingInCheck(board)) {
                legal = true;
                board.unmake(m);
                break;
            }
            board.unmake(m);
        }

        if (!legal) {
            if (VectorAttack120.isKingInCheck(board)) {
                return -(100_000 - depth);
            } else {
                return 0;
            }
        }

        // MoveGenerator.sortMoves(child);
        final int pvMove = (ply < MAX_DEPTH - 1 && pvLength[0] > ply) ? principalVariation[ply][0] : -1;

        orderMoves(child, pvMove);

        double bestEval = Double.NEGATIVE_INFINITY;
        int bestMove = 0;

        for (Integer mv : child) {
            board.make(mv);
            nodecount++;
            double eval = Double.NEGATIVE_INFINITY;
            if (!VectorAttack120.isKingInCheck(board)) {
                ply++;
                eval = -negamax(depth - 1, -beta, -alpha, -color);
                ply--;
                if (eval > bestEval) {
                    bestEval = eval;
                    bestMove = mv;

                    principalVariation[ply][0] = mv;
                    for (int i = 0; i < pvLength[ply + 1]; i++) {
                        principalVariation[ply][i + 1] = principalVariation[ply + 1][i];
                    }
                    pvLength[ply] = pvLength[ply + 1] + 1;
                }
            }
            board.unmake(mv);
            alpha = Math.max(alpha, eval);
            if (alpha >= beta) break; // beta cutoff
        }

        byte nodeType;

        if (bestEval <= alphaOrig) nodeType = NODE_TYPE_UPPER;
        else if (bestEval >= beta) nodeType = NODE_TYPE_LOWER;
        else nodeType = NODE_TYPE_EXACT;

        TranspositionEntry newEntry = new TranspositionEntry(bestEval, (byte) depth, nodeType);
        transpositionTable.put(board.getZobristHash(), newEntry);
        return bestEval;
    }

    private void orderMoves(List<Integer> unsorted, int pv) {
        unsorted.sort((lhs, rhs) -> {
            if (lhs == pv) return -1;
            if (rhs == pv) return 1;

            int f1 = Move.getFlag(lhs);
            int f2 = Move.getFlag(lhs);
            if (f1 != f2) return Integer.compare(f2, f1);

            int s1 = Move.getScore(lhs);
            int s2 = Move.getScore(lhs);
            return Integer.compare(s2, s1);
        });
    }
   public boolean isGameDrawn() {
        return isDrawBy50MoveRule() || isDrawByThreefold() || drawByInsufficientMaterial();
    }

    public boolean isCheckMate() {
        List<Integer> moves = MoveGenerator.generatePseudoLegal(board);
        Iterator<Integer> iterator = moves.iterator();

        while (iterator.hasNext()) {
            int mv =  iterator.next();
            board.make(mv);
            if (VectorAttack120.isKingInCheck(board)) {
                iterator.remove();
            }
            board.unmake(mv);
        }
        // no valid moves left
        return moves.isEmpty();
    }

    private double evalTerminalNode(int depth, int color) {
        List<Integer> moves = MoveGenerator.generatePseudoLegal(board);
        boolean foundLegal = false;
        Iterator<Integer> iterator = moves.iterator();
        while (iterator.hasNext()) {
            int mv =  iterator.next();
            if (!VectorAttack120.isKingInCheck(board)) {
                foundLegal = true;
                board.unmake(mv);
                break;
            } else {
                iterator.remove();
            }
            board.unmake(mv);
        }
        if (!foundLegal) {
            if (VectorAttack120.isKingInCheck(board)) {
                return -(100_000 - depth);
            } else return 0;
        }

        if (isGameDrawn()) return 0;

        return Double.NaN;
    }

    private void printPVLine(int depth) {
        System.out.print("PV (depth " + depth + "): ");
        for (int i = 0; i < pvLength[0] && i < depth; i++) {
            int m = principalVariation[0][i];
            if (m != 0)  System.out.print(Move.printMove(m) + " ");
        }
        System.out.println();
    }

    private List<Integer> getPV() {
        List<Integer> pv = new ArrayList<Integer>();
        for (int i = 0; i < pvLength[0]; i++) {
            pv.add(principalVariation[0][i]);
        }
        return pv;
    }

    private boolean isDrawBy50MoveRule() {
        return board.getHalfMoveClock() == DRAW_BY_50;
    }

    private boolean isDrawByThreefold() {
        long currentHash = board.getZobristHash();
        int rep = 0;
        long[] hashes = board.getHashHistory();

        int halfMoves = board.getHalfMoveClock();
        int st = board.getPly() - 2; // skip current
        for (int i = st; i >= st - halfMoves && i >= 0; i -= 2) {
            if (hashes[i] == currentHash) {
                rep++;
                if (rep >= 2) return true; // current + 2 previous = threefold
            }
        }
        return false;
    }

    private boolean drawByInsufficientMaterial() {
        int[] wList = board.getWhitePieceList();
        int[] bList = board.getBlackPieceList();

        // [N, B, PRQ] count the number of knights, bishop and other pieces on the board
        // estimates draws based on piece  combinations;
        int[] wPc = new int[3];
        int[] bPc = new int[3];
        int bSquare = 0;
        int wSquare = 0;

       for (int i = 0; i < 16; i++) {
            // return early, checkmate possible with (pawn, rook and queen) remaining
            if (wPc[2] > 2 || bPc[2] > 2) return false;

            int wp = (wList[i] >> 8) & 0xff;
            int bp = (bList[i] >> 8) & 0xff;
            if (wp == WKNIGHT) wPc[0]++;
            else if (wp == WBISHOP) {
                wPc[1]++;
                wSquare = wList[i] & 0xff;
            }
            else if (wList[i] != OFF_BOARD) wPc[2]++;

            if (bp == -BKNIGHT) bPc[0]++;
            else if (bp == -BBISHOP) {
                bPc[1]++;
                bSquare = bList[i] & 0xff;
            }
            else if (bList[i] != OFF_BOARD) bPc[2]++;
        }
        // King vs. king
        boolean  onlyBlackKing = bPc[0] == 0 && bPc[1] == 0 && bPc[2] == 1;
        boolean  onlyWhiteKing = wPc[0] == 0 && wPc[1] == 0 && wPc[2] == 1;
        boolean  whiteKingKnight = (wPc[0] == 1 && wPc[1] == 0 && wPc[2] == 1);
        boolean  blackKingKnight = (bPc[0] == 1 && bPc[1] == 0 && bPc[2] == 1);
        boolean  whiteKingBishop = (wPc[0] == 0 && wPc[1] == 1 && wPc[2] == 1);
        boolean  blackKingBishop = (bPc[0] == 0 && bPc[1] == 1 && bPc[2] == 1);

        if (onlyWhiteKing &&  onlyBlackKing) return true; // King vs king
        else if ( whiteKingKnight &&  onlyBlackKing)  return true; // KN vs k
        else if ( blackKingKnight &&  onlyWhiteKing)  return true; // kn vs K
        else if ( whiteKingBishop &&  onlyBlackKing)  return true;
        else if ( blackKingBishop &&  onlyWhiteKing)  return true;
        else if ( whiteKingBishop && blackKingBishop) {
            // check if the bishops are of the same color => stalemate
            for (int j = 0; j < KING_SQ; j++) {
                if (((bList[j] >> 8) & 0xff) == -BBISHOP) bSquare  = bList[j] & 0xff;
                if (((wList[j] >> 8) & 0xff) == WBISHOP)  wSquare  = wList[j] & 0xff;
            }
            return Board120Utils.COLOR[bSquare] == Board120Utils.COLOR[wSquare]; // same color = draw
        }
        return false;
    }


    public static void main(String[] args) {
        String fen = "r2q1rk1/pP1p2pp/Q4n2/bbp1p3/Np6/1B3NBn/pPPP1PPP/R3K2R b KQ - 0 1";
        Engine engine = new Engine(fen, WeightedCombiEval.getInstance());
        int best = engine.think();
        System.out.println("Best Move " + Move.printMove(best));
        System.out.println(engine.nodecount);

    }
}
