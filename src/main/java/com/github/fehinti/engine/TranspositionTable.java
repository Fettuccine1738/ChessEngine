package com.github.fehinti.engine;

import java.util.Arrays;

public class TranspositionTable {

    static final int UNKNOWN = 0;
    private static final int DEFAULT_CAPACITY = 4_194_304;
    private static final byte  NODE_TYPE_EXACT = 0;
    private static final byte  NODE_TYPE_LOWER = 1;
    private static final byte  NODE_TYPE_UPPER = 2;

    private long[] zobristKey  =  new long[DEFAULT_CAPACITY];
    private int[]  depth       =  new int[DEFAULT_CAPACITY];
    private byte[] nodeType    = new byte[DEFAULT_CAPACITY];
    private int[]  eval        = new int[DEFAULT_CAPACITY];
    private int size = 0;

    // builder() ??
    //private boolean alwaysReplace = true;
    //private boolean depthAware = false;
    //private boolean rootPrefered = false;

    public TranspositionTable() {
        Arrays.fill(zobristKey, UNKNOWN);
    }

    private TranspositionTable(int size) {
        //zobristKey = new long[size];
        //depth    = new int[size];
        //score    = new int[size];
        //eval    = new int[size];
    }

    public void put(long key, int depth, int bestEval, int alpha, int beta) {
        byte type;
        if (bestEval <= alpha) type = NODE_TYPE_UPPER;
        else if (bestEval >= beta) type = NODE_TYPE_LOWER;
        else type = NODE_TYPE_EXACT;

        int index = Math.toIntExact(key % DEFAULT_CAPACITY);
        this.zobristKey[index] = key;
        this.depth[index] = depth;
        this.eval[index] = bestEval;
        this.nodeType[index] = type;
    }

    public void cleanup() {
    }

    public int probe(long key, int depth, int alpha, int beta) {
        int index = Math.toIntExact(key % DEFAULT_CAPACITY);
        boolean exists = zobristKey[index] == key;
        if (exists) {
            int eval =  this.eval[index];
            byte nodeType = this.nodeType[index];
            if (nodeType == NODE_TYPE_EXACT) return eval;
            if (nodeType == NODE_TYPE_LOWER && eval >= beta) return eval;
            if (nodeType == NODE_TYPE_UPPER && eval <= alpha) return eval;
        }
        return UNKNOWN;
    }
}
