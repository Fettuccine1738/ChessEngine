/**
 * The {@code Board} class is a Hybrid solution for representing a board in Chess
 * my representation uses a Square centric 8 by 8 enum array of
 * {@code PieceType} enums that represents all piece types of both colors as well as
 * empty tiles. A Piece-centric disjoint list of all Piece types is used for efficient
 * move generation to avoid scanning the entire board, type and color of pieces are
 * associated by a certain index range or disjoint lists or arrays, index range can be
 * found in {@code BoardUtilities} class.
 * Additionally a static 10 x 12 and 8 x 8 static board is kept to make checking off board
 * moves in move generation easier.
 * @Author Favour F. Atilade.
 */
package com.github.fehinti.board;

import java.util.Arrays;
import java.util.List;

import static com.github.fehinti.board.BoardUtilities.*;
import static com.github.fehinti.board.Move.*;
import static com.github.fehinti.board.PieceType.*;

import com.github.fehinti.piece.Pawn;
import com.github.fehinti.piece.PieceMove;


public class Board  implements Cloneable{
    // test strings for FEN Notation
    static String FEN_INIT = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    static String FEN_2 = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1";
    static String FEN_3 = "rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq c6 0 2";
    static String FEN_4 = "rnbqkbnr/pp1ppppp/8/2p5/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2";
    static String FEN_K2 =  "8/8/8/3K4/8/8/8/8 w - - 0 1"; // single king 8 coordinates test
    static String FEN_K1 =  "8/8/8/8/8/8/8/K7 w - - 0 1"; // single king offboard test
    static String FEN_B =  "8/8/3p4/2p1B3/3b1P2/4P3/8/8 w - - 0 1"; // bishop off board test
    static String FEN_R =  "8/8/8/3p4/2r1R3/4P3/8/8 w - - 0 1"; // rook move generation test
    static String FEN_Q =  "8/8/8/3p1q2/2Q3P1/4P3/8/8 w - - 0 1"; // queen move generation test
    static String FEN_P =  "8/8/3p4/2P1p3/8/3P4/4P3/8 w - - 0 1"; // pawn move testing
    static String FEN_N = "8/3p4/8/4N3/3n1P2/8/4P3/8 w - - 0 1"; // knight move test
    // tests pawn moves, pseudo generation and undo
    // 23 for black, 12 for white
    static String FEN_P2 = "rnbqkbnr/p1p1p3/3p3p/1p6/2P1Pp2/8/PP1P1PpP/RNBQKB1R b - - 0 1";


    static String FEN_TESTPINS = "1R1K2R1/PPPPPPPP/8/6b1/3r4/8/8/8 w - - 0 1";
    static String FEN_PINS = "8/8/8/3r4/6b1/8/PPPPPPPP/1R1K2R1 w - - 0 1";
    static String FEN_ONE_PIN = "8/8/8/3r4/8/5b2/PPPP1PPP/2RKR3 w - - 0 1";
    // check if pseudo move generates pieces capture king
    // static String FEN_NO_WKING_CAP = "3b4/1n6/1p6/K7/8/8/8/r3q3 w - - 0 1";
    static String FEN_NO_WKING_CAP = "3b4/1n6/8/K7/8/8/8/r3q3 w - - 0 1"; // removes pawn blocking bishop
    // static String FEN_NO_BKING_CAP = "R3Q3/8/8/8/k7/1P6/1N6/3B4 b - - 0 1";
    static String FEN_NO_BKING_CAP = "R3Q3/8/8/8/k7/8/1N6/3B4 b - - 0 1";

    // white/black king long& shrt castles available
    static String FEN_CASTLE_RIGHTS_KING_MOVE = "r3k2r/pppbqppp/2n2n2/3pp3/3PP3/2N2N2/PPPBQPPP/R3K2R b KQkq - 5 7";
    // black king castles both sides white has none(already castled)
    static String FEN_CASTLE_RIGHTS_BKING_MOVE = "r3k2r/pppbqppp/2n2n2/3pp3/3PP3/2N2N2/PPPBQPPP/2KR3R b kq - 6 7";
    // white castles only king side
    static String FEN_WK_QUEENSIDE = "r3k2r/pppbqppp/2n2n2/3pp3/3PP3/2N2N2/PPPBQPPP/R3K1R1 w Qkq - 5 7";
    // king side blocked by white's pieces
    static String FEN_WQ = "r3k2r/ppp1qppp/2n2n2/3pp3/3PP3/2N2N2/PPPBQPPP/R3KBNR b KQkq - 5 7";
    // white king side clear
    static String FEN_WK_CLR = "r3k2r/ppp1qppp/2n2n2/3pp3/3PP3/2NB1N2/PPPBQPPP/RN2K2R b KQkq - 5 7";
    // castling with check scenarios
    static String FEN_CHK_1 = "r3k2r/ppp1qp1p/2n3p1/3pp3/3PP3/2NQ1b2/PPPB1PPP/R3K2R w KQkq - 5 7";
    static String FEN_CHK_2 = "r3k2r/ppp1qp1p/2n3p1/3pp3/3PP3/2N5/PPPBQbPP/R3K2R w KQkq - 5 7"; // king in check, cannot castle for white, has to capture
    static String FEN_CHK_3 = "r3k2r/ppp2ppp/2n5/3pp3/3PP3/2N2N2/PPPBQPbP/R3K2R w KQkq - 5 7"; // rook being attacked, no kingside castle for white
    static String FEN_CHK4 = "r3k2r/ppp2ppp/8/3pp3/3PP3/2N2N2/PPPBQPPq/R3K2R w KQkq - 5 7"; // h file rook attacked no kingside castles for white
    // minimal material easy testing for both black and white
    static String FEN_MIN1 = "4k2r/8/8/8/8/8/8/R3K3 w Qk - 0 1"; // blakc kingside and white queenside
    static String FEN_MIN2 = "r3k3/8/8/8/8/8/8/4K2R b Kq - 0 1"; // white kingside and black queen side available


    // sentinel and blocking piece
    private final static byte OFF_BOARD = -1;
    private final static byte WHITE_KINGSIDE = 1; // 0001
    private final static byte WHITE_QUEENSIDE = 2; // 0010
    private final static byte BLACK_KINGSIDE = 4; // 0100
    private final static byte BLACK_QUEENSIDE = 8; // 1000

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
    private final PieceType[] board64; // 8x8
    private boolean sideToMove; // white or black's turn
    private int fullMoveCounter; // full move counter begins at 1 incremented after black's turn
    private int halfMoveClock; // ply : move of one side only
    private byte castlingRights;
    private byte enPassant; // single bit to encode King and Queen side castle fro black and white
    private long positionHash; // hashKey for a single position

    // additional piece list for each type : efficient lookup for move generation instead of scanning the board for moves
    // implemented according to Fritz Reul architecture, with each piece having it's own lookup table
    private final int[] whitePieceList;
    private final int[] blackPieceList;
    private final int[] playHistory; // holds moves played on board : game history
    private final int[] irreversibleAspect; // holds irreversible moves such as castlingRights, enpassant,
    private int ply;

    public Board() {
        board64 = initializeBoard64();
        sideToMove = WHITE;
        fullMoveCounter = 1;
        halfMoveClock = RANK_1;
        enPassant = OFF_BOARD; // modifiable
        castlingRights = encodeCastlingRights(true, true, true, true);
        whitePieceList = new int[MAX_MAX];
        blackPieceList = new int[MAX_MAX];
        playHistory    = new int[1000];
        irreversibleAspect    = new int[1000];
        ply = RANK_1;
        fillPieceList();
    }

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
    public Board(PieceType[] pieces, boolean stm,int fmCounter, int hmClock, byte cRights, byte enPt) {
        if (pieces == null || pieces.length != BOARD_SIZE) throw new IllegalArgumentException("pieces must have 64 elements");
        board64 = pieces;
        sideToMove = stm;
        fullMoveCounter = fmCounter;
        halfMoveClock = hmClock;
        enPassant = enPt;
        setCastlingRights(cRights);
        whitePieceList = new int[MAX_MAX];
        blackPieceList = new int[MAX_MAX];
        fillPieceList();
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
        this.positionHash = board.positionHash;
        this.ply = board.ply;
    }

    public PieceType[] getBoard64() {
        PieceType[] copy = new PieceType[BOARD_SIZE];
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
        for (int i = 0; i < N; i++) {
            copy[i] = whitePieceList[i];
        }
        return copy;
    }

    /**
     * @return disjoint set of black pieces.
     */
    public int[] getBlackPieceList() {
        int[] copy = new int[blackPieceList.length];
        int N = blackPieceList.length;
        for (int i = 0; i < N; i++) {
            copy[i] = blackPieceList[i];
        }
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

    // a single byte encodes both castling rights (king side and queenside) for black and white

    /**
     * @param wk true if white can castle king side     0001
     * @param wq true if white can castle queen side    0010
     * @param bk true if white can castle king side     0100
     * @param bq true if white can castle queen side    1000
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
        byte castles = getCastlingRights();
        if (b) { // masks blacks bits if white to play
            castles &= ~(BLACK_QUEENSIDE | BLACK_KINGSIDE);
        }
        else            castles &= ~(WHITE_KINGSIDE | WHITE_QUEENSIDE);
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
     * @return piecetype enum on  index/ tile.
     */
    public PieceType getPieceOnBoard(int index) {
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

    /**
     * @param move 32 bit encoding of move information: from square, target square, flags etc
     */
    public void make(int move) {
        int flag = getFlag(move);
        int to = getTargetSquare(move);
        int from = getFromSquare(move);
        int capturedPiece = getCapturedPiece(move);
        int promotion = getPromotionPiece(move);
        PieceType piece = board64[from];
        assert(piece != EMPTY);

        addIrreversibleAspect(enPassant, castlingRights, halfMoveClock);
        addMoveToHistory(move);
        updatePly(true);

        // remove castles for rooks
        int isRookOrKing = Math.abs(piece.getValue());
        if (isRookOrKing == WHITE_KING.getValue() || isRookOrKing == WHITE_ROOK.getValue()
        && flag != FLAG_CASTLE) onRookMove(from, to, piece, flag);

        int[] side = (sideToMove) ? whitePieceList : blackPieceList;
        int[] xside = (sideToMove) ? blackPieceList : whitePieceList;

        int val = piece.getValue();
        int floor = getPieceListFloor(piece);
        int ceil = getPieceListCeiling(piece);
        int xfloor, xceil;
        xfloor = xceil = OFF_BOARD;
        if (capturedPiece != 0) {
            PieceType xpiece = getPieceType(capturedPiece);
            xfloor = getPieceListFloor(xpiece);
            xceil = getPieceListCeiling(xpiece);
        }

        switch (flag) {
            case FLAG_QUIET, FLAG_DOUBLE_PAWN_PUSH -> {
                assert(board64[to] == EMPTY);
                makeMove(from, to, piece);
                // increment half move clock for 50 move draw rule
                halfMoveClock++;
                boolean found = incrementalUpdate(side, floor, ceil, from, (val << RANK_8 | to));
                if (!found) throw new RuntimeException("Error f=quiet&dpPush");
                if (flag == FLAG_DOUBLE_PAWN_PUSH) {
                    if (piece.isWhite()) setEnPassant((byte) (to - RANK_8));
                    else setEnPassant((byte) (to + RANK_8));
                }
            }
            case FLAG_EN_PASSANT -> {
                assert(board64[to] == EMPTY);
                assert(to == enPassant && enPassant != OFF_BOARD);
                makeMove(from, to, piece);
                int xpos = OFF_BOARD;
                if (piece.isWhite()) {// enPassant capture happens at double push - 8
                    xpos = to - RANK_8;
                    assert(board64[xpos] == BLACK_PAWN);
                }
                else {// enPassant capture happens at double push + 8
                    xpos = to + RANK_8;
                    assert(board64[xpos] == WHITE_PAWN);
                }
                board64[xpos] = EMPTY;
                halfMoveClock = RANK_1; // reset to 0 with every capture.
                boolean found1 = incrementalUpdate(side, floor, ceil, from, (val << RANK_8 | to));
                boolean found2 = incrementalUpdate(xside, xfloor, xceil, xpos, 0); // remove piece
                if (!found1) throw new RuntimeException("Error f=ep, side" + sideToMove);
                if (!found2) throw new RuntimeException("Error f=ep, xside  " + sideToMove);
            }
            case FLAG_CAPTURE -> {
                assert(board64[to] != EMPTY);
                board64[from] = EMPTY;
                board64[to]   = piece;
                halfMoveClock = RANK_1; // reset to zero
                boolean found1 = incrementalUpdate(side, floor, ceil, from, (val << RANK_8 | to));
                boolean found2 = incrementalUpdate(xside, xfloor, xceil, to, 0); // remove piece
                if (!found1) throw new RuntimeException("Error f=cap, side");
                if (!found2) throw new RuntimeException("Error f=cap, xside");
            }
            case FLAG_PROMOTION -> {
                PieceType pp = getPieceType(promotion); // promoted Piece
                int f = getPieceListFloor(pp);
                int c = getPieceListCeiling(pp);
                board64[from] = EMPTY;
                board64[to] = pp;
                boolean found1 = incrementalUpdate(side, floor, ceil, from, 0);
                // used 0 because we're looking for the next free spot
                boolean found2 = incrementalUpdate(side, f, c, 0, ((pp.getValue()) << RANK_8 | to));
                if (capturedPiece != 0) {
                    boolean found3 = incrementalUpdate(xside, xfloor, xceil, to, 0); // remove piece
                    if (!found3) throw new RuntimeException("Error f=cap&Promo, xside");
                }
                if (!found1) throw new RuntimeException("Error f=promo, clearPawn");
                if (!found2) throw new RuntimeException("Error f=promo, freeslot");
            }
            case FLAG_CASTLE ->   makeCastle(from, to, piece, move);
            default -> throw new IllegalStateException("Unexpected value: " + flag);
        }// enPassant no longer valid after every (non-double pawn push)move
        if (flag != FLAG_DOUBLE_PAWN_PUSH) setEnPassant(OFF_BOARD);
        if (piece.isBlack()) fullMoveCounter++;
        alternateSide();
    }

    // remove castling rights when rooks move;
    private void onRookMove(int from, int to, PieceType piece, int flag) {
        if (castlingRights == 0 || flag == FLAG_CASTLE) return; // if no castles exists
        int value = piece.getValue();
        byte castles = getCastlingRights();
        assert(Math.abs(value) == 6 || Math.abs(value) == 4);
        if (piece.isWhite()) {
                if (value == WHITE_KING.getValue()
                        && (canWhiteCastleKingside(castles)
                        || canWhiteCastleQueenside(castles))) {
                    castles &= ~WHITE_KINGSIDE;
                    castles &= ~WHITE_QUEENSIDE;
                }
                else {
                    if (canWhiteCastleQueenside(getCastlingRights()) && from == A_1) castles &= ~WHITE_QUEENSIDE;
                    if (canWhiteCastleKingside(getCastlingRights()) && from == H_1) castles &= ~WHITE_KINGSIDE;
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

    private void makeCastle(int from, int to, PieceType p, int move){
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
        int fl = getPieceListFloor(WHITE_KING);
        int cl = getPieceListCeiling(WHITE_KING);
        int rfl = getPieceListFloor(WHITE_ROOK);
        int rcl = getPieceListCeiling(WHITE_ROOK);
        int val = (p.isWhite()) ? WHITE_ROOK.getValue() : BLACK_ROOK.getValue();
        makeMove(from, to, p); // for king
        makeMove(rookFr, rookTo, getPieceOnBoard(rookFr)); // for rook
        incrementalUpdate(side, fl, cl, from, (p.getValue() << RANK_8 | to));
        assert(Math.abs(getPieceOnBoard(rookFr).getValue()) == WHITE_ROOK.getValue());
        incrementalUpdate(side, rfl, rcl, rookFr, (val << RANK_8 | rookTo));
    }

    private void addMoveToHistory(int move) {
        playHistory[ply] = move;
    }

    private void updatePly(boolean b) {
        assert(ply >= OFF_BOARD);
        if (b)   ply++;
        else    ply--;
    }

    private void addIrreversibleAspect(int enPassant, int castleRights, int halfMoveClock) {
            int ep = (enPassant & 0x3F);   // Mask to 6 bits
            int cR = (castlingRights & 0xF) << 6; // Shift and mask to 4 bits
            int hM = (halfMoveClock & 0x3F) << 10; // Shift and mask to 6 bits
            irreversibleAspect[ply] = (ep | cR | hM);
    }

    private void unaddIrreversibleAspect() {
        int copyPly = ply - 1;
        int irreversible = irreversibleAspect[copyPly];
        int ep = (irreversible & 0x3f); // mask lower 6 bits
        ep = (ep == 63) ?  OFF_BOARD : ep; // don't remember why 63 is here
        int cR = (irreversible >> 6) & 0x3f;
        int hM = (irreversible >> 10) & 0x3f;
        setEnPassant((byte) ep);
        setCastlingRights((byte) cR);
        setHalfMoveClock(hM);
    }

    // switch sides implementation;
    public void alternateSide() {
        sideToMove = !sideToMove;
    }

    public void setSideToMove(boolean side) {
        this.sideToMove = side;
    }

    /**
     * @param move 32 bit integer encoding of from square, target square, flags and captured piece
     */
    public void unmake(int move) {
        alternateSide();
        int flag = getFlag(move);
        int from = getFromSquare(move);
        int to = getTargetSquare(move);
        int capturedPiece = getCapturedPiece(move);
        int promotion = getPromotionPiece(move);
        PieceType piece = board64[to]; // piece has moved to target square
        assert(ply != RANK_1);
        assert(board64[to] != EMPTY);
        int checkPly = ply - 1;
        assert(playHistory[checkPly] == move);
        unaddIrreversibleAspect();
        updatePly(false);

        // update side THAT moveD
        int[] side =  (sideToMove)  ? whitePieceList : blackPieceList;
        int[] xside = (sideToMove)  ? blackPieceList : whitePieceList;
        int fl = getPieceListFloor(piece);
        int ceil = getPieceListCeiling(piece);

        switch (flag) {
            case FLAG_QUIET, FLAG_DOUBLE_PAWN_PUSH -> {
                assert(board64[from] == EMPTY);
                makeMove(to, from, piece);
                boolean f = incrementalUpdate(side, fl, ceil, to,
                        encode(piece.getValue(), from));
                if (!f) throw new RuntimeException("Error unmaking f=quiet&dppush");
            }
            case FLAG_EN_PASSANT -> {
                makeMove(to, from, piece); //reverse capturing pawn to its previous square
                PieceType cap = getPieceType(capturedPiece);
                assert(Math.abs(cap.getValue()) == WHITE_PAWN.getValue());
                if (cap.isWhite()) board64[enPassant + RANK_8] = cap; // captured piece is a square above enpassant
                else board64[enPassant - RANK_8] = cap;
                boolean f1 = incrementalUpdate(side, fl, ceil, to,
                        encode(piece.getValue(), from));
                boolean f2 = incrementalUpdate(xside,
                        fl, ceil, 0, encode(capturedPiece, (sideToMove) ? enPassant - 8 : enPassant + 8));
                if (!f1) throw new RuntimeException("Error updating ep capturing piece");
                if (!f2) throw new RuntimeException("Error updating eP captured piece");
            }
            case FLAG_CAPTURE -> {
                assert(board64[from] == EMPTY);
                makeMove(to, from, piece);
                PieceType capt = getPieceType(capturedPiece);
                board64[to] = capt;
                int xfl = getPieceListFloor(capt);
                int xceil = getPieceListCeiling(capt);
                boolean f1 = incrementalUpdate(side, fl, ceil, to, encode(piece.getValue(), from));
                boolean f2 = incrementalUpdate(xside, xfl, xceil, 0, encode(piece.getValue(), to));
                if (!f1) throw new RuntimeException("Error updating capturing pc");
                if (!f2) throw new RuntimeException("Error updating captured pc");
            }
            case FLAG_PROMOTION -> {
                assert(board64[from] == EMPTY);
                if (sideToMove) makeMove(to, from, WHITE_PAWN);
                else makeMove(to, from, BLACK_PAWN);
                int enc = (sideToMove) ? WHITE_PAWN.getValue() : BLACK_PAWN.getValue();
                boolean found = incrementalUpdate(side,
                        getPieceListFloor(WHITE_PAWN),
                        getPieceListCeiling(WHITE_PAWN), to,
                        encode(enc, from));

               if (capturedPiece != 0) {
                   PieceType xPc = PieceType.getPieceType(capturedPiece);
                  int xfl = getPieceListFloor(xPc);
                  int xcl = getPieceListCeiling(xPc);
                  // this has encoding would have been set to 0 in the make's incremental update
                  boolean fd = incrementalUpdate(xside, xfl, xcl, 0, (xPc.getValue() << RANK_8 | to ));
                  if (!fd) throw new RuntimeException("Error restoring prev captured f=Promotion");
               }
                PieceType pp = PieceType.getPieceType(promotion);
                assert(Math.abs(pp.getValue()) != 1);
                int pfl = getPieceListFloor(piece);
                int pcl = getPieceListCeiling(piece);
                boolean found1 = incrementalUpdate(side, pfl, pcl, to, 0);
                // encodes previously removed pawn
                boolean found2 = incrementalUpdate(side, fl, ceil, 0, ((piece.getValue() << 8) | to));
                if (!found1) throw new RuntimeException("Error restoring prev captured f=Promotion");
                if (!found2) throw new RuntimeException("Error restoring prev captured f=Promotion");
            }
            case FLAG_CASTLE -> unmakeCastle(from, to, side,fl,ceil);
            default -> throw new IllegalArgumentException();
        }
        if (piece.isBlack()) fullMoveCounter--;
        // update side to play
    }

    private int encode(int pc, int info) {
        return (pc << RANK_8) | info;
    }

    private void unmakeCastle(int from, int to, int[] side, int fl, int ceil) {
        boolean fRook;
        if (!sideToMove) {
            board64[E_1] = WHITE_KING;
            if (to == G_1) { // short castles
                board64[G_1] = EMPTY;
                board64[F_1] = EMPTY; // undo rook's move
                board64[H_1] = WHITE_ROOK;
            }
            else if (to == C_1) { // long castle
                board64[C_1] = EMPTY; // undo king's move
                board64[D_1] = EMPTY; // undo king's move
                board64[A_1] = WHITE_ROOK;
            }
            fRook = incrementalUpdate(side, getPieceListFloor(WHITE_ROOK),
                    getPieceListCeiling(WHITE_ROOK),
                    (to == C_1) ? D_1 : F_1,
                    encode(WHITE_ROOK.getValue(), (to == C_1) ? A_1 : H_1));
        }
        else {
            board64[E_8] = BLACK_KING;
            if (to == G_8) { // short castles
                board64[G_8] = EMPTY; // undo rook's move
                board64[F_8] = EMPTY; // undo rook's move
                board64[H_8] = BLACK_ROOK;
            }
            else if (to == C_8) { // long castle
                board64[C_8] = EMPTY; // undo rook's move
                board64[D_8] = EMPTY; // undo rook's move
                board64[A_8] = BLACK_ROOK; // put rook back on A_8
            }
            fRook = incrementalUpdate(side,getPieceListFloor(BLACK_ROOK),
                    getPieceListCeiling(BLACK_ROOK),
                    (to == C_8) ? D_8 : F_8, // position after castling is either +1 / -1 of king's
                    encode(BLACK_ROOK.getValue(),
                            (to == C_8) ? A_8 : H_8)); // position before castling

        }
        if (!fRook) throw new RuntimeException("Error updating Rook f=castle");
        int enc = (!sideToMove) ? WHITE_KING.getValue() : BLACK_KING.getValue();
        boolean f1 = incrementalUpdate(side, fl, ceil, to,
                encode(enc, from));
        if (!f1) throw new RuntimeException("Error updating kingside");
    }

    private void makeMove(int from, int to, PieceType p) {
        board64[from] = EMPTY;
        board64[to] = p;
    }


    private boolean incrementalUpdate(int[] side, int start, int end, int target, int encode) {
        boolean found = false;
        for (; start < end; start++) {
            int decode = side[start];
            int square = (decode & 0xff);
            if (square == target) {
                side[start] = encode;
                return true;
            }
        }
        return  found;
    }

    // intialize field variable Piecetype []
    private static PieceType[] initializeBoard64() {
         return new PieceType[] {
                WHITE_ROOK, WHITE_KNIGHT, WHITE_BISHOP, WHITE_QUEEN, WHITE_KING, WHITE_BISHOP, WHITE_KNIGHT, WHITE_ROOK,
                WHITE_PAWN, WHITE_PAWN,   WHITE_PAWN,   WHITE_PAWN,  WHITE_PAWN, WHITE_PAWN,   WHITE_PAWN,   WHITE_PAWN,
                EMPTY,      EMPTY,        EMPTY,        EMPTY,       EMPTY,      EMPTY,        EMPTY,        EMPTY,
                EMPTY,      EMPTY,        EMPTY,        EMPTY,       EMPTY,      EMPTY,        EMPTY,        EMPTY,
                EMPTY,      EMPTY,        EMPTY,        EMPTY,       EMPTY,      EMPTY,        EMPTY,        EMPTY,
                EMPTY,      EMPTY,        EMPTY,        EMPTY,       EMPTY,      EMPTY,        EMPTY,        EMPTY,
                BLACK_PAWN, BLACK_PAWN,   BLACK_PAWN,   BLACK_PAWN,  BLACK_PAWN, BLACK_PAWN,   BLACK_PAWN,   BLACK_PAWN,
                BLACK_ROOK, BLACK_KNIGHT, BLACK_BISHOP, BLACK_QUEEN, BLACK_KING, BLACK_BISHOP, BLACK_KNIGHT, BLACK_ROOK
        };
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

    // fill in piece list
    private void fillPieceList() {
        fillPieceList(blackPieceList, BLACK_KING);
        fillPieceList(blackPieceList, BLACK_QUEEN);
        fillPieceList(blackPieceList, BLACK_ROOK);
        fillPieceList(blackPieceList, BLACK_KNIGHT);
        fillPieceList(blackPieceList, BLACK_BISHOP);
        fillPieceList(blackPieceList, BLACK_PAWN);
        fillPieceList(whitePieceList, WHITE_KING);
        fillPieceList(whitePieceList, WHITE_QUEEN);
        fillPieceList(whitePieceList, WHITE_ROOK);
        fillPieceList(whitePieceList, WHITE_BISHOP);
        fillPieceList(whitePieceList, WHITE_KNIGHT);
        fillPieceList(whitePieceList, WHITE_PAWN);
    }

    // fills piece list encoding piece value and square 64 coordinates
    private void fillPieceList(int[] pieceList, PieceType piece) {
        int index = getPieceListFloor(piece);
        int max = getPieceListCeiling(piece);
        for (int square = 0, j = index; square < BOARD_SIZE && j <= max; square++) {
            if (board64[square] == piece) {
                // encode both square and piece on the square
                pieceList[j] = ((piece.getValue() << 8) | square);
                j++;
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
        if (sq < RANK_1 || sq >= BOARD_SIZE) throw new IllegalArgumentException();
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

        for (rank = RANK_8; rank > RANK_1; rank--) {
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

        for (rank = RANK_8; rank > RANK_1; rank--) {
            board.append(rank).append('\t').append('|');
            for (file = FILE_A; file < FILE_H; file++) {
                char c = board64[(rank - 1) * RANK_8 + file].getName();
                if (c == 'k' || c == 'K') board.append('[').append(c).append(']').append('|');
                else if (c == '.')board.append(' ').append(' ').append(' ').append('|');
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
        return info.toString();
    }

    public static void main(String[] args) {
        int count = 0;
        String falseCap = "rnbqkbnr/1ppppppp/p7/8/P7/8/1PPPPPPP/RNBQKBNR w KQkq - 3 2";
        String blackAtckd = "rnbqkbnr/ppp1pppp/3p4/8/Q7/2P5/PP1PPPPP/RNB1KBNR b KQkq - 1 1";
        // Board board = FENParser.parseFENotation("8/5k2/8/2Pp4/2B5/1K6/8/8 w - d6 0 1");
        Board b = FENParser.parseFENotation("r1bqkbnr/pppp1ppp/n7/8/3p4/2K5/PPP1PPPP/RNBQ1BNR w kq - 0 1");
        System.out.println(b.print());

       // List<Integer> wronEnPassant = PieceMove.pseudoLegal(board);
        List<Integer> list = PieceMove.pseudoLegal(b);
        System.out.println(list.size());
        list = PieceMove.validateMoves(b, list);

        for (Integer m : list) {
            System.out.printf(++count +"\t" + printMove(m) + "\n");
            b.make(m);
            System.out.println(b);
            System.out.println(b.getBoardData());
            System.out.println(m);
            System.out.println("\n");
            b.unmake(m);
            System.out.println(b);
            System.out.println(b.getBoardData());
            System.out.println("\n");
        }

    }


}