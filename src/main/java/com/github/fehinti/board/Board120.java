package com.github.fehinti.board;

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

    private final static byte BLACK = (byte) 0b10000000;
    private final static byte WHITE = (byte) 0b00000000;
    public final static byte  MOVED_FLAG = (byte) 0b1;

    public int lastEntry = 0;

    private final byte[] board120; // 8x8
    private boolean sideToMove; // white or black's turn
    private int fullMoveCounter; // full move counter begins at 1 incremented after black's turn
    private int halfMoveClock; // ply : move of one side only
    private byte castlingRights;
    private byte enPassant;
    private long zobristKey; // hashKey for a single position

    // additional piece list for each type : efficient lookup
    // for move generation to avoid scanning the board for moves
    private final int[] whitePieceList;
    private final int[] blackPieceList;
    private final int[] playHistory;
    private final int[] irreversibleAspect;
    private int ply;
    Stack<Integer> captureEntry;


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
        board120 = new byte[pieces.length];
        System.arraycopy(pieces, 0, board120, 0, pieces.length);
        sideToMove = stm;
        fullMoveCounter = fmCounter;
        halfMoveClock = hmClock;
        enPassant = enPt;
        setCastlingRights(cRights);
        whitePieceList = new int[MAX_LEN_16];
        blackPieceList = new int[MAX_LEN_16];
        fillLists();
        captureEntry = new Stack<>();
        playHistory    = new int[512];
        irreversibleAspect    = new int[512];
        ply = 0;
    }

    public static int getMailbox64Number(int index) {
        return MAILBOX_64[index];
    }

    public static int getMailbox120Number(int index) {
        return MAILBOX_120[index];
    }

    private void fillLists() {
        Arrays.fill(whitePieceList, OFF_BOARD);
        Arrays.fill(blackPieceList, OFF_BOARD);

        int wp= 0;
        int bp= 0;
        int wk = 15;

        for (int sq = 0; sq < BOARD_SIZE; sq++) {
            int square = getMailbox64Number(sq);
            int value = board120[square];
            if (value > 0) {
                if (value == WKING) whitePieceList[wk] = ((value << RANK_8) | square);
                else whitePieceList[wp++] = ((value << RANK_8) | square);
            }
            // * negate value used because signed bit is still maintained if actual value
            // * -num is used, this allows us to use the unused bits
            else if (value < 0){
                if (value == BKING) blackPieceList[wk] = ((-value << RANK_8) | square);
                else blackPieceList[bp++] = ((-value << RANK_8) | square);
            }
        }
        // sort list will be valuable when looking for smallest attackers
        // pieces are sorted with minor pieces first (ordering: P, N, B, R, Q , K)
        Arrays.sort(whitePieceList, 0, 15);
        // sort descending, encoding of pawns > major pieces , this keeps pawns before other pieces
        // allowing us to get smallest attacker for black easily
        sortDescending(blackPieceList);
    }

    private void sortDescending(int[] arr) {
        Integer[] copy = new Integer[15];
        for (int i = 0; i < 15; i++) {
            copy[i] = arr[i];
        }
        Arrays.sort(copy, Collections.reverseOrder());
        for (int i = 0; i < 15; i++) {
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
        this.castlingRights = encodeCastlingRights(wk, wq, bk, bq);
    }

    /**
     * @return single byte encoding of black and white castling rights
     */
    public byte getCastlingRights() {
        return castlingRights;
    }

    public Stack<Integer> getCaptureEntry()
    {
        return captureEntry;
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
        byte castles = castlingRights;
        if (b) {
            // masks blacks bits if white to play
            castles &= ~(BLACK_QUEENSIDE | BLACK_KINGSIDE);
        }
        else            castles &= ~(WHITE_KINGSIDE | WHITE_QUEENSIDE); // vice versa
        return castles != 0;
    }

    // is king side castling right for white available
    public boolean canWhiteCastleKingside() {
        return (castlingRights & WHITE_KINGSIDE) != 0;
    }

    // is queenside castling right for white available
    public  boolean canWhiteCastleQueenside() {
        return (castlingRights & WHITE_QUEENSIDE) != 0;
    }

    // is king side castling right for black available
    public boolean canBlackCastleKingside() {
        return (castlingRights & BLACK_KINGSIDE) != 0;
    }

    public  boolean canBlackCastleQueenside() {
        return (castlingRights & BLACK_QUEENSIDE) != 0;
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
        return sideToMove;
    }

    public int[] getWhitePieceList() {
        int[] copy = new int[MAX_LEN_16];
        for (int i = 0; i < MAX_LEN_16; i++) {
            copy[i] = whitePieceList[i];
        }
        return copy;
    }

    public int[] getBlackPieceList() {
        int[] copy = new int[MAX_LEN_16];
        for (int i = 0; i < MAX_LEN_16; i++) {
            copy[i] = blackPieceList[i];
        }
        return copy;
    }

    public byte getPieceOnSquare(int index) {
        if (index < 0 || index > 119) throw new IllegalArgumentException("index out of bounds.");
        return board120[index];
    }

    public int getBlackKingSq() {
        return blackPieceList[15] & 0xff;
    }

    public int getWhiteKingSq() {
        return whitePieceList[15] & 0xff;
    }

    public int getEnPassant() {
        return enPassant;
    }

    /**
     * @return a decimal number of half moves with respect to the 50 move
     * draw rule. It is reset to zero after a capture or a pawn move and incremented otherwise.
     */
    public int getHalfMoveClock() {
        return halfMoveClock;
    }

    /**
     * @return The number of the full moves in a game. It starts at 1, and is incremented
     * after each Black's move.
     */
    public int getFullMoveCounter() {
        return fullMoveCounter;
    }

    public void setZobrist(long key) {
        zobristKey = key;
    }

    private int getPieceListIndex(int piece, int square, boolean captured) {
        // if this method is called when capture occurs, find the index of the captured piece in its
        // own piece list, else find a piece in our own list (e.g find rook when castling)
        int[] piecelist = (captured) ?  ((!sideToMove) ? whitePieceList : blackPieceList) :
                (sideToMove) ? whitePieceList : blackPieceList;
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
        byte piece = board120[from];
        assert(piece != EMPT_SQ);
        assert(isWhitePiece(piece) == sideToMove);

        addMoveToHistory(move);
        addIrreversibleAspect();

        if ((piece == WKING || piece == BKING || piece == WROOK || piece == BROOK)
                && flag != FLAG_CASTLE) onRookMove(from,  piece, flag);

        int[] side = (sideToMove) ? whitePieceList : blackPieceList;
        int[] xside = (sideToMove) ? blackPieceList : whitePieceList;

        int xindex = OFF_BOARD;
        // every capture will be on the to square except enpassant where piece-to-be-captured
        // will be below enpassant (black) : above enpassant (white)
        int captured = (flag != FLAG_EN_PASSANT) ?  Math.abs(board120[to]) :
                        (sideToMove) ? Math.abs(board120[enPassant - 10]) :
                        Math.abs(board120[enPassant + 10]);
        int val = Math.abs(piece);
        if (captured != 0) { // capture on the board
            if (flag == FLAG_EN_PASSANT) {
                assert((sideToMove) ? board120[to] == BPAWN : board120[to] == WPAWN);
                int epSq = (sideToMove) ? enPassant - 10 : enPassant + 10;
                xindex = getPieceListIndex( captured, epSq , true);
            } else xindex = getPieceListIndex(captured, to, true);
            assert(xindex != OFF_BOARD);
            captureEntry.push(captured << RANK_8 | xindex); // store index of captured piece
        }

        switch (flag) {
            case FLAG_QUIET, FLAG_DOUBLE_PAWN_PUSH -> {
                assert(board120[to] == EMPT_SQ);
                makeMove(from, to, piece);
                halfMoveClock++;
                boolean found = incrementalUpdate(side, index, (val << RANK_8 | to), (val << RANK_8 | from));
                if (!found) throw new RuntimeException("Error f=quiet&dpPush");
                if (flag == FLAG_DOUBLE_PAWN_PUSH) {
                    if (sideToMove) enPassant = (byte) (to - 10);
                    else enPassant = (byte) (to + 10);
                }
            }
            case FLAG_EN_PASSANT -> {
                assert(board120[to] == EMPT_SQ);
                assert(to == enPassant);
                if (xindex == OFF_BOARD) throw new IllegalArgumentException("captured piece index not found + \n" +
                        print8x8() +"\n" + FENParser.getFENotation(this) +"\n" + getBoardData()
                        + "\n" + Move.dbgMove(move) + "\n" + printMailbox() + " " + captured +
                        Arrays.toString((sideToMove) ? blackPieceList : whitePieceList));
                makeMove(from, to, piece);
                int xpos = OFF_BOARD;
                if (sideToMove) {
                    xpos = to - 10;
                    assert(board120[xpos] == BPAWN);
                }
                else {
                    xpos = to + 10;
                    assert(board120[xpos] == WPAWN);
                }
                board120[xpos] = EMPT_SQ;
                halfMoveClock = EMPT_SQ;
                boolean found1 = incrementalUpdate(side, index, (val << RANK_8 | to), (val << RANK_8 | from));
                boolean found2 = incrementalUpdate(xside, xindex, OFF_BOARD, (captured << RANK_8 | xpos));
                if (!found1) throw new RuntimeException("Error f=ep, side" + sideToMove);
                if (!found2) throw new RuntimeException("Error f=ep, xside  " + sideToMove);
            }
            case FLAG_CAPTURE -> {
                assert(board120[to] != EMPT_SQ);
                // captured piece is again opponent
                if (captured == WROOK || captured == Math.abs(BROOK)) onCaptureRook(to);
                if (xindex == OFF_BOARD) throw new IllegalArgumentException("captured piece index not found + \n" +
                        print8x8() +"\n" + FENParser.getFENotation(this) +"\n" + getBoardData()
                        + "\n" + Move.dbgMove(move));
                board120[from] = EMPT_SQ;
                board120[to]   = piece;
                halfMoveClock = EMPT_SQ;
                boolean found1 = incrementalUpdate(side, index, (val << RANK_8 | to), (val << RANK_8 | from));
                boolean found2 = incrementalUpdate(xside, xindex, OFF_BOARD, (captured << RANK_8 | to));
                if (!found1) throw new RuntimeException("Error f=cap, side");
                if (!found2) throw new RuntimeException("Error f=cap, xside");
            }
            case FLAG_PROMOTION, FLAG_PROMOTION_CAPTURE -> {
                byte pp = getPromotionPiece(promotion);
                board120[from] = EMPT_SQ;
                board120[to] = pp;
                boolean found2 = incrementalUpdate(side, index,
                        (Math.abs(pp) << RANK_8 | to), (Math.abs(piece) << RANK_8 | from));
                if (!found2) throw new RuntimeException("Error f=promo, freeslot");
                if (flag == FLAG_PROMOTION_CAPTURE) {
                    if (xindex == OFF_BOARD) throw new IllegalArgumentException("captured piece index not found + \n" +
                            print8x8() +"\n" + FENParser.getFENotation(this) +"\n" + getBoardData()
                            + "\n" + Move.dbgMove(move));
                    if (captured == WROOK || captured == Math.abs(BROOK)) onCaptureRook(to);
                    boolean found3 = incrementalUpdate(xside, xindex, OFF_BOARD, (captured << RANK_8 | to)); // remove piece
                    if (!found3) throw new RuntimeException("Error f=cap&Promo, xside");
                }
            }
            case FLAG_CASTLE ->  makeCastle(from, to, piece);
            default -> throw new IllegalStateException("Unexpected value: " + flag);
        }
        // enPassant no longer valid after every (non-double pawn push)move
        if (flag != FLAG_DOUBLE_PAWN_PUSH) enPassant =  OFF_BOARD;
        if (!isPieceWhite(piece)) fullMoveCounter++;
        sideToMove = !sideToMove;
    }

    /**
     * @param move 32 bit integer encoding of from square, target square, flags and captured piece
     */
    public void unmake(int move) {
        // change to opponent of side that played
        sideToMove = !sideToMove;
        int flag = getFlag(move);
        int from = getFromSquare(move);
        int to   = getTargetSquare(move);
        int promo = getPromotion(move);
        int index = getIndex(move);
        byte piece = board120[to]; // piece has moved to target square
        byte capturedPiece = 0;
        assert(ply != EMPT_SQ);
        assert(board120[to] != EMPT_SQ);
        assert(playHistory[ply - 1] == move);
        unaddIrreversibleAspect();

        int xindex = OFF_BOARD;
        if (flag == FLAG_CAPTURE || flag == FLAG_PROMOTION_CAPTURE || flag == FLAG_EN_PASSANT) {
            int entry = captureEntry.pop();
            capturedPiece = (byte) ((entry >> RANK_8) & 0xff);
            xindex = entry & 0xff;
            if (sideToMove) capturedPiece = (byte) -capturedPiece; // preserves sign bit for black pieces
        }
        // update side THAT moveD
        int v = Math.abs(piece);
        int[] side =  (sideToMove)  ? whitePieceList : blackPieceList;
        int[] xside = (sideToMove)  ? blackPieceList : whitePieceList;

        switch (flag) {
            case FLAG_QUIET, FLAG_DOUBLE_PAWN_PUSH -> {
                assert(board120[from] == EMPT_SQ);
                boolean f = incrementalUpdate(side, index, encode(v, from), encode(v, to));
                if (!f) {
                    throw new RuntimeException("Error unmaking f=quiet&dppush\n" + getBoardData()
                             +"\n" + print8x8() +"\n" + Arrays.toString((sideToMove) ? whitePieceList : blackPieceList)
                       + "\n" + Move.dbgMove(move) + "\n" + (encode(v, from)) +"\n" + (encode(v, to))
                       +"\n" + (blackPieceList[index]) +
                            "\n" + index +
                            "\n" + promo +
                            "\n" + to);
                }
                makeMove(to, from, piece);
            }
            case FLAG_EN_PASSANT -> {
                makeMove(to, from, piece); //reverse capturing pawn to its previous square
                assert(capturedPiece == WPAWN || capturedPiece == BPAWN);// captured piece is a square above enpassant
                if (isPieceWhite(capturedPiece)) board120[enPassant + 10] = capturedPiece;
                else board120[enPassant - 10] = capturedPiece;
                boolean f1 = incrementalUpdate(side, index, encode(v, from), encode(v, to));
                boolean f2 = incrementalUpdate(xside, xindex,
                        encode(Math.abs(capturedPiece), (sideToMove) ? enPassant - 10 : enPassant + 10), OFF_BOARD);
                if (!f1) throw new RuntimeException("Error updating ep capturing piece");
                if (!f2) throw new RuntimeException("Error updating eP captured piece");
            }
            case FLAG_CAPTURE -> {
                assert(board120[from] == EMPT_SQ);
                makeMove(to, from, piece); // return capturing piece
                assert(capturedPiece != EMPT_SQ);
                board120[to] = capturedPiece; // returned captured piece
                boolean f1 = incrementalUpdate(side, index, encode(v, from), encode(v, to));
                boolean f2 = incrementalUpdate(xside, xindex,  encode(Math.abs(capturedPiece), to), OFF_BOARD);
                if (!f1) throw new RuntimeException("Error updating capturing pc");
                if (!f2) throw new RuntimeException("Error updating captured pc");
            }
            case FLAG_PROMOTION, FLAG_PROMOTION_CAPTURE -> {
                assert(board120[from] == EMPT_SQ);
                if (sideToMove) makeMove(to, from, WPAWN);
                else makeMove(to, from, BPAWN);
                int enc = (sideToMove) ? WPAWN : -BPAWN;
                boolean found = incrementalUpdate(side, index, encode(enc, from), encode(v, to));
                if (!found) throw new RuntimeException("Error restoring promoting pawn f=Promotion");
                if (flag == FLAG_PROMOTION_CAPTURE) {
                    board120[to] = capturedPiece;
                    // this has encoding would have been set to -1 in the make's incremental update
                    boolean fd = incrementalUpdate(xside, xindex, (Math.abs(capturedPiece) << RANK_8 | to ), OFF_BOARD);
                    if (!fd) throw new RuntimeException("Error rest;oring prev captured f=Promotion");
                }
            }
            case FLAG_CASTLE -> unmakeCastle(from, to, side, index);
            default -> throw new IllegalArgumentException();
        }
        if (!isPieceWhite(piece)) fullMoveCounter--;
    }

    private void unmakeCastle(int from, int to, int[] side, int index) {
        boolean fRook;
        int rv = (sideToMove) ? WROOK : -BROOK;
        int ri = OFF_BOARD;
        if (sideToMove) {
            board120[E1] = WKING;
            if (to == G1) { // short castles
                board120[G1] = EMPT_SQ;
                board120[F1] = EMPT_SQ; // undo rook's move
                board120[H1] = WROOK;
            }
            else if (to == C1) { // long castle
                board120[C1] = EMPT_SQ; // undo king's move
                board120[D1] = EMPT_SQ; // undo rooks's move
                board120[A1] = WROOK;
            }
            ri = getPieceListIndex(rv, (to == C1) ? D1 : F1, false);
            fRook = incrementalUpdate(side, ri, encode(rv, (to == C1) ? A1 : H1),
                    encode(rv, (to == C1) ? D1 : F1));
        }
        else {
            board120[E8] = BKING;
            if (to == G8) { // short castles
                board120[G8] = EMPT_SQ; // undo king's move
                board120[F8] = EMPT_SQ; // undo rook's move
                board120[H8] = BROOK;
            }
            else if (to == C8) { // long castle
                board120[C8] = EMPT_SQ; // undo rook's move
                board120[D8] = EMPT_SQ; // undo rook's move
                board120[A8] = BROOK; // put rook back on A_8
            }
            ri = getPieceListIndex(rv, (to == C8) ? D8 : F8, false);
            fRook = incrementalUpdate(side, ri, encode(rv, (to == C8) ? A8 : H8),
                    encode(rv, (to == C8) ? D8 : F8));
        }
        if (!fRook) throw new RuntimeException("Error updating Rook f=castle");
        int enc = (sideToMove) ? WKING : -BKING;
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
        if (!canSideCastle(sideToMove)) return; // we cannot castle
        if (sideToMove) {
            assert(board120[from] == WKING);
            if (to == C1) {
                rookFr = A1;  // queenside castle
                rookTo = D1;
            }
            else if (to == G1) {
                rookFr = H1; // kingside castles
                rookTo = F1;
            }
            else throw new IllegalArgumentException("invalid castle");
            castlingRights &= ~(WHITE_QUEENSIDE | WHITE_KINGSIDE); // remove kside and qside castling
        }
        else {
            assert(board120[from] == BKING);
            if (to == C8)  { // queenside
                rookFr = A8;
                rookTo = D8;
            }
            else if (to == G8) { // kingside
                rookFr = H8;
                rookTo = F8;
            }
            else throw new IllegalArgumentException("invalid castle");
            castlingRights &= ~(BLACK_KINGSIDE | BLACK_QUEENSIDE);
        }
        // TODO mask out the moved flag
        int[] side = (sideToMove) ? whitePieceList : blackPieceList;
        byte val = (sideToMove) ? WROOK : BROOK;
        makeMove(from, to, p); // for king
        makeMove(rookFr, rookTo, val); // for rook
        boolean kEntry = incrementalUpdate(side, 15, (Math.abs(p) << RANK_8 | to),
                ((Math.abs(p) << RANK_8) | from)); // king is always on index 15
        if (!kEntry) throw new RuntimeException("Error updating kingside");
        assert(board120[rookFr] == WROOK || board120[rookFr] == BROOK);
        boolean rEntry = incrementalUpdate(side,
                getPieceListIndex(Math.abs(val), rookFr, false),
                (Math.abs(val) << RANK_8 | rookTo),
                (Math.abs(val) << RANK_8 | rookFr));
        if (!rEntry) throw new RuntimeException("Error updating rookside");
    }

    private void makeMove(int from, int to, byte p) {
        board120[from] = EMPT_SQ;
        board120[to] = p;
    }

    private boolean incrementalUpdate(int[] side, int index, int encode, int validate) {
        // if entry is 'off boarded' (captured) do not bother to check if encoding matches previous state
        boolean found = validate == OFF_BOARD && side[index] == OFF_BOARD;
        // compares piece and square encoding match our validate encoding before overwriting
        if ((side[index] & 0xff) == (validate & 0xff) && (side[index] >> RANK_8) == validate >> RANK_8) {
            found = true;
            lastEntry = encode;
        }
        side[index] = encode;
        return  found;
    }

    // remove castling rights when rooks move;
    private void onRookMove(int from, byte piece, int flag) {
        if (!canSideCastle(sideToMove) || flag == FLAG_CASTLE) return; // there are no castling rights to update
        assert(piece == WROOK || piece == WKING ||piece == BKING ||piece == BROOK);
        boolean side = canSideCastle(sideToMove);
        if (sideToMove) {
            if (piece == WKING && side) {
                castlingRights &= ~(WHITE_KINGSIDE | WHITE_QUEENSIDE);
                // whitePieceList[15] &= ~(MOVED_FLAG << 16); // remove moved bit,
            }
            else {
                if (canWhiteCastleQueenside() && from == A1) castlingRights &= ~WHITE_QUEENSIDE;
                if (canWhiteCastleKingside() && from == H1) castlingRights &= ~WHITE_KINGSIDE;
            }
        }
        else {
            if (piece == BKING && side) { // remove long and short castles if king moves
                castlingRights &= ~(BLACK_KINGSIDE | BLACK_QUEENSIDE);
                // blackPieceList[15] &= ~(MOVED_FLAG << 16);
            }
            else {
                if (canBlackCastleQueenside() && from == A8) castlingRights &= ~BLACK_QUEENSIDE;
                if (canBlackCastleKingside() && from == H8) castlingRights &= ~BLACK_KINGSIDE;
            }
        }
    }

    // removes opponents castlign rights  when rooks are captured
    private void onCaptureRook(int square) {
        if (!canSideCastle(!sideToMove)) return; // opponent has no castles
        if (sideToMove) {
            if (square == A8) castlingRights &= ~BLACK_QUEENSIDE;
            else if (square == H8) castlingRights &= ~BLACK_KINGSIDE;
        }
        else {
            if (square == A1) castlingRights &= ~WHITE_QUEENSIDE;
            else if (square == H1) castlingRights &= ~WHITE_KINGSIDE;
        }
    }

    private void addMoveToHistory(int move) {
        playHistory[ply] = move;
    }

    public int getMoveFromHistory() {
        return playHistory[ply];
    }

    private void addIrreversibleAspect() { int ep = (enPassant & 0xff);   // Mask to 6 bits
        int cR = (castlingRights & 0xF) << 8; // Shift and mask to 4 bits
        int hM = (halfMoveClock & 0x3F) << 16; // Shift and mask to 6 bits
        irreversibleAspect[ply++] = (ep | cR | hM);
    }

    private void unaddIrreversibleAspect() {
        int irreversible = irreversibleAspect[--ply];
        int ep = (irreversible & 0xff);
        ep = (ep == 63) ?  OFF_BOARD : ep; // don't remember why 63 is here
        int cR = (irreversible >> 8) & 0x3f;
        int hM = (irreversible >> 16) & 0x3f;
        enPassant  = (byte) ep;
        castlingRights = ((byte) cR);
        halfMoveClock = hM;
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
        return (sideToMove) ? piece : (byte) (BLACK | piece);
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
                System.out.print(mapByteToChar(board120[i * 10 + j]) + " ");
            }
            System.out.println();
        }
    }

    public String printMailbox() {
        StringBuilder sb = new StringBuilder();
        for (int i = 11; i >= 0; i--) {
            for(int j = 0; j < 10; j++) {
                sb.append(mapByteToChar(board120[i * 10 + j]) + " ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    // returns string represntation of en passant square
    public String getEnpassantString() {
        byte sq = (enPassant == OFF_BOARD) ? enPassant : (byte) getMailbox120Number(enPassant);
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
        info.append("\n").append("fullCount = ").append(fullMoveCounter);
        info.append("\n").append("halfMove = ").append(halfMoveClock);
        info.append("\nSide:\t").append((sideToMove) ? "white" : "black").append("\n");
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
                byte piece = board120[(byte) Board120.getMailbox64Number((rank - 1) * FILE_H + file)];
                char c = mapByteToChar(piece);
                if (c == 'k' || c == 'K') board.append('[').append(c).append(']').append('|');
                else if (c == '.') {
                    if (getMailbox64Number(((rank - 1) * RANK_8 + file)) == enPassant) {
                        board.append(' ').append("o ").append('|');
                    }
                    else board.append(' ').append(' ').append(' ').append('|');
                }
                else board.append(' ').append(c).append(' ').append('|');
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

  // has piece
  public static void main(String[] args) {
      Board120  board = FENParser.parseFENotation120("rnbqkbnr/ppppp1pp/8/8/4p3/8/PPPPKPPP/RNBQ1BNR w kq - 0 3");
      board.print();
      System.out.printf(board.getBoardData());
      System.out.println(board.print8x8());

      long start = System.currentTimeMillis();
      List<Integer> list = MoveGenerator.generatePseudoLegal(board);
      System.out.println("end" + "\t" + (System.currentTimeMillis() - start));
      int count = 0;
      System.out.println();

      for (int m: list) {
          System.out.println();
          System.out.println((++count) + "\t" + Move.printMove(m));
          board.make(m);
          System.out.println(board.print8x8());
          System.out.printf(board.getBoardData());
          board.unmake(m);
          System.out.printf(board.getBoardData());
      }
  }
}

