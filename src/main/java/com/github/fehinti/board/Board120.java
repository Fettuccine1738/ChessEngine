package com.github.fehinti.board;

import com.github.fehinti.engine.Evaluator;
import com.github.fehinti.engine.PESTO;
import com.github.fehinti.engine.SimpleEvaluator;
import com.github.fehinti.engine.WeightedCombiEval;
import com.github.fehinti.piece.Move;
import com.github.fehinti.piece.MoveGenerator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import static com.github.fehinti.board.Board120Utils.*;
import static com.github.fehinti.board.Board120Utils.BOARD_SIZE;
import static com.github.fehinti.piece.Move.*;

/**
 * The {@code Board} class is a Hybrid solution for representing a board in Chess
 * my representation uses a Square centric 8 by 8 enum array of
 * {@code Piece} enums that represents all piece types of both colors as well as
 * empty tiles. A Piece-centric disjoint list of all Piece types is used for efficient
 * move generation to avoid scanning the entire board, type and color of pieces are
 * associated by a certain index range or disjoint lists or arrays, index range can be
 * found in {@code BoardUtilities} class.
 * Additionally a static 10 x 12 and 8 x 8 static board is kept to make checking off board
 * moves in move generation easier.
 * @Author Favour F. Atilade.
 */
public final class Board120 {

    static final  int[] MAILBOX_120 = {
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
    -1,  0,  1,  2,  3,  4,  5,  6,  7, -1,
    -1,  8,  9, 10, 11, 12, 13, 14, 15, -1,
    -1, 16, 17, 18, 19, 20, 21, 22, 23, -1,
    -1, 24, 25, 26, 27, 28, 29, 30, 31, -1,
    -1, 32, 33, 34, 35, 36, 37, 38, 39, -1,
    -1, 40, 41, 42, 43, 44, 45, 46, 47, -1,
    -1, 48, 49, 50, 51, 52, 53, 54, 55, -1,
    -1, 56, 57, 58, 59, 60, 61, 62, 63, -1,
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
    };

    static final int[] MAILBOX_64 = {
    21, 22, 23, 24, 25, 26, 27, 28,
    31, 32, 33, 34, 35, 36, 37, 38,
    41, 42, 43, 44, 45, 46, 47, 48,
    51, 52, 53, 54, 55, 56, 57, 58,
    61, 62, 63, 64, 65, 66, 67, 68,
    71, 72, 73, 74, 75, 76, 77, 78,
    81, 82, 83, 84, 85, 86, 87, 88,
    91, 92, 93, 94, 95, 96, 97, 98
    };


    private static final byte BLACK = (byte) 0b10000000;
    private static final byte WHITE = (byte) 0b00000000;
    public static final byte  MOVED_FLAG = (byte) 0b1;
    public static final int KING_SQ = 15;
    static final int INIT_BUFFER = 512;

    // ! public int lastEntry = 0;
    private final byte[] _board120; // 8x8
    private boolean _sideToMove; // white or black's turn
    private int _fullMoveCounter; // full move counter begins at 1 incremented after black's turn
    private int _halfMoveClock; // ply : move of one side only
    private byte _castlingRights;
    private byte _enPassant;
    private long _zobristKey; // hashKey for a single position
    private final int[] _whitePieceList;
    private final int[] _blackPieceList;
    private final int[] _playHistory;
    private final long[] _hashHistory;
    private final int[] _irreversibleAspect;
    private int _ply;
    private final Stack<Integer> _captureEntry;


    /**
     * likely method of creating new boards or position during search where moves will be edges
     * connecting  boards as nodes
     * @param pieces    array of 64 enums representing pieces on the board or an empty tile
     * @param fmCounter full move counter updated after blacks turn
     * @param hmClock   ply, a move by a single side
     * @param cRights   castling rights, a single bytes that encodes castling rights for both colors
     * @param enPt      is there an enpassant on the board, only possible on rank 4 and 5
     * @throws          IllegalArgumentException if piece is null or those not have 64 elements
     */
    public Board120(byte[] pieces, boolean stm, int fmCounter, int hmClock, byte cRights, byte enPt) {
        if (pieces == null || pieces.length != 120)
            throw new IllegalArgumentException("pieces must have 64 elements");
        _board120 = new byte[pieces.length];
        System.arraycopy(pieces, 0, _board120, 0, pieces.length);
        _sideToMove = stm;
        _fullMoveCounter = fmCounter;
        _halfMoveClock = hmClock;
        _enPassant = enPt;
        setCastlingRights(cRights);
        _whitePieceList = new int[MAX_LEN_16];
        _blackPieceList = new int[MAX_LEN_16];
        fillLists();
        _captureEntry = new Stack<>();
        _playHistory    = new int[INIT_BUFFER];
        _hashHistory    = new long[INIT_BUFFER];
        _irreversibleAspect    = new int[INIT_BUFFER];
        _ply = 0;
        _zobristKey = ZobristHash.hashAtInit(this);
    }

    public Board120(Board120 copy) {
        this._board120 = new byte[copy._board120.length];
        this._whitePieceList = new int[copy._whitePieceList.length];
        this._blackPieceList = new int[copy._blackPieceList.length];
        this._hashHistory = new long[copy._hashHistory.length];
        System.arraycopy(copy._board120, 0, this._board120, 0, this._board120.length);
        System.arraycopy(copy._hashHistory, 0, this._hashHistory, 0, this._hashHistory.length);
        this._sideToMove = copy.getSideToMove();
        this._fullMoveCounter = copy.getFullMoveCounter();
        this._halfMoveClock = copy.getHalfMoveClock();
        this._enPassant = (byte) copy.getEnPassant();
        this.setCastlingRights(copy.getCastlingRights());
        fillLists();
        _captureEntry = new Stack<>();
        _playHistory    = new int[INIT_BUFFER];
        _irreversibleAspect    = new int[INIT_BUFFER];
        _ply = 0;
        this._zobristKey = copy._zobristKey;
    }

    public static int getMailbox64Number(int index) {
        return MAILBOX_64[index];
    }

    public static int getMailbox120Number(int index) {
        return MAILBOX_120[index];
    }

    private void fillLists() {
        Arrays.fill(_whitePieceList, OFF_BOARD);
        Arrays.fill(_blackPieceList, OFF_BOARD);

        int wp= 0;
        int bp= 0;
        int wk = 15;

        for (int sq = 0; sq < BOARD_SIZE; sq++) {
            int square = getMailbox64Number(sq);
            int value = _board120[square];
            if (value > 0) {
                if (value == WKING) _whitePieceList[wk] = ((value << RANK_8) | square);
                else _whitePieceList[wp++] = ((value << RANK_8) | square);
            } else if (value < 0) {
                // * negate value used because signed bit is still maintained if actual value
                // * -num removes the signed bits allows us to use the unused bits
                if (value == BKING) _blackPieceList[wk] = ((-value << RANK_8) | square);
                else _blackPieceList[bp++] = ((-value << RANK_8) | square);
            }
        }
        // sort list will be valuable when looking for smallest attackers
        // pieces are sorted with minor pieces first (ordering: P, N, B, R, Q , K)
        Arrays.sort(_whitePieceList, 0, KING_SQ);
        // sort descending, encoding of pawns > major pieces , this keeps pawns before other pieces
        // allowing us to get smallest attacker for black easily
        sortDescending(_blackPieceList);
    }

    private void sortDescending(int[] arr) {
        Integer[] copy = new Integer[KING_SQ];
        for (int i = 0; i < KING_SQ; i++) {
            copy[i] = arr[i];
        }
        Arrays.sort(copy, Collections.reverseOrder());
        for (int i = 0; i < KING_SQ; i++) {
           arr[i] = copy[i];
        }
    }

    /**
     * @param rights byte encoding of queen side and king side castle rights of black and white
     */
    public void setCastlingRights(byte rights) {
        boolean wk = (rights & WHITE_KINGSIDE) != 0;
        boolean wq = (rights & WHITE_QUEENSIDE) != 0;
        boolean bk = (rights & BLACK_KINGSIDE) != 0;
        boolean bq = (rights & BLACK_QUEENSIDE) != 0;
        this._castlingRights = encodeCastlingRights(wk, wq, bk, bq);
    }

    /**
     * @return single byte encoding of black and white castling rights
     */
    public byte getCastlingRights() {
        return _castlingRights;
    }

    public Stack<Integer> getCaptureEntry()
    {
        return _captureEntry;
    }

    /**
     * a single byte encodes both castling rights (king side and queenside) for black and white
     * @param wk true if white can castle king side     0001
     * @param wq true if white can castle queen side    0010
     * @param bk true if black can castle king side     0100
     * @param bq true if black can castle queen side    1000
     * @return single byte encoding by OR all binary constants
     */
    public static byte encodeCastlingRights(boolean wk, boolean wq, boolean bk, boolean bq) {
        byte rights = 0;
        if (wk) rights |= WHITE_KINGSIDE;
        if (wq) rights |= WHITE_QUEENSIDE;
        if (bk) rights |= BLACK_KINGSIDE;
        if (bq) rights |= BLACK_QUEENSIDE;
        return rights;
    }

    public boolean canSideCastle(boolean b) {
        byte castles = _castlingRights;
        if (b) { // masks blacks bits if white to play
            castles &= ~(BLACK_QUEENSIDE | BLACK_KINGSIDE);
        } else            castles &= ~(WHITE_KINGSIDE | WHITE_QUEENSIDE); // vice versa
        return castles != 0;
    }

    // is king side castling right for white available
    public boolean canWhiteCastleKingside() {
        return (_castlingRights & WHITE_KINGSIDE) != 0;
    }

    // is queenside castling right for white available
    public  boolean canWhiteCastleQueenside() {
        return (_castlingRights & WHITE_QUEENSIDE) != 0;
    }

    // is king side castling right for black available
    public boolean canBlackCastleKingside() {
        return (_castlingRights & BLACK_KINGSIDE) != 0;
    }

    public  boolean canBlackCastleQueenside() {
        return (_castlingRights & BLACK_QUEENSIDE) != 0;
    }

    public static boolean isWhitePiece(byte piece) {
        return (piece & BLACK) == 0;
    }

    public static boolean isBlackPiece(byte piece) {
        return (piece & BLACK ) != 0;
    }

    public boolean isPieceWhite(byte piece) {
        return piece > EMPT_SQ;
    }

    public boolean getSideToMove() {
        return _sideToMove;
    }

    public int[] getWhitePieceList() {
        int[] copy = new int[MAX_LEN_16];
        for (int i = 0; i < MAX_LEN_16; i++) {
            copy[i] = _whitePieceList[i];
        }
        return copy;
    }

    public int[] getBlackPieceList() {
        int[] copy = new int[MAX_LEN_16];
        for (int i = 0; i < MAX_LEN_16; i++) {
            copy[i] = _blackPieceList[i];
        }
        return copy;
    }

    public byte getPieceOnSquare(int index) {
        if (index < 0 || index > 119) throw new IllegalArgumentException("index out of bounds.");
        return _board120[index];
    }

    public int getBlackKingSq() {
        return _blackPieceList[KING_SQ] & 0xff;
    }

    public int getWhiteKingSq() {
        return _whitePieceList[KING_SQ] & 0xff;
    }

    public int getEnPassant() {
        return _enPassant;
    }

    /**
     * @return a decimal number of half moves with respect to the 50 move
     * draw rule. It is reset to zero after a capture or a pawn move and incremented otherwise.
     */
    public int getHalfMoveClock() {
        return _halfMoveClock;
    }

    /**
     * @return The number of the full moves in a game. It starts at 1, and is incremented
     * after each Black's move.
     */
    public int getFullMoveCounter() {
        return _fullMoveCounter;
    }

    private int getPieceListIndex(int piece, int square, boolean captured) {
        // if this method is called when capture occurs, find the index of the captured piece in its
        // own piece list, else find a piece in our own list (e.g find rook when castling)
        int[] piecelist = (captured) ?  ((!_sideToMove) ? _whitePieceList : _blackPieceList) :
                (_sideToMove) ? _whitePieceList : _blackPieceList;
        for (int index = 0; index < piecelist.length; index++) {
            int pie = (piecelist[index] >> 8) & 0xff; // piece value
            int pos = piecelist[index] & 0xff; // square
            if (pie == piece && pos == square) return index;
        }
        return OFF_BOARD;
    }

    public void make(int move) {
        int flag = getFlag(move);
        int to = getTargetSquare(move);
        int from = getFromSquare(move);
        int promotion = getPromotion(move);
        int index = getIndex(move);
        byte piece = _board120[from];
        assert(piece != EMPT_SQ);
        assert(isWhitePiece(piece) == _sideToMove);

        addMoveToHistory(move);
        addIrreversibleAspect();

        if ((piece == WKING || piece == BKING || piece == WROOK || piece == BROOK)
                && flag != CASTLE) onRookMove(from,  piece, flag);

        int[] side = (_sideToMove) ? _whitePieceList : _blackPieceList;
        int[] xside = (_sideToMove) ? _blackPieceList : _whitePieceList;

        int xindex = OFF_BOARD;
        // every capture will be on the to square except enpassant where piece-to-be-captured
        // will be below enpassant (black) : above enpassant (white)
        int captured = (flag != EN_PASSANT) ?  Math.abs(_board120[to]) :
                        (_sideToMove) ? Math.abs(_board120[_enPassant - 10]) :
                        Math.abs(_board120[_enPassant + 10]);
        int val = Math.abs(piece);
        if (captured != 0) { // capture on the board
            if (flag == EN_PASSANT) {
                assert((_sideToMove) ? _board120[to] == BPAWN : _board120[to] == WPAWN);
                int epSq = (_sideToMove) ? _enPassant - 10 : _enPassant + 10;
                xindex = getPieceListIndex( captured, epSq , true);
            } else xindex = getPieceListIndex(captured, to, true);
            assert(xindex != OFF_BOARD);
            _captureEntry.push(captured << RANK_8 | xindex); // store index of captured piece
        }

        switch (flag) {
            case QUIET, DOUBLE_PAWN_PUSH -> {
                assert(_board120[to] == EMPT_SQ);
                makeMove(from, to, piece);
                _halfMoveClock++;
                boolean found = incrementalUpdate(side, index, (val << RANK_8 | to), (val << RANK_8 | from));
                if (!found) throw new RuntimeException("Error f=quiet&dpPush");
                if (flag == DOUBLE_PAWN_PUSH) {
                    if (_sideToMove) _enPassant = (byte) (to - 10);
                    else _enPassant = (byte) (to + 10);
                }
            }
            case EN_PASSANT -> {
                assert(_board120[to] == EMPT_SQ);
                assert(to == _enPassant);
                if (xindex == OFF_BOARD) throw new IllegalArgumentException("captured piece index not found + \n" +
                        print8x8() +"\n" + FENParser.getFENotation(this) +"\n" + getBoardData()
                        + "\n" + Move.dbgMove(move) + "\n" + printMailbox() + " " + captured +
                        Arrays.toString((_sideToMove) ? _blackPieceList : _whitePieceList));
                makeMove(from, to, piece);
                int xpos = OFF_BOARD;
                if (_sideToMove) {
                    xpos = to - 10; // black piece to capture is a square below enpassnt
                    _zobristKey ^= ZobristHash.zobristKey(getMailbox120Number(to - 10), BPAWN);
                    assert(_board120[xpos] == BPAWN);
                }
                else {
                    xpos = to + 10; // white piece to capture is a square above enpassant
                    _zobristKey ^= ZobristHash.zobristKey(getMailbox120Number(to + 10), WPAWN);
                    assert(_board120[xpos] == WPAWN);
                }
                _board120[xpos] = EMPT_SQ;
                _halfMoveClock = EMPT_SQ;
                boolean found1 = incrementalUpdate(side, index, (val << RANK_8 | to), (val << RANK_8 | from));
                boolean found2 = incrementalUpdate(xside, xindex, OFF_BOARD, (captured << RANK_8 | xpos));
                if (!found1) throw new RuntimeException("Error f=ep, side" + _sideToMove);
                if (!found2) throw new RuntimeException("Error f=ep, xside  " + _sideToMove);
            }
            case CAPTURE -> {
                assert(_board120[to] != EMPT_SQ);
                if (captured == WROOK || captured == Math.abs(BROOK)) onCaptureRook(to);
                if (xindex == OFF_BOARD) throw new IllegalArgumentException("captured piece index not found + \n" +
                        print8x8() +"\n" + FENParser.getFENotation(this) +"\n" + getBoardData()
                        + "\n" + Move.dbgMove(move));
                _zobristKey ^= ZobristHash.zobristKey(getMailbox120Number(from), piece); // XOR out capturER
                _zobristKey ^= ZobristHash.zobristKey(getMailbox120Number(to),   _board120[to]); // XOR out captured
                _zobristKey ^= ZobristHash.zobristKey(getMailbox120Number(to),   piece); // XOR in
                _board120[from] = EMPT_SQ;
                _board120[to]   = piece;
                _halfMoveClock = EMPT_SQ;
                boolean found1 = incrementalUpdate(side, index, (val << RANK_8 | to), (val << RANK_8 | from));
                boolean found2 = incrementalUpdate(xside, xindex, OFF_BOARD, (captured << RANK_8 | to));
                if (!found1) throw new RuntimeException("Error f=cap, side");
                if (!found2) throw new RuntimeException("Error f=cap, xside");
            }
            case PROMOTION, PROMOTION_CAPTURE -> {
                byte pp = getPromotionPiece(promotion);
                _zobristKey ^= ZobristHash.zobristKey(getMailbox120Number(from), piece); // xor out pawn
                _zobristKey ^= ZobristHash.zobristKey(getMailbox120Number(to), pp);
                _board120[from] = EMPT_SQ;
                _board120[to] = pp;
                boolean found2 = incrementalUpdate(side, index,
                        (Math.abs(pp) << RANK_8 | to), (Math.abs(piece) << RANK_8 | from));
                if (!found2) throw new RuntimeException("Error f=promo, freeslot");
                if (flag == PROMOTION_CAPTURE) {
                    _zobristKey ^= ZobristHash.zobristKey(getMailbox120Number(to), _board120[to]);
                    if (xindex == OFF_BOARD) throw new IllegalArgumentException("captured piece index not found + \n" +
                            print8x8() +"\n" + FENParser.getFENotation(this) +"\n" + getBoardData()
                            + "\n" + Move.dbgMove(move)
                            + "\n" + from
                            + "\n" + to
                            + "\n" + piece
                           );
                    if (captured == WROOK || captured == Math.abs(BROOK)) onCaptureRook(to);
                    boolean found3 = incrementalUpdate(xside, xindex, OFF_BOARD, (captured << RANK_8 | to)); // remove piece
                    if (!found3) throw new RuntimeException("Error f=cap&Promo, xside");
                }
            }
            case CASTLE ->  makeCastle(from, to, piece);
            default -> throw new IllegalStateException("Unexpected value: " + flag);
        }
        // _enPassant no longer valid after every (non-double pawn push)move
        if (flag != DOUBLE_PAWN_PUSH) _enPassant =  OFF_BOARD;
        if (!isPieceWhite(piece)) _fullMoveCounter++;
        _sideToMove = !_sideToMove;
    }

    public long getZobristHash() {
        return _zobristKey;
    }

    /**
     * @param move 32 bit integer encoding of from square, target square, flags and captured piece
     */
    public void unmake(int move) {
        // change to opponent of side that played
        _sideToMove = !_sideToMove;
        int flag = getFlag(move);
        int from = getFromSquare(move);
        int to   = getTargetSquare(move);
        int promo = getPromotion(move);
        int index = getIndex(move);
        byte piece = _board120[to]; // piece has moved to target square
        byte capturedPiece = 0;
        assert(_ply != EMPT_SQ);
        assert(_board120[to] != EMPT_SQ);
        assert(_playHistory[_ply - 1] == move);
        unaddIrreversibleAspect();

        int xindex = OFF_BOARD;
        if (flag == CAPTURE || flag == PROMOTION_CAPTURE || flag == EN_PASSANT) {
            int entry = _captureEntry.pop();
            capturedPiece = (byte) ((entry >> RANK_8) & 0xff);
            xindex = entry & 0xff;
            if (_sideToMove) capturedPiece = (byte) -capturedPiece; // preserves sign bit for black pieces
        }
        // update side THAT moveD
        int v = Math.abs(piece);
        int[] side =  (_sideToMove)  ? _whitePieceList : _blackPieceList;
        int[] xside = (_sideToMove)  ? _blackPieceList : _whitePieceList;

        switch (flag) {
            case QUIET, DOUBLE_PAWN_PUSH -> {
                assert(_board120[from] == EMPT_SQ);
                boolean f = incrementalUpdate(side, index, encode(v, from), encode(v, to));
                if (!f) {
                    throw new RuntimeException("Error unmaking f=quiet&dppush\n" + getBoardData()
                             +"\n" + print8x8() +"\n" + Arrays.toString((_sideToMove) ? _whitePieceList : _blackPieceList)
                       + "\n" + Move.dbgMove(move) + "\nold" + (encode(v, from)) +"\nnew" + (encode(v, to))
                       +"\n" + (_sideToMove ? _whitePieceList[index] : _blackPieceList[index]) +
                            "\nind" + index +
                            "\npro" + promo +
                            "\nto" + to);
                }
                makeMove(to, from, piece);
            }
            case EN_PASSANT -> {
                makeMove(to, from, piece); //reverse capturing pawn to its previous square
                assert(capturedPiece == WPAWN || capturedPiece == BPAWN);// captured piece is a square above enpassant
                if (isPieceWhite(capturedPiece))  {
                    _board120[_enPassant + 10] = capturedPiece;
                    _zobristKey ^= ZobristHash.zobristKey(getMailbox120Number(_enPassant + 10), capturedPiece);
                }
                else {
                    _board120[_enPassant - 10] = capturedPiece;
                    _zobristKey ^= ZobristHash.zobristKey(getMailbox120Number(_enPassant - 10), capturedPiece);
                }
                boolean f1 = incrementalUpdate(side, index, encode(v, from), encode(v, to));
                boolean f2 = incrementalUpdate(xside, xindex,
                        encode(Math.abs(capturedPiece), (_sideToMove) ? _enPassant - 10 : _enPassant + 10), OFF_BOARD);
                if (!f1) throw new RuntimeException("Error updating ep capturing piece");
                if (!f2) throw new RuntimeException("Error updating eP captured piece");
            }
            case CAPTURE -> {
                assert(_board120[from] == EMPT_SQ);
                makeMove(to, from, piece); // return capturing piece
                assert(capturedPiece != EMPT_SQ);
                _board120[to] = capturedPiece; // returned captured piece
                _zobristKey ^= ZobristHash.zobristKey(getMailbox120Number(to), capturedPiece);
                boolean f1 = incrementalUpdate(side, index, encode(v, from), encode(v, to));
                boolean f2 = incrementalUpdate(xside, xindex,  encode(Math.abs(capturedPiece), to), OFF_BOARD);
                if (!f1) throw new RuntimeException("Error updating capturing pc");
                if (!f2) throw new RuntimeException("Error updating captured pc");
            }
            case PROMOTION, PROMOTION_CAPTURE -> {
                assert(_board120[from] == EMPT_SQ);
                if (_sideToMove) makeMove(to, from, WPAWN);
                else makeMove(to, from, BPAWN);
                int enc = (_sideToMove) ? WPAWN : -BPAWN;
                boolean found = incrementalUpdate(side, index, encode(enc, from), encode(v, to));
                if (!found) throw new RuntimeException("Error restoring promoting pawn f=Promotion");
                if (flag == PROMOTION_CAPTURE) {
                    _board120[to] = capturedPiece;
                    _zobristKey ^= ZobristHash.zobristKey(getMailbox120Number(to), capturedPiece);
                    // this has encoding would have been set to -1 in the make's incremental update
                    boolean fd = incrementalUpdate(xside, xindex, (Math.abs(capturedPiece) << RANK_8 | to ), OFF_BOARD);
                    if (!fd) throw new RuntimeException("Error rest;oring prev captured f=Promotion");
                }
            }
            case CASTLE -> unmakeCastle(from, to, side, index);
            default -> throw new IllegalArgumentException();
        }
        if (!isPieceWhite(piece)) _fullMoveCounter--;
    }

    private void unmakeCastle(int from, int to, int[] side, int index) {
        boolean fRook;
        int rv = (_sideToMove) ? WROOK : -BROOK;
        int ri = OFF_BOARD;
        if (_sideToMove) {
            _board120[E1] = WKING;
            if (to == G1) { // short castles
                // 4 zobrist updates
                _board120[G1] = EMPT_SQ;
                _board120[F1] = EMPT_SQ; // undo rook's move
                _board120[H1] = WROOK;
            } else if (to == C1) { // long castle
                _board120[C1] = EMPT_SQ; // undo king's move
                _board120[D1] = EMPT_SQ; // undo rooks's move
                _board120[A1] = WROOK;
            }
            _zobristKey ^= ZobristHash.zobristKey(getMailbox120Number(to), WKING);
            _zobristKey ^= ZobristHash.zobristKey(getMailbox120Number(from), WKING);
            _zobristKey ^= ZobristHash.zobristKey(getMailbox120Number((to) == C1 ? D1 : F1), WROOK);
            _zobristKey ^= ZobristHash.zobristKey(getMailbox120Number((to) == C1 ? A1 : H1), WROOK);
            ri = getPieceListIndex(rv, (to == C1) ? D1 : F1, false);
            fRook = incrementalUpdate(side, ri, encode(rv, (to == C1) ? A1 : H1),
                    encode(rv, (to == C1) ? D1 : F1));
        } else {
            _board120[E8] = BKING;
            if (to == G8) { // short castles
                _board120[G8] = EMPT_SQ; // undo king's move
                _board120[F8] = EMPT_SQ; // undo rook's move
                _board120[H8] = BROOK;
            } else if (to == C8) { // long castle
                _board120[C8] = EMPT_SQ; // undo rook's move
                _board120[D8] = EMPT_SQ; // undo rook's move
                _board120[A8] = BROOK; // put rook back on A_8
            }
            ri = getPieceListIndex(rv, (to == C8) ? D8 : F8, false);
            fRook = incrementalUpdate(side, ri, encode(rv, (to == C8) ? A8 : H8),
                    encode(rv, (to == C8) ? D8 : F8));

            _zobristKey ^= ZobristHash.zobristKey(getMailbox120Number(to), BKING);
            _zobristKey ^= ZobristHash.zobristKey(getMailbox120Number(from), BKING);
            _zobristKey ^= ZobristHash.zobristKey(getMailbox120Number((to) == C8 ? D8 : F8), BROOK);
            _zobristKey ^= ZobristHash.zobristKey(getMailbox120Number((to) == C8 ? A8 : H8), BROOK);
        }
        if (!fRook) throw new RuntimeException("Error updating Rook f=castle");
        int enc = (_sideToMove) ? WKING : -BKING;
        assert(index == 0);
        boolean f1 = incrementalUpdate(side, index, encode(enc, from), encode(enc, to));
        if (!f1) throw new RuntimeException("Error updating kingside");
    }

    private int encode(int pc, int info) {
        return (pc << RANK_8) | info;
    }

    private void makeCastle(int from, int to, byte p){
        int rookFr = 0;
        int rookTo = 0;
        if (!canSideCastle(_sideToMove)) return; // we cannot castle
        if (_sideToMove) {
            assert(_board120[from] == WKING);
            if (to == C1) {
                rookFr = A1;  // queenside castle
                rookTo = D1;
            } else if (to == G1) {
                rookFr = H1; // kingside castles
                rookTo = F1;
            } else throw new IllegalArgumentException("invalid castle");
            _castlingRights &= ~(WHITE_QUEENSIDE | WHITE_KINGSIDE); // remove kside and qside castling
        } else {
            assert(_board120[from] == BKING);
            if (to == C8)  { // queenside
                rookFr = A8;
                rookTo = D8;
            } else if (to == G8) { // kingside
                rookFr = H8;
                rookTo = F8;
            } else throw new IllegalArgumentException("invalid castle");
            _castlingRights &= ~(BLACK_KINGSIDE | BLACK_QUEENSIDE);
        }
        // TODO mask out the moved flag
        int[] side = (_sideToMove) ? _whitePieceList : _blackPieceList;
        byte val = (_sideToMove) ? WROOK : BROOK;
        makeMove(from, to, p); // for king
        makeMove(rookFr, rookTo, val); // for rook
        boolean kEntry = incrementalUpdate(side, 15, (Math.abs(p) << RANK_8 | to),
                ((Math.abs(p) << RANK_8) | from)); // king is always on index 15
        if (!kEntry) throw new RuntimeException("Error updating kingside");
        assert(_board120[rookFr] == WROOK || _board120[rookFr] == BROOK);
        boolean rEntry = incrementalUpdate(side,
                getPieceListIndex(Math.abs(val), rookFr, false),
                (Math.abs(val) << RANK_8 | rookTo),
                (Math.abs(val) << RANK_8 | rookFr));
        if (!rEntry) throw new RuntimeException("Error updating rookside");
    }

    private void makeMove(int from, int to, byte p) {
        _board120[from] = EMPT_SQ;
        _board120[to] = p;
        _zobristKey ^= ZobristHash.zobristKey(getMailbox120Number(from), p);
        _zobristKey ^= ZobristHash.zobristKey(getMailbox120Number(to), p);
    }

    public int getPly() {return _ply;}

    private boolean incrementalUpdate(int[] side, int index, int encode, int validate) {
        // if entry is 'off boarded' (captured) do not bother to check if encoding matches previous state
        boolean found = validate == OFF_BOARD && side[index] == OFF_BOARD;
        // compares piece and square encoding match our validate encoding before overwriting
        if ((side[index] & 0xff) == (validate & 0xff) && (side[index] >> RANK_8) == validate >> RANK_8) {
            found = true;
            //! lastEntry = encode;
        }
        side[index] = encode;
        return  found;
    }

    // remove castling rights when rooks move;
    private void onRookMove(int from, byte piece, int flag) {
        boolean side = canSideCastle(_sideToMove);
        // there are no castling rights to update, castles updated separately
        if (!side || flag == CASTLE) return;
        assert(piece == WROOK || piece == WKING ||piece == BKING ||piece == BROOK);
        if (_sideToMove) { // white
            if (piece == WKING) {
                _castlingRights &= ~(WHITE_KINGSIDE | WHITE_QUEENSIDE);
                // whitePieceList[15] &= ~(MOVED_FLAG << 16); // remove moved bit,
            }
            else {
                if (canWhiteCastleQueenside() && from == A1) _castlingRights &= ~WHITE_QUEENSIDE;
                if (canWhiteCastleKingside() && from == H1) _castlingRights &= ~WHITE_KINGSIDE;
            }
        }
        else {
            if (piece == BKING) {
                _castlingRights &= ~(BLACK_KINGSIDE | BLACK_QUEENSIDE);
                // _blackPieceList[15] &= ~(MOVED_FLAG << 16);
            }
            else {
                if (canBlackCastleQueenside() && from == A8) _castlingRights &= ~BLACK_QUEENSIDE;
                if (canBlackCastleKingside() && from == H8) _castlingRights &= ~BLACK_KINGSIDE;
            }
        }
    }

    // removes opponents castlign rights  when rooks are captured
    private void onCaptureRook(int square) {
        if (!canSideCastle(!_sideToMove)) return; // opponent has no castles
        if (_sideToMove) {
            if (square == A8) _castlingRights &= ~BLACK_QUEENSIDE;
            else if (square == H8) _castlingRights &= ~BLACK_KINGSIDE;
        } else {
            if (square == A1) _castlingRights &= ~WHITE_QUEENSIDE;
            else if (square == H1) _castlingRights &= ~WHITE_KINGSIDE;
        }
    }

    private void addMoveToHistory(int move) {
        _playHistory[_ply] = move;
    }

    public int getMoveFromHistory() {
        return _playHistory[_ply];
    }

    public int[] getPlayHistory() {
        int[] copy = new int[_playHistory.length];
        for (int i : _playHistory) {
            copy[i] = _playHistory[i];
        }
        return copy;
    }

    public long[] getHashHistory() {
       // int[] copy = new int[_playHistory.length];
       // for (int i : _playHistory) {
       //     copy[i] = _playHistory[i];
       // }
       // return copy;
        return _hashHistory;
    }

    private void addIrreversibleAspect() { int ep = (_enPassant & 0xff);   // Mask to 6 bits
        int cR = (_castlingRights & 0xF) << 8; // Shift and mask to 4 bits
        int hM = (_halfMoveClock & 0x3F) << 16; // Shift and mask to 6 bits
        _hashHistory[_ply] = this._zobristKey;
        _irreversibleAspect[_ply++] = (ep | cR | hM);
    }

    private void unaddIrreversibleAspect() {
        int irreversible = _irreversibleAspect[--_ply];
        int ep = (irreversible & 0xff);
        ep = (ep == 63) ?  OFF_BOARD : ep; // don't remember why 63 is here
        int cR = (irreversible >> 8) & 0x3f;
        int hM = (irreversible >> 16) & 0x3f;
        _enPassant  = (byte) ep;
        _castlingRights = ((byte) cR);
        _halfMoveClock = hM;
    }

    public byte getPromotionPiece(int flag) {
        byte piece = 0;
        switch (flag) {
            case 0: piece = WKNIGHT; break;
            case 1: piece = WBISHOP; break;
            case 2: piece = WROOK; break;
            case 3: piece = WQUEEN; break;
            default:
        }
        return (_sideToMove) ? piece : (byte) (BLACK | piece);
    }


    public static char mapByteToChar(byte b) {
        switch (b) {
            case 0 -> { return '.'; }
            case 1 -> { return 'P'; }
            case 2 -> { return 'N'; }
            case 3 -> { return 'B'; }
            case 4 -> { return 'R'; }
            case 5 -> { return 'Q'; }
            case 6 -> { return 'K'; }
            case -127 -> { return 'p'; }
            case -126 -> { return 'n'; }
            case -125 -> { return 'b'; }
            case -124 -> { return 'r'; }
            case -123 -> { return 'q'; }
            case -122 -> { return 'k'; }
        }
        return 'X';
    }

    public static byte mapCharToByte(char ch) {
        switch (ch) {
            case '.' -> { return 0; }
            case 'P' -> { return 1; }
            case 'N' -> { return 2; }
            case 'B' -> { return 3; }
            case 'R' -> { return 4; }
            case 'Q' -> { return 5; }
            case 'K' -> { return 6; }
            case 'p' -> { return -127; }
            case 'n' -> { return -126; }
            case 'b' -> { return -125; }
            case 'r' -> { return -124; }
            case 'q' -> { return -123; }
            case 'k' -> { return -122; }
        }
        return OFF_BOARD;
    }

    public void print() {
        for (int i = 11; i >= 0; i--) {
            for(int j = 0; j < 10; j++) {
                System.out.print(mapByteToChar(_board120[i * 10 + j]) + " ");
            }
            System.out.println();
        }
    }

    public String printMailbox() {
        StringBuilder sb = new StringBuilder();
        for (int i = 11; i >= 0; i--) {
            for(int j = 0; j < 10; j++) {
                sb.append(mapByteToChar(_board120[i * 10 + j]) + " ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    // returns string represntation of en passant square
    public String getEnpassantString() {
        byte sq = (_enPassant == OFF_BOARD) ? _enPassant : (byte) getMailbox120Number(_enPassant);
        if (sq == OFF_BOARD) return "-";
        if (sq < EMPT_SQ || sq >= BOARD_SIZE) throw new IllegalArgumentException();
        StringBuilder sb = new StringBuilder(2);
        int rank = sq >> 3;
        int file = sq & 7;
        sb.append((char) (file + 'a')).append(rank + 1);
        return sb.toString();
    }

    public String getBoardData() {
        StringBuilder info = new StringBuilder();
        info.append("\nEnPassant:\t").append(getEnpassantString()).append("\n");
        info.append("Castles: ").append(FENParser.getCastlingRightsFENotation(this));
        info.append("\n").append("fullCount = ").append(_fullMoveCounter);
        info.append("\n").append("halfMove = ").append(_halfMoveClock);
        info.append("\nSide:\t").append((_sideToMove) ? "white" : "black").append("\n");
        info.append("Zobristhash: ").append(_zobristKey).append("\n");
        return info.toString();
    }

    /**
     * @return combination of board representationg and board info
     */
    public String print8x8() {
        StringBuilder board = new StringBuilder();
        int rank; int file;
        String newline = "+---";
        board.append('\t');
        for (int i = 0; i < 8; i++) board.append(newline);
        board.append("+\n");

        for (rank = RANK_8; rank > EMPT_SQ; rank--) {
            board.append(rank).append('\t').append('|');
            for (file = FILE_A; file < FILE_H; file++) {
                byte piece = _board120[(byte) Board120.getMailbox64Number((rank - 1) * FILE_H + file)];
                char c = mapByteToChar(piece);
                if (c == 'k' || c == 'K') board.append('[').append(c).append(']').append('|');
                else if (c == '.') {
                    if (getMailbox64Number(((rank - 1) * RANK_8 + file)) == _enPassant) {
                        board.append(' ').append("o ").append('|');
                    } else board.append(' ').append(' ').append(' ').append('|');
                } else board.append(' ').append(c).append(' ').append('|');
            }
            board.append("\n\t");
            for (int c = FILE_A; c < FILE_H; c++)   {
                board.append(newline);
                if (c == FILE_H - 1) board.append('+');
            }
            board.append("\n");
        }
        board.append('\t');
        for (file = 0; file < FILE_H; file++)  board.append("  ")
                .append((char) ('a' + file)).append(" ");
        return board.toString();
    }

    // use this to determine if we are in the endgame, middlegame, start
    public int getTotalPcCount() {
        int count = 0;
        for (int i = 0; i < 16; i++) {
            if (_blackPieceList[i] != OFF_BOARD) count++;
            if (_whitePieceList[i] != OFF_BOARD) count++;
        }
        return count;
    }

    public int getBlackPcCount() {
        int count = 0;
        for (int i = 0; i < 16; i++) {
           if (_blackPieceList[i] != OFF_BOARD) count++;
        }
        return count;
    }

    public int getWhitePcCount() {
        int count = 0;
        for (int i = 0; i < 16; i++) {
            if (_whitePieceList[i] != OFF_BOARD) count++;
        }
        return count;
    }


  // has piece
  public static void main(String[] args) {
      Board120  board = FENParser.parseFENotation120("r2q1rk1/pP1p2pp/Q4n2/bbp1p3/Np6/1B3NBn/pPPP1PPP/R3K2R b KQ - 0 1 ");
      Evaluator simple = SimpleEvaluator.getInstance();
      Evaluator pesto  = PESTO.getInstance();
      Evaluator weg    = WeightedCombiEval.getInstance();


      board.print();
      double eval1 = simple.evaluate(board);
      System.out.println("Simple eval " + eval1);
      System.out.println("Pesto eval " + pesto.evaluate(board));
      System.out.printf(board.getBoardData());
      System.out.println(board.print8x8());

      List<Integer> list = MoveGenerator.generatePseudoLegal(board);
      MoveGenerator.sortGen(list);

      int count = 0;
      System.out.println("Leafs : " + list.size());

     // List<Integer> valid = list.stream().filter(
     //         x -> {
     //             board.make(x);
     //             boolean checked = VectorAttack120.isKingInCheck(board);
     //             board.unmake(x);
     //             return !checked;
     //         }
     // ).toList();

      for (int m: list) {
          System.out.println();
          System.out.println((++count) + "\t" + Move.asString(m));
          System.out.println("score"   + Move.getScore(m));
          board.make(m);
          System.out.println("Simple eval " + simple.evaluate(board));
          System.out.println("Pesto eval  " + pesto.evaluate(board));
          System.out.println("Weg eval  " + weg.evaluate(board));
         // System.out.println(board.print8x8());
         // System.out.printf(board.getBoardData());

          board.unmake(m);
          //System.out.printf(board.getBoardData());
      }
  }
}

