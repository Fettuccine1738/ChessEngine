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
package com.github.fehinti.board;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import static com.github.fehinti.board.BoardUtilities.*;
import static com.github.fehinti.board.Move.*;
import static com.github.fehinti.piece.Piece.*;
import static com.github.fehinti.board.ZobristHash.*;
import static com.github.fehinti.piece.PieceMove.generatePseudoLegal;


import com.github.fehinti.piece.AttackMap;
import com.github.fehinti.piece.Piece;
import com.github.fehinti.piece.PieceMove;
import java.util.Stack;


public class Board  implements Cloneable{
    // look up table for offboard move generation;
    private final static int[] MAILBOX_64 = new int[BOARD_SIZE]; // maps from 64 to 120
    private final static int[] MAILBOX_120 = new int[BOARD_SIZE_120]; // maps from 120 to 64

    //int mailbox[120] = {
                //-1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                //-1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                //-1,  0,  1,  2,  3,  4,  5,  6,  7, -1,
                //-1,  8,  9, 10, 11, 12, 13, 14, 15, -1,
                //-1, 16, 17, 18, 19, 20, 21, 22, 23, -1,
                //-1, 24, 25, 26, 27, 28, 29, 30, 31, -1,
                //-1, 32, 33, 34, 35, 36, 37, 38, 39, -1,
                //-1, 40, 41, 42, 43, 44, 45, 46, 47, -1,
                //-1, 48, 49, 50, 51, 52, 53, 54, 55, -1,
                //-1, 56, 57, 58, 59, 60, 61, 62, 63, -1,
                //-1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                //-1, -1, -1, -1, -1, -1, -1, -1, -1, -1
    //};
//
    //int mailbox64[64] = {
        //        21, 22, 23, 24, 25, 26, 27, 28,
                //31, 32, 33, 34, 35, 36, 37, 38,
                //41, 42, 43, 44, 45, 46, 47, 48,
                //51, 52, 53, 54, 55, 56, 57, 58,
                //61, 62, 63, 64, 65, 66, 67, 68,
                //71, 72, 73, 74, 75, 76, 77, 78,
                //81, 82, 83, 84, 85, 86, 87, 88,
                //91, 92, 93, 94, 95, 96, 97, 98
    //};

    private final Piece[] board64; // 8x8
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
    public Board(Piece[] pieces, boolean stm, int fmCounter, int hmClock, byte cRights, byte enPt) {
        if (pieces == null || pieces.length != BOARD_SIZE)
            throw new IllegalArgumentException("pieces must have 64 elements");
        board64 = pieces;
        sideToMove = stm;
        fullMoveCounter = fmCounter;
        halfMoveClock = hmClock;
        enPassant = enPt;
        setCastlingRights(cRights);
        whitePieceList = new int[MAX_LEN_16];
        blackPieceList = new int[MAX_LEN_16];
        fillLists();
        captureEntry = new Stack<>();
        playHistory    = new int[1000];
        irreversibleAspect    = new int[1000];
        ply = 0;
    }

    // Clones a board from another board instance
    public Board(Board board) {
        this.board64 = board.getBoard64();
        this.sideToMove = board.sideToMove;
        this.fullMoveCounter = board.fullMoveCounter;
        this.halfMoveClock = board.halfMoveClock;
        this.enPassant = board.enPassant;
        this.castlingRights = board.castlingRights;
        this.whitePieceList = board.getWhitePieceList();
        this.blackPieceList = board.getBlackPieceList();
        this.playHistory = board.getPlayHistory();
        this.irreversibleAspect = board.getIrreversibleAspect();
        this.zobristKey = board.zobristKey;
        this.ply = board.ply;
    }

    public Piece[] getBoard64() {
        Piece[] copy = new Piece[BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++) {
            copy[i] = board64[i];
        }
        return copy;
    }

    /**
     * @return disjoint set of white pieces (encoded with value and current position).
     */
    public int[] getWhitePieceList() {
        int[] copy = new int[whitePieceList.length];
        int N = whitePieceList.length;
        System.arraycopy(whitePieceList, 0, copy, 0, N );
        return copy;
    }

    /**
     * @return disjoint set of black pieces.
     */
    public int[] getBlackPieceList() {
        int[] copy = new int[blackPieceList.length];
        int N = blackPieceList.length;
        System.arraycopy(blackPieceList, 0, copy, 0, N);
        return copy;
    }

    public int[] getPlayHistory() {
        int[] copy = new int[playHistory.length];
        int N = playHistory.length;
        System.arraycopy(playHistory, 0, copy, 0, N);
        return copy;
    }

    public int[] getIrreversibleAspect() {
        int[] copy = new int[irreversibleAspect.length];
        int N = irreversibleAspect.length;
        System.arraycopy(irreversibleAspect, 0, copy, 0, N);
        return copy;
    }

    /**
     * @return side(white (TRUE) / black (TRUE) to move.
     */
    public boolean getSideToMove() {
        return sideToMove;
    }

    /**
     * @param fullMoves assigns fullMoves to field fullMoveCounter
     */
    public void setFullMoveCounter(int fullMoves) {
        this.fullMoveCounter = fullMoves;
    }

    /**
     * @param halfMove assigns  halfMove to field halfMoveClock
     */
    public void setHalfMoveClock(int halfMove) {
        this.halfMoveClock = halfMove;
    }

    /**
     * @param rights byte encoding of queen side and king side castle rights of black and white
     */
    public void setCastlingRights(byte rights) {
        boolean wk = canWhiteCastleKingside(rights);
        boolean wq = canWhiteCastleQueenside(rights);
        boolean bk = canBlackCastleKingside(rights);
        boolean bq = canBlackCastleQueenside(rights);
        this.castlingRights = encodeCastlingRights(wk, wq, bk, bq);
    }

    /**
     * @param eP single byte 0-63 corresponding to square on board.
     *           available on only third(white) and 5th(black) rank.
     */
    public void setEnPassant(byte eP) {
        this.enPassant = eP;
    }

    /**
     * @return enPassant field
     */
    public byte getEnPassant() {
        return enPassant;
    }

    /**
     * @return single byte encoding of black and white castling rights
     */
    public byte getCastlingRights() {
        return castlingRights;
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

    // a single byte encodes both castling rights (king side and queenside) for black and white
    /**
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
    public boolean canWhiteCastleKingside(byte rights) {
        return (rights & WHITE_KINGSIDE) != 0;
    }

    // is queenside castling right for white available
    public  boolean canWhiteCastleQueenside(byte rights) {
        return (rights & WHITE_QUEENSIDE) != 0;
    }

    // is king side castling right for black available
    public boolean canBlackCastleKingside(byte rights) {
        return (rights & BLACK_KINGSIDE) != 0;
    }

    public  boolean canBlackCastleQueenside(byte rights) {
        return (rights & BLACK_QUEENSIDE) != 0;
    }

    /**
     * @param index index
     * @throws IllegalArgumentException if index is not between 0 and 63.
     * @return Piece enum on  index/ tile.
     */
    public Piece getPieceOnBoard(int index) {
        if (index < 0 || index >= BOARD_SIZE) {
            throw new IllegalArgumentException("index out of bounds");
        }
        return board64[index];
    }

    public static int getMailbox64Number(int index) {
        return MAILBOX_64[index];
    }

    public static int getMailbox120Number(int index) {
        return MAILBOX_120[index];
    }

    private int getPieceListIndex(int piece, int square, boolean captured) {
        // if this method is called when capture occurs, find the index of the captured piece in its
        // own piece list, else find a piece in our own list (e.g find rook when castling)
        int[] piecelist = (captured) ?  ((!sideToMove) ? whitePieceList : blackPieceList) :
                (sideToMove) ? whitePieceList : blackPieceList;
        for (int index = 0; index < piecelist.length; index++) {
            int pie = piecelist[index] >> 8; // piece value
            int pos = piecelist[index] & 0xff; // square
            if (pie == piece && pos == square) return index;
        }
        return OFF_BOARD;
    }

    /**
     * @param move 32 bit encoding of move information: from square, target square, flags etc
     */
    public void make(int move) {
        int flag = getFlag(move);
        int to = getTargetSquare(move);
        int from = getFromSquare(move);
        int capturedPiece = getCapturedPiece(move);
        int promotion = getPromotionPiece(move);
        int index = getIndex(move);
        Piece piece = board64[from];
        assert(piece != EMPTY);
        assert(piece.isWhite() == sideToMove);

        addMoveToHistory(move);
        addIrreversibleAspect();

        int isRookOrKing = Math.abs(piece.getValue());
        if (isRookOrKing == WHITE_KING.getValue() || isRookOrKing == WHITE_ROOK.getValue()
        && flag != FLAG_CASTLE) onRookMove(from,  piece, flag);

        int[] side = (sideToMove) ? whitePieceList : blackPieceList;
        int[] xside = (sideToMove) ? blackPieceList : whitePieceList;

        int val = piece.getValue();
        int xindex = OFF_BOARD;
        if (capturedPiece != 0) {
            if (flag == FLAG_EN_PASSANT) {
                assert(Math.abs(capturedPiece) == 1);
                xindex = getPieceListIndex(capturedPiece, (sideToMove) ? enPassant - 8 : enPassant + 8, true);
            } else xindex = getPieceListIndex(capturedPiece, to, true);
            assert(xindex != OFF_BOARD);
            if (xindex == OFF_BOARD) throw new RuntimeException("xindex not found");
            captureEntry.push(xindex);
        }

        switch (flag) {
            case FLAG_QUIET, FLAG_DOUBLE_PAWN_PUSH -> {
                assert(board64[to] == EMPTY);
                makeMove(from, to, piece);
                halfMoveClock++;
                boolean found = incrementalUpdate(side, index, (val << RANK_8 | to), (val << 8 | from));
                if (!found) throw new RuntimeException("Error f=quiet&dpPush");
                if (flag == FLAG_DOUBLE_PAWN_PUSH) {
                    if (piece.isWhite()) setEnPassant((byte) (to - RANK_8));
                    else setEnPassant((byte) (to + RANK_8));
                }
            }
            case FLAG_EN_PASSANT -> {
                assert(board64[to] == EMPTY);
                assert(to == enPassant);
                makeMove(from, to, piece);
                int xpos = OFF_BOARD;
                // piece captured by enpassant is a square after enpassant for white
                // a square before ep for black
                if (sideToMove) {
                    xpos = to - RANK_8;
                    assert(board64[xpos] == BLACK_PAWN);
                }
                else {
                    xpos = to + RANK_8;
                    assert(board64[xpos] == WHITE_PAWN);
                }
                board64[xpos] = EMPTY;
                halfMoveClock = EMPT_SQ;
                boolean found1 = incrementalUpdate(side, index, (val << RANK_8 | to), (val << RANK_8 | from));
                boolean found2 = incrementalUpdate(xside, xindex, OFF_BOARD, (capturedPiece << 8 | xpos)); // remove piece
                if (!found1) throw new RuntimeException("Error f=ep, side" + sideToMove);
                if (!found2) throw new RuntimeException("Error f=ep, xside  " + sideToMove);
            }
            case FLAG_CAPTURE -> {
                assert(board64[to] != EMPTY);
                // captured piece is again opponent
                if (capturedPiece == Math.abs(WHITE_ROOK.getValue())) onCaptureRook(to);
                board64[from] = EMPTY;
                board64[to]   = piece;
                halfMoveClock = EMPT_SQ;
                boolean found1 = incrementalUpdate(side, index, (val << RANK_8 | to), (val << RANK_8 | from));
                boolean found2 = incrementalUpdate(xside, xindex, OFF_BOARD,(capturedPiece << RANK_8 | to));
                if (!found1) throw new RuntimeException("Error f=cap, side");
                if (!found2) throw new RuntimeException("Error f=cap, xside");
            }
            case FLAG_PROMOTION -> {
                Piece pp = getPiece(promotion);
                board64[from] = EMPTY;
                board64[to] = pp;
                boolean found2 = incrementalUpdate(side, index,
                        ((pp.getValue()) << RANK_8 | to), (piece.getValue() << RANK_8 | from));
                if (capturedPiece != 0) {
                    if (capturedPiece == Math.abs(WHITE_ROOK.getValue())) onCaptureRook(to);
                    boolean found3 = incrementalUpdate(xside, xindex, OFF_BOARD, (capturedPiece << RANK_8 | to)); // remove piece
                    if (!found3) throw new RuntimeException("Error f=cap&Promo, xside");
                }
                if (!found2) throw new RuntimeException("Error f=promo, freeslot");
            }
            case FLAG_CASTLE ->  makeCastle(from, to, piece);
            default -> throw new IllegalStateException("Unexpected value: " + flag);
        }
        // enPassant no longer valid after every (non-double pawn push)move
        if (flag != FLAG_DOUBLE_PAWN_PUSH) setEnPassant((byte) OFF_BOARD);
        if (piece.isBlack()) fullMoveCounter++;
        alternateSide();
    }

    // removes opponents castlign rights  when rooks are captured
    private void onCaptureRook(int square) {
        if (!canSideCastle(!sideToMove)) return; // opponent has no castles
        if (sideToMove) {
            if (square == A_8) castlingRights &= ~BLACK_QUEENSIDE;
            else if (square == H_8) castlingRights &= ~BLACK_KINGSIDE;
        }
        else {
            if (square == A_1) castlingRights &= ~WHITE_QUEENSIDE;
            else if (square == H_1) castlingRights &= ~WHITE_KINGSIDE;
        }
    }

    // remove castling rights when rooks move;
    private void onRookMove(int from, Piece piece, int flag) {
        // ? previously (!canSideCastle(!side) ?? what the hell
        if (!canSideCastle(sideToMove) || flag == FLAG_CASTLE) return; // there are no castling rights to update
        int value = piece.getValue();
        byte castles = getCastlingRights();
        assert(Math.abs(value) == 6 || Math.abs(value) == 4);
        if (piece.isWhite()) {
                if (value == WHITE_KING.getValue()
                        && (canWhiteCastleKingside(castles)
                        || canWhiteCastleQueenside(castles))) {
                    castles &= ~(WHITE_KINGSIDE | WHITE_QUEENSIDE);
                }
                else {
                    if (canWhiteCastleQueenside(castles) && from == A_1) castles &= ~WHITE_QUEENSIDE;
                    if (canWhiteCastleKingside(castles) && from == H_1) castles &= ~WHITE_KINGSIDE;
                }
        }
        else {
                if (value == BLACK_KING.getValue()
                        && (canBlackCastleKingside(castles)
                        || canBlackCastleQueenside(castles))) { // remove long and short castles if king moves
                    castles &= ~BLACK_KINGSIDE;
                    castles &= ~BLACK_QUEENSIDE;
                }
                else {
                    if (canBlackCastleQueenside(castles) && from == A_8) castles &= ~BLACK_QUEENSIDE;
                    if (canBlackCastleKingside(castles) && from == H_8) castles &= ~BLACK_KINGSIDE;
                }
            }
            setCastlingRights(castles);
    }

    private void makeCastle(int from, int to, Piece p){
        int rookFr = 0;
        int rookTo = 0;
        byte castles = getCastlingRights();
        if (!canSideCastle(p.isWhite())) return; // we cannot castle
        if (p.isWhite()) {
            assert(getPieceOnBoard(from) == WHITE_KING);
            if (to == C_1) {
                rookFr = A_1;  // queenside castle
                rookTo = D_1;
            }
            else if (to == G_1) {
                rookFr = H_1; // kingside castles
                rookTo = F_1;
            }
            else throw new IllegalArgumentException("invalid castle");
            castles &= ~(WHITE_QUEENSIDE | WHITE_KINGSIDE); // remove kside and qside castling
            setCastlingRights(castles);
        }
        else {
            assert(getPieceOnBoard(from) == BLACK_KING);
            if (to == C_8)  { // queenside
                rookFr = A_8;
                rookTo = D_8;
            }
            else if (to == G_8) { // kingside
                rookFr = H_8;
                rookTo = F_8;
            }
            else throw new IllegalArgumentException("invalid castle");
            castles &= ~(BLACK_KINGSIDE | BLACK_QUEENSIDE);
            setCastlingRights(castles);
        }
        int[] side = (sideToMove) ? whitePieceList : blackPieceList;
        int val = (sideToMove) ? WHITE_ROOK.getValue() : BLACK_ROOK.getValue();
        makeMove(from, to, p); // for king
        makeMove(rookFr, rookTo, getPieceOnBoard(rookFr)); // for rook
        incrementalUpdate(side, 0, (p.getValue() << RANK_8 | to), (p.getValue() << RANK_8 | from)); // king is always on index 0
        assert(Math.abs(getPieceOnBoard(rookFr).getValue()) == WHITE_ROOK.getValue());
        incrementalUpdate(side, getPieceListIndex(val, rookFr, false), (val << RANK_8 | rookTo), (val << RANK_8 | rookFr));
    }

    private void addMoveToHistory(int move) {
        playHistory[ply] = move;
    }

    public int getWhiteKingSq() {
        return (whitePieceList[0] & 0xff);
    }

    public int getBlackKingSq() {
        return (blackPieceList[0] & 0xff);
    }

    private void addIrreversibleAspect() {
            int ep = (enPassant & 0x3F);   // Mask to 6 bits
            int cR = (castlingRights & 0xF) << 6; // Shift and mask to 4 bits
            int hM = (halfMoveClock & 0x3F) << 10; // Shift and mask to 6 bits
            irreversibleAspect[ply++] = (ep | cR | hM);
    }

    private void unaddIrreversibleAspect() {
        int irreversible = irreversibleAspect[--ply];
        int ep = (irreversible & 0x3f);
        ep = (ep == 63) ?  OFF_BOARD : ep; // don't remember why 63 is here
        int cR = (irreversible >> 6) & 0x3f;
        int hM = (irreversible >> 10) & 0x3f;
        setEnPassant((byte) ep);
        setCastlingRights((byte) cR);
        setHalfMoveClock(hM);
    }

    // switch sides implementation;
    private void alternateSide() {
        sideToMove = !sideToMove;
    }

    /**
     * @param move 32 bit integer encoding of from square, target square, flags and captured piece
     */
    public void unmake(int move) {
        alternateSide(); // change to opponent of side that played
        int flag = getFlag(move);
        int from = getFromSquare(move);
        int to = getTargetSquare(move);
        int capturedPiece = getCapturedPiece(move);
        int promo = getPromotionPiece(move);
        int index = getIndex(move);
        Piece piece = board64[to]; // piece has moved to target square
        assert(ply != EMPT_SQ);
        assert(board64[to] != EMPTY);
        int checkPly = ply - 1;
        assert(playHistory[checkPly] == move);
        unaddIrreversibleAspect();

        int xindex = OFF_BOARD;
        int v = piece.getValue();
        if (capturedPiece != 0) {
            xindex = captureEntry.pop();
        }
        // update side THAT moveD
        int[] side =  (sideToMove)  ? whitePieceList : blackPieceList;
        int[] xside = (sideToMove)  ? blackPieceList : whitePieceList;

        switch (flag) {
            case FLAG_QUIET, FLAG_DOUBLE_PAWN_PUSH -> {
                assert(board64[from] == EMPTY);
                makeMove(to, from, piece);
                boolean f = incrementalUpdate(side, index, encode(v, from), encode(v, to));
                if (!f) throw new RuntimeException("Error unmaking f=quiet&dppush");
            }
            case FLAG_EN_PASSANT -> {
                makeMove(to, from, piece); //reverse capturing pawn to its previous square
                Piece cap = getPiece(capturedPiece);
                assert(Math.abs(cap.getValue()) == WHITE_PAWN.getValue());
                if (cap.isWhite()) board64[enPassant + RANK_8] = cap; // captured piece is a square above enpassant
                else board64[enPassant - RANK_8] = cap;
                boolean f1 = incrementalUpdate(side, index, encode(v, from), encode(v, to));
                boolean f2 = incrementalUpdate(xside, xindex,
                        encode(capturedPiece, (sideToMove) ? enPassant - 8 : enPassant + 8), OFF_BOARD);
                if (!f1) throw new RuntimeException("Error updating ep capturing piece");
                if (!f2) throw new RuntimeException("Error updating eP captured piece");
            }
            case FLAG_CAPTURE -> {
                assert(board64[from] == EMPTY);
                makeMove(to, from, piece); // return capturing piece
                Piece capt = getPiece(capturedPiece);
                assert(capt != EMPTY);
                board64[to] = capt; // returned captured piece
                boolean f1 = incrementalUpdate(side, index, encode(v, from), encode(v, to));
                boolean f2 = incrementalUpdate(xside, xindex,  encode(capt.getValue(), to), OFF_BOARD);
                if (!f1) throw new RuntimeException("Error updating capturing pc");
                if (!f2) throw new RuntimeException("Error updating captured pc");
            }
            case FLAG_PROMOTION -> {
                assert(board64[from] == EMPTY);
                if (sideToMove) makeMove(to, from, WHITE_PAWN);
                else makeMove(to, from, BLACK_PAWN);
                int enc = (sideToMove) ? WHITE_PAWN.getValue() : BLACK_PAWN.getValue();
                boolean found = incrementalUpdate(side, index, encode(enc, from), encode(promo, to));
                if (!found) throw new RuntimeException("Error restoring promoting pawn f=Promotion");
                if (capturedPiece != 0) {
                    Piece xPc = Piece.getPiece(capturedPiece);
                    board64[to] = xPc;
                   // this has encoding would have been set to 0 in the make's incremental update
                   boolean fd = incrementalUpdate(xside, xindex, (capturedPiece << RANK_8 | to ), OFF_BOARD);
                   if (!fd) throw new RuntimeException("Error restoring prev captured f=Promotion");
                }
            }
            case FLAG_CASTLE -> unmakeCastle(from, to, side, index);
            default -> throw new IllegalArgumentException();
        }
        if (piece.isBlack()) fullMoveCounter--;
    }

    private int encode(int pc, int info) {
        return (pc << RANK_8) | info;
    }

    private void unmakeCastle(int from, int to, int[] side, int index) {
        boolean fRook;
        int rv = (sideToMove) ? WHITE_ROOK.getValue() : BLACK_ROOK.getValue();
        int ri = OFF_BOARD;
        if (sideToMove) {
            board64[E_1] = WHITE_KING;
            if (to == G_1) { // short castles
                board64[G_1] = EMPTY;
                board64[F_1] = EMPTY; // undo rook's move
                board64[H_1] = WHITE_ROOK;
            }
            else if (to == C_1) { // long castle
                board64[C_1] = EMPTY; // undo king's move
                board64[D_1] = EMPTY; // undo rooks's move
                board64[A_1] = WHITE_ROOK;
            }
            ri = getPieceListIndex(rv, (to == C_1) ? D_1 : F_1, false);
            fRook = incrementalUpdate(side, ri, encode(rv, (to == C_1) ? A_1 : H_1),
                    encode(rv, (to == C_1) ? D_1 : F_1));
        }
        else {
            board64[E_8] = BLACK_KING;
            if (to == G_8) { // short castles
                board64[G_8] = EMPTY; // undo king's move
                board64[F_8] = EMPTY; // undo rook's move
                board64[H_8] = BLACK_ROOK;
            }
            else if (to == C_8) { // long castle
                board64[C_8] = EMPTY; // undo rook's move
                board64[D_8] = EMPTY; // undo rook's move
                board64[A_8] = BLACK_ROOK; // put rook back on A_8
            }
            ri = getPieceListIndex(rv, (to == C_8) ? D_8 : F_8, false);
            fRook = incrementalUpdate(side, ri, encode(rv, (to == C_8) ? A_8 : H_8),
                    encode(rv, (to == C_8) ? D_8 : F_8));
        }
        if (!fRook) throw new RuntimeException("Error updating Rook f=castle");
        int enc = (sideToMove) ? WHITE_KING.getValue() : BLACK_KING.getValue();
        assert(index == 0);
        boolean f1 = incrementalUpdate(side, index, encode(enc, from), encode(enc, to));
        if (!f1) throw new RuntimeException("Error updating kingside");
    }

    private void makeMove(int from, int to, Piece p) {
        board64[from] = EMPTY;
        board64[to] = p;
    }

    private boolean incrementalUpdate(int[] side, int index, int encode, int validate) {
        // if entry is 'off boarded' (captured) do not bother to check if encoding matches previous state
        boolean found = validate == OFF_BOARD && side[index] == OFF_BOARD;
        if ((side[index] & 0xff) == (validate & 0xff) && (side[index] >> RANK_8) == validate >> RANK_8) {
            found = true;
        }
        side[index] = encode;
        return  found;
    }

    // fill board 64 and board 120
     static {
        int index = 0;
        // fill index 120 with OFF_BOUND values
        Arrays.fill(MAILBOX_120, OFF_BOARD);

        for (int i = 0; i < RANK_8; i++) {
            for (int j = 0; j < FILE_H; j++) {
                index = mapIndex64To120(i, j);
                MAILBOX_64[i * FILE_H + j] = index;
                MAILBOX_120[index] = (i << 3) + j;  // multiplied by 8
            }
        }
    }

    private void fillLists() {
        // 0 index reserved for king
        int wp = 1;
        int bp = 1;
        for (int square = 0; square < BOARD_SIZE; square++) {
           int value = board64[square].getValue();
           if (value > 0) {
               if (value == WHITE_KING.getValue()) whitePieceList[0] = ((value << RANK_8) | square);
               else whitePieceList[wp++] = ((value << RANK_8) | square);
           }
           else if (value < 0) {
               if (value == BLACK_KING.getValue()) blackPieceList[0] = ((value << RANK_8) | square);
               else blackPieceList[bp++] = ((value << 8) | square);
           }
        }
    }

    // maps index of an 8 x8 board to 10 x 12 board
    public static int mapIndex64To120(int rank, int file) {
        return (rank * 10) + 21 + file;
    }

    // retunrs string represntation of en passant square
    public String getEnpassantString() {
        byte sq = enPassant;
        if (sq == OFF_BOARD) return "-";
        if (sq < EMPT_SQ || sq >= BOARD_SIZE) throw new IllegalArgumentException();
        StringBuilder sb = new StringBuilder(2);
        int rank = sq >> 3;
        int file = sq & 7;
        sb.append((char) (file + 'a')).append(rank + 1);
        return sb.toString();
    }

    /**
     * @return 8 x 8 representation of board with enums in index represented by their char values
     *         Uppercase chars for white and lowercase for black
     */
    public String toString() {
        StringBuilder board = new StringBuilder();
        int rank; int file;

        for (rank = RANK_8; rank > EMPT_SQ; rank--) {
            board.append(rank).append('\t');
            for (file = FILE_A; file < FILE_H; file++) {
                board.append(board64[(rank - 1) * RANK_8 + file].getName()).append('\t');
            }
            board.append('\n');
        }
        board.append('\t');
        for (file = 0; file < FILE_H; file++) board.append((char) ('a' + file)).append('\t');

        return board.toString();
    }

    /**
     * @return combination of board representationg and board info
     */
    public String print() {
        StringBuilder board = new StringBuilder();
        int rank; int file;
        String newline = "+---";
        board.append('\t');
        for (int i = 0; i < 8; i++) board.append(newline);
        board.append("+\n");

        for (rank = RANK_8; rank > EMPT_SQ; rank--) {
            board.append(rank).append('\t').append('|');
            for (file = FILE_A; file < FILE_H; file++) {
                char c = board64[(rank - 1) * RANK_8 + file].getName();
                if (c == 'k' || c == 'K') board.append('[').append(c).append(']').append('|');
                else if (c == '.') {
                    if (((rank - 1) * RANK_8 + file) == enPassant) {
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

    public String getBoardData() {
        StringBuilder info = new StringBuilder();
        info.append("\nEnPassant:\t").append(getEnpassantString()).append("\n");
        info.append("Castles: ").append(FENParser.getCastlingRightsFENotation(this));
        info.append("\n").append("fullCount = ").append(fullMoveCounter);
        info.append("\n").append("halfMove = ").append(halfMoveClock);
        info.append("\nSide:\t").append(sideToMove).append("\n");
        return info.toString();
    }

    private static void dbg_perft(int odepth, int curDepth, Board b, Scanner sc) {
        if (curDepth == 0) return;
        else {
            List<Integer> psuedo = generatePseudoLegal(b);
            List<Integer> list = PieceMove.generateLegalMoves(b, psuedo);
            System.out.println(list.size() + "pseudo");
            System.out.println(" Enter an index:  ");

            //int index = sc.nextInt();
            int move = list.getFirst();
            b.make(move);
            System.out.println(b.print());
            System.out.println(b.getBoardData());
            System.out.println(FENParser.getFENotation(b));
            dbg_perft(odepth, curDepth - 1, b, sc);

            b.unmake(move);
            System.out.println(b.print());
            System.out.println(b.getBoardData());
            System.out.println(FENParser.getFENotation(b));
        }
    }

    public static void main(String[] args) {
        //int count = 0;
        //String falseCap = "rnbqkbnr/1ppppppp/p7/8/P7/8/1PPPPPPP/RNBQKBNR w KQkq - 3 2";
        //String blackAtckd = "rnbqkbnr/ppp1pppp/3p4/8/Q7/2P5/PP1PPPPP/RNB1KBNR b KQkq - 1 1";
        ////Scanner sc = new Scanner(System.in);
        ////System.out.println("DEPTH? ");
        ////int depth = sc.nextInt();
        ////dbg_perft(depth, depth, b, sc);
        String bp = "r3k2r/8/3Q4/8/8/5q2/8/R3K2R b KQkq - 0 1";
        String wp = "r3k2r/8/5Q2/8/8/3q4/8/R3K2R w KQkq - 0 1";
        String third ="8/5k2/8/2Pp4/2B5/1K6/8/8 w - d6 0 1";
        Board b = FENParser.parseFENotation(wp);
        System.out.println(b.print());
        System.out.println(b.getBoardData());
        List<Integer> list = generatePseudoLegal(b);
        int count =0;

        for (int i : list) {
            System.out.println(++count);
            System.out.println(Move.printMove(i));
            printMove(i);
            // if (getCapturedPiece(i) == 0 && getPromotionPiece(i) == 0) continue;
            b.make(i);
            if (AttackMap.isKingInCheck(b)) {
                count--; // count only valid moves
                System.out.println("illegal move ");
            }
            System.out.println(b.print());
            System.out.println(b.getBoardData() + "\n");
            b.unmake(i);
            System.out.println(b.print());
            System.out.println(b.getBoardData() + "\n");
        }
        System.out.println("Legal count " + count);

        // try (BufferedReader br = Files.newBufferedReader(Paths.get("C:\\Users\\favya\\IdeaProjects\\ChessEngine\\src\\test\\java\\com\\github\\fehinti\\piece\\fenway.txt"))) {
            // br.lines().forEach(e -> {
                // Board b = FENParser.parseFENotation(e);
                // List<Integer> moves = generatePseudoLegal(b);
                // System.out.println(e + "\t" + moves.size());
            // });
        // } catch (IOException e) {
            // System.out.println(e.getMessage());
        // }
    }
}
