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

    public Engine(String fen, int eval) {
        this.board = FENParser.parseFENotation120(fen);
        transpositionTable = new HashMap<>();
        evaluator = (eval == 0) ? WeightedCombiEval.getInstance()
                : (eval == 1) ? PESTO.getInstance() : SimpleEvaluator.getInstance();
        gameBoard = new Board120(board);
        principalVariation = new int[MAX_DEPTH][MAX_DEPTH];
        pvLength = new int[MAX_DEPTH];
    }

    public int think() {
        int i = 1;
        int bestMove = 0;
        while (i <= 8) {
            bestMove = iterativeDeepening(i++);
            printPVLine(i - 1);
        }
        return bestMove;
    }

    private int iterativeDeepening(int depth) {
        boolean side = gameBoard.getSideToMove();
        List<Integer> pseudoLegal = MoveGenerator.generatePseudoLegal(board);
        ply = 0;

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
            MoveGenerator.sortGen(pseudoLegal);
            pseudoLegal.sort((lhs, rhs) -> { // retrieve best move from previous iteration
                if (Objects.equals(lhs, principalVariation[depth - 2][depth - 1])) return -1;
                if (Objects.equals(rhs, principalVariation[depth - 2][depth - 1])) return 1;
                else {
                   int lFlag = Move.getFlag(lhs);
                   int rFlag = Move.getFlag(rhs);
                   if (lFlag != rFlag) return -Integer.compare(lFlag, rFlag);
                   else {
                       int lScore = Move.getScore(lhs);
                       int rScore = Move.getScore(rhs);
                       return -Integer.compare(lScore, rScore);
                   }
                }
            });
        }

        int bestMove = pseudoLegal.getFirst();
        double bestEval = Double.NEGATIVE_INFINITY;

        for (int move: pseudoLegal) {
            board.make(move);
            double eval = Double.NEGATIVE_INFINITY;
            if (!VectorAttack120.isKingInCheck(board)) {
               eval = -negamax(depth - 1, INIT_ALPHA, INIT_BETA, side ? COLOR_WH: -COLOR_WH);
            }
            board.unmake(move);
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
        return bestMove;
    }

    private double negamax(int depth, double alpha, double beta, int color) {
        double alphaOrig = alpha;
        pvLength[ply] = ply;

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
        // MoveGenerator.sortMoves(child);
        final int pvMove = (ply < MAX_DEPTH - 1 && pvLength[0] > ply) ? principalVariation[0][ply] : -1;

        child.sort((lhs, rhs) -> {
            if (lhs == principalVariation[0][ply]) return -1;
            if (rhs == principalVariation[0][ply]) return 1;

            int f1 = Move.getFlag(lhs);
            int f2 = Move.getFlag(rhs);
            if (f1 != f2) return -Integer.compare(f1, f2);

            int s1 = Move.getScore(lhs);
            int s2 = Move.getScore(rhs);
            return -Integer.compare(s1, s2);
        });

        double bestEval = Double.NEGATIVE_INFINITY;
        if (child.isEmpty()) return 0; // TODO : checkmate ? draw/ terminal node

        for (Integer mv : child) {
            board.make(mv);
            ply++;
            double eval = Double.NEGATIVE_INFINITY;
            if (!VectorAttack120.isKingInCheck(board)) {
                eval = -negamax(depth - 1, -beta, -alpha, -color);
                if (eval > bestEval) {
                    bestEval = eval;
                    bestMove = mv;

                    principalVariation[ply - 1][ply - 1] = mv;
                    for (int i = 0; i < pvLength[ply]; i++) {
                        principalVariation[ply - 1][0] = principalVariation[ply][i];
                    }
                    pvLength[ply - 1] = pvLength[ply];
                }
            }
            ply--;
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

    private void printPVLine(int depth) {
        System.out.print("PV (dpeth " + depth + "): ");
        for (int i = 0; i < pvLength[0]; i++) {
            System.out.print(Move.printMove(principalVariation[0][i]) + " ");
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
        // todo
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
            else wPc[2]++;
            if (bp == -BKNIGHT) bPc[0]++;
            else if (bp == -BBISHOP) {
                bPc[1]++;
                bSquare = bList[i] & 0xff;
            }
            else bPc[2]++;
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
}
