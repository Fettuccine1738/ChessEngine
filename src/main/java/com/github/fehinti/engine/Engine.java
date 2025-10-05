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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static com.github.fehinti.board.Board120Utils.*;

public class Engine {

    private enum TimeControl {
        BLITZ(1, 0),
        BLITZ_1_1(1, 1),
        BLITZ_2_1(2, 1),
        BULLET(3, 0),
        BULLET_3_2(3, 2),
        BULLET_5_0(3, 3),
        RAPID(10, 0),
        RAPID_15_10(15, 10),
        RAPID_30_0(30, 0),
        NONE(0, 0);

        byte minute;
        byte second;

        TimeControl(int minutes, int increment) {
            this.minute = (byte) minutes;
            this.second = (byte) increment;
        }
    }

    private class PVLine {
        int nMoves;
        int[] line;

        PVLine() {
            nMoves = 0;
            line = new int[MAX_DEPTH];
        }

        void updateLine(int move, PVLine other) {
            line[0] = move;
            System.arraycopy(other.line, 0, line, 1, other.nMoves);
            this.nMoves = other.nMoves + 1;
        }

        int copyFrom(PVLine other) {
            System.arraycopy(other.line, 0, line, 0, other.line.length);
            return line[0];
        }

        void clear() {
            this.nMoves = 0;
            Arrays.fill(line, 0);
        }
    }

    private static final Logger logger = LogManager.getClassLogger(Engine.class);
    private static final double INIT_ALPHA = Double.NEGATIVE_INFINITY;
    private static final double INIT_BETA  = Double.POSITIVE_INFINITY;
    private static final long   MAX_NODE_COUNT  = 10_000_000;
    private static final int TIME_CHECK_INTERVAL = 2047;
    private static final double TIMEOUT = Double.NaN;

    private static final int DRAW_BY_50 = 50;
    private static final int COLOR_WH = 1;
    private static final int MAX_DEPTH = 16;
    private static final int DRAWN = 0;
    private static final int MATED = 100_000;

    private final Board120 _board;
    private final TranspositionTable _transpositionTable;
    private final Evaluator _evaluator;
    private final int[][] _killerMoves;
    private int _ply;
    private int _nodecount;
    private long elapsedTime;
    private int bestMoveFound;
    private double bestEvalFound;
    private PVLine pvLine;


    /***
     * Drawing on the observation that terminating a depth halfway is wasteful, a large
     * enhancement to a basic time management scheme is to introduce two levels of time allocation.
     * Namely, one optimium time threshold (soft bound), and one maximum time threshold
     * (hard bound). During search, the optimum time threshold is checked on each iteration of iterative
     * deepening, while the maximum time threshold is checked periodically, usually by every set amount of nodes.
     * base / 20 + increment / 2
     */
    private TimeControl _timeControl = TimeControl.NONE;
    private boolean isTimeLimitReached = false;
    private long start;
    private long stop;

     // ! debug only
     static List<List<String>> dbugMoveOrder;

    public Engine(String fen, Evaluator ev) {
        logger.info("{}", fen);
        _board = FENParser.parseFENotation120(fen);
        _transpositionTable = new TranspositionTable();
        _evaluator = ev;
        _killerMoves = new int[MAX_DEPTH][2];
        _nodecount = 0;
        dbugMoveOrder = new ArrayList<>();
        pvLine = new PVLine();
        resetSearchHelpers();
    }

    public int think(long minutes) {
        this.stop = minutes * 60 * 1000;
        this.start = System.currentTimeMillis();
        resetSearchHelpers();
        PVLine tempLine = new PVLine();
        boolean side = _board.getSideToMove();
        int depth = 1;
        while (!isTimeLimitReached) {
            double bestEval = negamax(depth, INIT_ALPHA, INIT_BETA, (side ? COLOR_WH : -COLOR_WH), tempLine);
            logger.info("best eval {}", bestEval);
            if (isTimeLimitReached) break;
            this.bestEvalFound = Math.max(bestEvalFound, bestEval);
            this.bestMoveFound = pvLine.copyFrom(tempLine);
            printPVLine(depth++);
            checkTime();
        }
        elapsedTime = System.currentTimeMillis() - start;
        return bestMoveFound;
    }

    private boolean checkTime() {
        if (System.currentTimeMillis() - start > stop) {
            isTimeLimitReached = true;
            System.out.println("Time limit reached with " + _ply + " and searched. " + _nodecount);
        }
        return isTimeLimitReached;
    }

    private void resetSearchHelpers() {
        this.bestMoveFound = 0;
        this.bestEvalFound = 0;
        _ply = 0;
        _nodecount = 0;
        elapsedTime = 0;
        isTimeLimitReached = false;
        pvLine.clear();
    }

    private double quiescence(double alpha, double beta) {
        if (isTimeLimitReached) return alpha;
        if (checkTime()) {
            return TIMEOUT;
        }
        double best = _evaluator.evaluate(_board);
        if (best >= beta) return best;
        if (best > alpha) alpha = best;

        if (_ply >= MAX_DEPTH) {return best;}
        List<Integer> moves = MoveGenerator.generatePseudoCaptures(_board);
        for (int i = 0; i < moves.size(); i++) {
            if (checkTime()) {
                return TIMEOUT;
            }
            _board.make(moves.get(i));
            double current = 0.;
            if (!VectorAttack120.isKingInCheck(_board)) {
                _nodecount++;
                _ply++;
                current = -quiescence(-beta, -alpha);
                _ply--;
            }
            _board.unmake(moves.get(i));
            if (current >= beta) return current; // beta cutoff ??
            if (current >  best) best =  current;
            if (current > alpha) alpha = current;
        }
        return best;
    }

    private double negamax(int depth, double alpha, double beta, int color, PVLine pvLine) {
        if (isTimeLimitReached) return TIMEOUT;
        double alphaOrig = alpha;
        //_pvLength[_ply] = 0;

        int value = _transpositionTable.probe(_board.getZobristHash(), depth, (int) alpha, (int) beta);
        if (value != TranspositionTable.UNKNOWN) return value;
        if (depth == 0) {
            return  color * quiescence(alpha, beta);
        }

        List<Integer> child = MoveGenerator.generatePseudoLegal(_board);
        PVLine localPvLine = new PVLine();

        double mate = isCheckOrStale(depth, child);
        if (!Double.isNaN(mate)) {
            return mate;
        }
        if (checkTime()) {
            return TIMEOUT;
        }

        final int pvMove = (_ply < MAX_DEPTH - 1 && pvLine.nMoves > _ply) ?  pvLine.line[0] : -1;
        orderMoves(child, pvMove);

       double bestEval = Double.NEGATIVE_INFINITY;
       for (Integer mv : child) {
           _board.make(mv);
           double eval = Double.NEGATIVE_INFINITY;
           if (!VectorAttack120.isKingInCheck(_board)) {
               _nodecount++;
               _ply++;
               eval = -negamax(depth - 1, -beta, -alpha, -color, localPvLine);
               _ply--;
           }
           _board.unmake(mv);

           if (Double.isNaN(eval)) return TIMEOUT;
           if (eval > bestEval) {
               bestEval = eval;
               // ? updatePV(mv);

               // pvLine.line[0] = mv;
               // System.arraycopy(localPvLine.line, 0, pvLine.line, 1,
               //         localPvLine.nMoves);
               pvLine.updateLine(mv, localPvLine);
           }
           alpha = Math.max(alpha, eval);
           if (alpha >= beta && !isTimeLimitReached) {
               updateKillerMoves(mv);
               break;
           } else if (isTimeLimitReached) return TIMEOUT;
       }

       _transpositionTable.put(_board.getZobristHash(), depth, (int) bestEval, (int) alpha, (int) beta);
       return bestEval;
    }

    private void updateKillerMoves(int move) {
        if (Move.getFlag(move) != Move.QUIET) return;
        if (_killerMoves[_ply][0] != move) {
            _killerMoves[_ply][1] = _killerMoves[_ply][0];
            _killerMoves[_ply][0] = move;
        }
    }

    private void orderMoves(List<Integer> unsorted, int pv) {
        unsorted.sort((lhs, rhs) -> {
            if (lhs == pv) return -1;
            if (rhs == pv) return 1;

            boolean lKiller = isKillerMove(lhs);
            boolean rKiller = isKillerMove(rhs);
            if (lKiller && !rKiller) return -1;
            if (!lKiller && rKiller) return 1;

            int f1 = Move.getFlag(lhs);
            int f2 = Move.getFlag(rhs);
            if (f1 != f2) return Integer.compare(f2, f1);

            int s1 = Move.getScore(lhs);
            int s2 = Move.getScore(rhs);
            return Integer.compare(s2, s1);
        });
    }

    private boolean isKillerMove(int move) {
        return Move.compareMoves(_killerMoves[_ply][0], move)
                || Move.compareMoves(_killerMoves[_ply][1], move);
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
                return -(MATED - depth); // score mates at higher nodes
            } else return DRAWN;
        }
        if (isGameDrawn()) return DRAWN;
        return Double.NaN;
    }

    private void printPVLine(int depth) {
        logger.info("PV (depth   {} +  ", depth);
        for (int i = 0; i < pvLine.line.length; i++) {
            int m = pvLine.line[i];
            if (m == 0) break;
            String mvStr = Move.asString(m);
            logger.info("{} \t", mvStr);
        }
        logger.info("Node count {} in {}", _nodecount, elapsedTime);
    }

    private boolean isDrawBy50MoveRule() {
        return _board.getHalfMoveClock() == DRAW_BY_50;
    }

    // TODO: test this code, GPT
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
        int[] whitePieceCombi = new int[3];
        int[] blackPieceCombi = new int[3];
        int bSquare = 0;
        int wSquare = 0;

       for (int i = 0; i < 16; i++) {
            // return early, checkmate possible with (pawn, rook and queen) remaining
            if (whitePieceCombi[2] > 2 || blackPieceCombi[2] > 2) return false;

            int wp = (wList[i] >> 8) & 0xff;
            int bp = (bList[i] >> 8) & 0xff;
            if (wp == WKNIGHT) whitePieceCombi[0]++;
            else if (wp == WBISHOP) {
                whitePieceCombi[1]++;
                wSquare = wList[i] & 0xff;
            }
            else if (wList[i] != OFF_BOARD) whitePieceCombi[2]++;

            if (bp == -BKNIGHT) blackPieceCombi[0]++;
            else if (bp == -BBISHOP) {
                blackPieceCombi[1]++;
                bSquare = bList[i] & 0xff;
            }
            else if (bList[i] != OFF_BOARD) blackPieceCombi[2]++;
        }
        // piece combinations that could lead to a draw
        // not interested in accurately evaluating these positions anyway
        boolean  onlyBlackKing = blackPieceCombi[0] == 0 && blackPieceCombi[1] == 0 && blackPieceCombi[2] == 1;
        boolean  onlyWhiteKing = whitePieceCombi[0] == 0 && whitePieceCombi[1] == 0 && whitePieceCombi[2] == 1;
        boolean  whiteKingKnight = (whitePieceCombi[0] == 1 && whitePieceCombi[1] == 0 && whitePieceCombi[2] == 1);
        boolean  blackKingKnight = (blackPieceCombi[0] == 1 && blackPieceCombi[1] == 0 && blackPieceCombi[2] == 1);
        boolean  whiteKingBishop = (whitePieceCombi[0] == 0 && whitePieceCombi[1] == 1 && whitePieceCombi[2] == 1);
        boolean  blackKingBishop = (blackPieceCombi[0] == 0 && blackPieceCombi[1] == 1 && blackPieceCombi[2] == 1);

        if  (onlyWhiteKing &&  onlyBlackKing) return true;
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
        Engine engine = new Engine(m_4, SimpleEvaluator.getInstance());
        int best = engine.think(1);
        String bb = Move.asString(best);

        logger.info("Best Move {}", bb);
        System.out.println(engine._board.print8x8());

        System.out.println(FENParser.getFENotation(engine._board));
        Arrays.stream(engine.pvLine.line).filter(x -> x > 0)
                .forEach(mv -> System.out.println(Move.asString(mv)));
    }
}
