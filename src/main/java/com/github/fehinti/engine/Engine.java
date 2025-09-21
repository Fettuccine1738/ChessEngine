package com.github.fehinti.engine;

import org.slf4j.Logger;
import com.github.fehinti.LogManager;
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
import java.util.stream.Collectors;

import static com.github.fehinti.board.Board120Utils.*;

public class Engine {

    record LMove(boolean isMate,  List<Integer> legalMoves) { }
    record TranspositionEntry(double eval, byte depth, byte nodeType) {}

    private static final Logger logger = LogManager.getClassLogger(Engine.class);
    private static final double INIT_ALPHA = Double.NEGATIVE_INFINITY;
    private static final double INIT_BETA  = Double.POSITIVE_INFINITY;
    private static final byte  NODE_TYPE_EXACT = 0;
    private static final byte  NODE_TYPE_LOWER = 1;
    private static final byte  NODE_TYPE_UPPER = 2;
    private static final int DRAW_BY_50 = 50;
    private static final int COLOR_WH = 1;
    private static final int MAX_DEPTH = 16;
    private static final int DRAWN = 0;

    private final Board120 _board;
    private final HashMap<Long, TranspositionEntry> _transpositionTable;
    private final Evaluator _evaluator;
    private final int[][] _principalVariation;
    private final int[] _pvLength;
    private int _ply;
    private int _nodecount;
    private long elapsedTime;

    public Engine(String fen, Evaluator ev) {
        logger.info("{}", fen);
        _board = FENParser.parseFENotation120(fen);
        _transpositionTable = new HashMap<>();
        _evaluator = ev;
        _principalVariation = new int[MAX_DEPTH][MAX_DEPTH];
        _pvLength = new int[MAX_DEPTH];
        clearPV();
        _nodecount = 0;
    }

    public int think(int depth) {
        long start = System.currentTimeMillis();
        clearPV();
        int best = 0;
        double bestEval = 0.;
        // private double negamax(int depth, double alpha, double beta, int color) {
        // -negamax(depth - 1, -INIT_BETA, -INIT_ALPHA, side ? -COLOR_WH : COLOR_WH);
        boolean side = _board.getSideToMove();
        for (int i = 1; i <= Math.min(MAX_DEPTH, depth); i++){
            bestEval = negamax(i, INIT_ALPHA, INIT_BETA, (side ? COLOR_WH : -COLOR_WH));
            best = _principalVariation[0][0];
            printPVLine(i);
        }
        elapsedTime = System.currentTimeMillis() - start;
        return best;
    }

    public int think() {
        clearPV();
        int best = 0;
        for (int i = 1; i <= 8; i++){
            best = iterativeDeepening(i);
            printPVLine(i);
        }
        return best;
    }

    private void clearPV() {
        for (int i = 0; i < MAX_DEPTH; i++) {
            _pvLength[i] = 0;
            for (int j = 0; j < MAX_DEPTH; j++) {
                _principalVariation[i][j] = 0;
            }
        }
    }

    private void initPVLength() {
        for (int i = 0; i < MAX_DEPTH; i++) {
            _pvLength[i] = 0;
        }
    }

    private void updatePV(int move) {
        _principalVariation[_ply][0] = move;
        for (int i = 0; i < _pvLength[_ply + 1]; i++) {
            _principalVariation[_ply][i + 1] = _principalVariation[_ply + 1][i];
        }
        _pvLength[_ply] = _pvLength[_ply + 1] + 1;
    }

    private int iterativeDeepening(int depth) {
        boolean side = _board.getSideToMove();
        List<Integer> pseudoLegal = MoveGenerator.generatePseudoLegal(_board);
        _ply = 0;

        initPVLength();

        if (depth == 1) {
            pseudoLegal = pseudoLegal.stream().filter(mv -> {
                _board.make(mv);
                boolean legal = !VectorAttack120.isKingInCheck(_board);
                _board.unmake(mv);
                return legal;
            }).collect(Collectors.toList()); // modifiable collection> intentional
        }

        final int pvMv = (_pvLength[0] > 0) ? _principalVariation[0][0] : -1;
        orderMoves(pseudoLegal, pvMv);
        int bestMove = pseudoLegal.isEmpty() ? 0 : pseudoLegal.get(0);
        double bestEval = Double.NEGATIVE_INFINITY;

        for (int move: pseudoLegal) {
            _board.make(move);
            _nodecount++;
            if (!VectorAttack120.isKingInCheck(_board)) {
               _ply = 1;
               double eval = -negamax(depth - 1, -INIT_BETA, -INIT_ALPHA, side ? -COLOR_WH : COLOR_WH);
               _ply--;
                if (eval > bestEval) {
                    bestMove = move;
                    bestEval = eval;
                    updatePV(bestMove);
                }
            }
            _board.unmake(move);
        }
        return bestMove;
    }


    private double quiesence(double alpha, double beta, int color, List<Integer> moves) {
        double static_eval = _evaluator.evaluate(_board);
        // stand pat
        double best = static_eval;
        if (best >= beta) return best * color;
        if (best > alpha) alpha = best;

        for (int i = 0; i < moves.size(); i++) {
            _board.make(moves.get(i));
            double current = 0.;
            if (!VectorAttack120.isKingInCheck(_board)) {
                current = -quiesence(-beta, -alpha, -color, moves);
            }
            _board.unmake(moves.get(i));
            if (current >= beta) return current * color;
            if (current > best) best = current;
            if (current > alpha) alpha = current;
        }
        return best;
    }

    private double negamax(int depth, double alpha, double beta, int color) {
        double alphaOrig = alpha;
        _pvLength[_ply] = 0;

        TranspositionEntry tEntry = _transpositionTable.get(_board.getZobristHash());
        if (tEntry != null && tEntry.depth >= depth) {
            double value = tEntry.eval;
            if (tEntry.nodeType == NODE_TYPE_EXACT) return value;
            else if (tEntry.nodeType == NODE_TYPE_LOWER && value >= beta ) {
                return value;
            } else if (tEntry.nodeType == NODE_TYPE_UPPER && value <= alpha ) {
                return value;
            }
        }

        if (depth == 0) return  color * _evaluator.evaluate(_board);
        List<Integer> child = MoveGenerator.generatePseudoLegal(_board);

        double mate = isCheckOrStale(depth, child);
        if (!Double.isNaN(mate)) {
            return mate;
        }

        final int pvMove = (_ply < MAX_DEPTH - 1 && _pvLength[0] > _ply) ? _principalVariation[_ply][0] : -1;

        orderMoves(child, pvMove);

        double bestEval = Double.NEGATIVE_INFINITY;

        for (Integer mv : child) {
            _board.make(mv);
            _nodecount++;
            double eval = Double.NEGATIVE_INFINITY;
            if (!VectorAttack120.isKingInCheck(_board)) {
                _ply++;
                eval = -negamax(depth - 1, -beta, -alpha, -color);
                _ply--;
                if (eval > bestEval) {
                    bestEval = eval;
                    updatePV(mv);
                }
            }
            _board.unmake(mv);
            alpha = Math.max(alpha, eval);
            if (alpha >= beta) break;
        }

        byte nodeType;

        if (bestEval <= alphaOrig) nodeType = NODE_TYPE_UPPER;
        else if (bestEval >= beta) nodeType = NODE_TYPE_LOWER;
        else nodeType = NODE_TYPE_EXACT;

        TranspositionEntry newEntry = new TranspositionEntry(bestEval, (byte)depth, nodeType);
        _transpositionTable.put(_board.getZobristHash(), newEntry);
        return bestEval;
    }

    private void orderMoves(List<Integer> unsorted, int pv) {
        unsorted.sort((lhs, rhs) -> {
            if (lhs == pv) return -1;
            if (rhs == pv) return 1;

            int f1 = Move.getFlag(lhs);
            int f2 = Move.getFlag(rhs);
            if (f1 != f2) return Integer.compare(f2, f1);

            int s1 = Move.getScore(lhs);
            int s2 = Move.getScore(rhs);
            return Integer.compare(s2, s1);
        });
    }

   public boolean isGameDrawn() {
        return isDrawBy50MoveRule() || drawByInsufficientMaterial() || isDrawByThreefold();
   }


    private double isCheckOrStale(int depth, List<Integer> pseudoLegal) {
        boolean foundLegal = false;
        Iterator<Integer> iterator = pseudoLegal.iterator();
        while (iterator.hasNext()) {
            int mv =  iterator.next();
            _board.make(mv);
            if (!VectorAttack120.isKingInCheck(_board)) {
                foundLegal = true;
                _board.unmake(mv);
                break;
            } else iterator.remove();
            _board.unmake(mv);
        }

        if (!foundLegal) {
            if (VectorAttack120.isKingInCheck(_board)) {
                return -(100_000 - depth); // score mates at higher nodes
            } else return DRAWN;
        }
        if (isGameDrawn()) return DRAWN;
        return Double.NaN;
    }

    private void printPVLine(int depth) {
        logger.info("PV (depth   {} +  ", depth);
        for (int i = 0; i < _pvLength[0] && i < depth; i++) {
            int m = _principalVariation[0][i];
            if (m != 0)  {
                logger.info("{} \t", Move.asString(m));
            }
        }
    }

    private List<Integer> getPV() {
        List<Integer> pv = new ArrayList<>();
        for (int i = 0; i < _pvLength[0]; i++) {
            pv.add(_principalVariation[0][i]);
        }
        return pv;
    }

    private boolean isDrawBy50MoveRule() {
        return _board.getHalfMoveClock() == DRAW_BY_50;
    }

    // TODO: test this code
    private boolean isDrawByThreefold() {
        long currentHash = _board.getZobristHash();
        int rep = 0;
        long[] hashes = _board.getHashHistory();
        int halfMoves = _board.getHalfMoveClock();
        int st = _board.getPly() - 2; // skip current
        for (int i = st; i >= st - halfMoves && i >= 0; i -= 2) {
            if (hashes[i] == currentHash) {
                rep++;
                if (rep >= 2) return true; // current + 2 previous = threefold
            }
        }
        return false;
    }

    private boolean drawByInsufficientMaterial() {
        int[] wList = _board.getWhitePieceList();
        int[] bList = _board.getBlackPieceList();

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
        // piece combinations that could lead to a draw
        // not interested in accurately evaluating these positions anyway
        boolean  onlyBlackKing = bPc[0] == 0 && bPc[1] == 0 && bPc[2] == 1;
        boolean  onlyWhiteKing = wPc[0] == 0 && wPc[1] == 0 && wPc[2] == 1;
        boolean  whiteKingKnight = (wPc[0] == 1 && wPc[1] == 0 && wPc[2] == 1);
        boolean  blackKingKnight = (bPc[0] == 1 && bPc[1] == 0 && bPc[2] == 1);
        boolean  whiteKingBishop = (wPc[0] == 0 && wPc[1] == 1 && wPc[2] == 1);
        boolean  blackKingBishop = (bPc[0] == 0 && bPc[1] == 1 && bPc[2] == 1);

        if (onlyWhiteKing &&  onlyBlackKing) return true;
        else if (whiteKingKnight &&  onlyBlackKing)  return true;
        else if (blackKingKnight &&  onlyWhiteKing)  return true;
        else if (whiteKingBishop &&  onlyBlackKing)  return true;
        else if (blackKingBishop &&  onlyWhiteKing)  return true;
        else if (whiteKingBishop && blackKingBishop) {
            // check if the bishops are of the same color => stalemate
            return Board120Utils.COLOR[bSquare] == Board120Utils.COLOR[wSquare];
        }
        return false;
    }


    public static void main(String[] args) {
        String fen = "r2q1rk1/pP1p2pp/Q4n2/bbp1p3/Np6/1B3NBn/pPPP1PPP/R3K2R b KQ - 0 1";
        String m_4 = "8/k2r4/p7/2b1Bp2/P3p3/qp4R1/4QP2/1K6 b - - 0 1";
        String m_4_f = "1k6/4qp2/QP4r1/p3P3/2B1bP2/P7/K2R4/8 w - - 0 1";
        String unkn = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1";
        Engine engine = new Engine(m_4, WeightedCombiEval.getInstance());
        int best = engine.think(9);
        logger.info("Best Move {}", Move.asString(best));
        logger.info("Node count {} in {}", engine._nodecount, engine.elapsedTime);
    }
}
