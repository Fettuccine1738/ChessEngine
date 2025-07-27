package com.github.fehinti.engine;

import com.github.fehinti.board.Board120;
import com.github.fehinti.board.Board120Utils;
import com.github.fehinti.board.FENParser;
import com.github.fehinti.piece.MoveGenerator;
import com.github.fehinti.piece.VectorAttack120;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import static com.github.fehinti.board.Board120.KING_SQ;
import static com.github.fehinti.board.Board120Utils.*;

public class Engine {

    record Move(boolean isMate,  List<Integer> Legalmoves) {
    }

    private static final double INIT_ALPHA = Double.NEGATIVE_INFINITY;
    private static final double INIT_BETA  = Double.POSITIVE_INFINITY;
    private static final int DRAW_BY_50 = 50;
    private static final boolean MAX_PLAYER = true;
    private static final boolean MIN_PLAYER = false;
    private static final byte SIMPLE = 1;
    private static final byte ADV = 0;

    private final Board120 board;
    private HashMap<Long, Integer> previousEval;
    private final Evaluator evaluator;

    public Engine(String fen, int eval) {
        this.board = FENParser.parseFENotation120(fen);
        previousEval = new HashMap<>();
        evaluator = (eval == 0) ? PESTO.getInstance() : SimpleEvaluator.getInstance();
    }

    public int search() {
        return 0;
    }

    // negamax form of alphabeta, were both sides are maximizing their scores
    private double alphaBeta(int depth, double alpha, double beta) {
        if (depth == 0) return  evaluator.evaluate(board);
        List<Integer> child = MoveGenerator.generatePseudoLegal(board);
        double eval = Double.NEGATIVE_INFINITY;
        if (child.isEmpty()) return 0; // TODO : checkmate ? draw
        for (Integer mv : child) {
            board.make(mv);
            if (!VectorAttack120.isKingInCheck(board)) {
                eval = -alphaBeta(depth - 1, -beta, -alpha);
            }
            board.unmake(mv);
            if (eval >= beta) return beta;
            alpha = Math.max(alpha, eval);
        }
        return alpha;
    }

    // code implementation from
    // https://en.wikipedia.org/wiki/Alpha%E2%80%93beta_pruning
    private double alphaBeta(int depth, double alpha, double beta, boolean maximizingplayer) {
        List<Integer> child = MoveGenerator.generatePseudoLegal(board);
        if (depth == 0) return evaluator.evaluate(board);
        // checkmate or stalemate // return worst possible scores
        if (child.isEmpty()) return (maximizingplayer ? INIT_ALPHA : INIT_BETA); // todo
        double eval;
        if (maximizingplayer) {
            eval = Double.NEGATIVE_INFINITY; // worst possible score for max player
            for (Integer move : child) {
                board.make(move);
                if (!VectorAttack120.isKingInCheck(board)) {
                    eval = Math.max(eval, alphaBeta(depth - 1, alpha, beta, false));
                    if (eval >= beta) {
                        // value we can get by going down this path is larger than what we already found
                        // the minimzer will never go down this route (beta - cutoff)
                        board.unmake(move);
                        break; // cutoff
                    }
                    alpha = Math.max(alpha, eval);
                }
                board.unmake(move);
            }
        } else {
            eval = Double.POSITIVE_INFINITY; // worst possible score for minimizer
            for (Integer move : child) {
                board.make(move);
                if (!VectorAttack120.isKingInCheck(board)) {
                    eval = Math.min(eval, alphaBeta(depth - 1, alpha, beta, true));
                    if (eval <= alpha) {
                        board.unmake(move);
                        break;
                    }
                    beta = Math.min(beta, eval);
                }
                board.unmake(move);
            }
        }
        return eval;
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

        // [N, B, PRQ]
        int[] wPc = new int[3];
        int[] bPc = new int[3];

       for (int i = 0; i < 16; i++) {
            // return early, checkmate is still possible with pieces remaining
            if (wPc[2] > 2 || bPc[2] > 2) return false;
            int wp = (wList[i] >> 8) & 0xff;
            int bp = (bList[i] >> 8) & 0xff;
            if (wp == WKNIGHT) wPc[0]++;
            else if (wp == WBISHOP) wPc[1]++;
            else wPc[2]++;
            if (bp == -BKNIGHT) bPc[0]++;
            else if (bp == -BBISHOP) bPc[1]++;
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
            int bSquare = 0;
            int wSquare = 0;
            for (int j = 0; j < KING_SQ; j++) {
                if (((bList[j] >> 8) & 0xff) == -BBISHOP) bSquare  = bList[j] & 0xff;
                if (((wList[j] >> 8) & 0xff) == WBISHOP)  wSquare  = wList[j] & 0xff;
            }
            return Board120Utils.COLOR[bSquare] == Board120Utils.COLOR[wSquare]; // same color = draw
        }
        return false;
    }

}
