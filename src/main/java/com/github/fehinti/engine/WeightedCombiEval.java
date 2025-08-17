package com.github.fehinti.engine;

import com.github.fehinti.board.Board120;

public class WeightedCombiEval implements Evaluator {

    private final static WeightedCombiEval instance = new WeightedCombiEval();
    private static final PESTO pesto = PESTO.getInstance();
    private static final SimpleEvaluator simple = SimpleEvaluator.getInstance();

    public static WeightedCombiEval getInstance() {
        return instance;
    }

    @Override
    public double evaluate(Board120 board) {
        double pieceWise = pesto.evaluate(board);
        double posWise  = simple.evaluate(board);
        return pieceWise + posWise;
    }
}
