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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Stack;

import static com.github.fehinti.board.BoardUtilities.*;
import static com.github.fehinti.board.Move.*;
import static com.github.fehinti.board.PieceType.*;
import com.github.fehinti.piece.King;


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
     * @return disjoint list of only white pieces.
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
     * @return disjoint list of only white pieces.
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
     * @return side(white/black) to move.
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
        // if (move == null) throw new IllegalArgumentException("moves must not be null");
        int flags = getFlags(move);
        int to = getTargetSquare(move);
        int from = getFromSquare(move);
        int capturedPiece = getCapturedPiece(move);
        int promotion = getPromotionPiece(move);
        PieceType piece = board64[from];

        assert(piece != EMPTY);
        addIrreversibleAspect(enPassant, castlingRights, halfMoveClock);
        addMoveToHistory(move);
        updatePly(true);
        if (flags != FLAG_CASTLE) {
            updatePieceList(move, piece, from, to, flags); }

        // remove castles for rooks
        int isRookOrKing = Math.abs(piece.getValue());
        if (isRookOrKing == WHITE_KING.getValue()
                || isRookOrKing == WHITE_ROOK.getValue())  onRookMove(from, to, piece, flags);

        switch (flags) {
            case FLAG_QUIET -> {
                assert(board64[to] == EMPTY);
                makeMove(from, to, piece);
                // increment half move clock for 50 move draw rule
                halfMoveClock++;
            }
            case FLAG_EN_PASSANT -> {
                assert(board64[to] == EMPTY);
                assert(enPassant != OFF_BOARD);
                makeMove(from, to, piece);
                PieceType pt = board64[to - RANK_8];
                assert(pt != EMPTY);
                if (piece.isWhite()) {// enPassant capture happens at double push - 8
                    assert(pt.isBlack());
                    board64[to - RANK_8] = EMPTY;
                }
                else {// enPassant capture happens at double push + 8
                    assert(pt.isWhite());
                    board64[to + RANK_8] = EMPTY;
                }
                setEnPassant(OFF_BOARD); // reset enPassant to offboard
                halfMoveClock = RANK_1; // reset to 0 with every capture.
            }
            case FLAG_DOUBLE_PAWN_PUSH -> {
                assert(board64[to] == EMPTY);
                makeMove(from, to, piece);
                if (piece.isWhite()) setEnPassant((byte) (to - RANK_8));
                else setEnPassant((byte) (to + RANK_8));
                halfMoveClock++;
            }
            case FLAG_CAPTURE -> {
                assert(board64[to] != EMPTY);
                board64[from] = EMPTY;
                board64[to]   = piece;
                halfMoveClock = RANK_1;
            }
            case FLAG_PROMOTION -> {
                PieceType promotedPiece = getPieceType(promotion);
                board64[from] = EMPTY;
                board64[to] = promotedPiece;
            }
            case FLAG_CASTLE ->   makeCastle(from, to, piece, move);
            default -> throw new IllegalStateException("Unexpected value: " + flags);
        }
        if (piece.isBlack()) fullMoveCounter++;
        alternateSide();
    }

    // remove castling rights when rooks move;
    private void onRookMove(int from, int to, PieceType piece, int flag) {
        if (castlingRights == 0 || flag == FLAG_CASTLE) return; // if no castles exists
        int value = piece.getValue();
        byte castles = getCastlingRights();
        assert(Math.abs(value) == 6 || Math.abs(value) == 4);

        if (value == WHITE_ROOK.getValue()
                || value == WHITE_KING.getValue()) {
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
            castles &= ~(WHITE_QUEENSIDE | WHITE_KINGSIDE);
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
        // move, piece, from, to, flags
        // int move, PieceType p, int from, int to, int flag
        makeMove(from, to, p); // for king
        updatePieceList(move, p, from, to, FLAG_CASTLE);
        updatePieceList(move, getPieceOnBoard(rookFr), rookFr, rookTo, FLAG_CASTLE); // BUGBUG:
        assert(getPieceOnBoard(rookFr).getValue() == WHITE_ROOK.getValue());
        makeMove(rookFr, rookTo, getPieceOnBoard(rookFr)); // for rook
        // moved rook off to but still uses the piece on to (which is empty) and messes up the update list
    }


    private void addMoveToHistory(int move) {
        playHistory[ply] = move;
    }

    private void updatePly(boolean b) {
        assert(ply >= OFF_BOARD);
        if (b) {
            ply++;
        }
        else {
            ply--;
        }
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


    private void updatePieceList(int move, PieceType p, int from, int to, int flag) {
        if (p == null) throw new IllegalArgumentException("Piece is null");
        int floor;
        int ceil;
        int square, xsquare;
        boolean sideToPlay = p.isWhite();
        int[] side = (sideToPlay) ? whitePieceList : blackPieceList;
        int[] xside = (sideToPlay) ? blackPieceList : whitePieceList;
        int promotedPiece = getPromotionPiece(move);
        PieceType capturedPiece = getPieceType(getCapturedPiece(move));

        // update piece list for promotion
        if (flag == FLAG_PROMOTION) {
            PieceType promo = getPieceType(promotedPiece);
            floor = getPieceListFloor(promo);
            ceil = getPieceListCeiling(promo);

            for (int i = floor; i < ceil; i++) {
                if (side[i] == RANK_1) { // place the piece encoding in next available position ( = 0)
                    side[i] = ((promo.getValue() << RANK_8) | to);
                    break;
                }
            }
        }
        // update opponent's list if opponent piece is captured
        if (capturedPiece != EMPTY) {
            int index = getPieceListFloor(capturedPiece);
            int ceiling = getPieceListCeiling(capturedPiece);
            for (; index < ceiling; index++) {
                if (xside[index] != RANK_1) {
                    xsquare = xside[index] & 0xff;
                    // update piece's square to zero
                    if (flag == FLAG_EN_PASSANT) {
                        if (p.isWhite() && (xsquare == to + RANK_8)) xside[index] = RANK_1;
                        else if (p.isBlack() && (xsquare == to - RANK_8)) xside[index] = RANK_1;
                    }
                    // set capture to zero for any other flag
                    if (xsquare == to) {
                        xside[index] = RANK_1;
                        break;
                    }
                }
            }
        }

        // update current side piecelist
        floor = getPieceListFloor(p);
        ceil = getPieceListCeiling(p);
        boolean found = false;
        for (int index = floor; index < ceil; index++) {
            if (side[index] != RANK_1) { // check entry if it is not empty ( = 0)
                square = side[index] & 0xff;
                // update piece's square to targetSquare if it is the piece on the current index
                if (square == from) {
                    // if promotion is possible set the promoting pawn to a 0
                    // else update it's current square
                    side[index] = (flag == FLAG_PROMOTION) ? RANK_1 : ((p.getValue() << RANK_8) | to);
                    found = true;
                    break; // piece value in piece list adjusted
                }
            }
            // do not update opponent list if it is a quiet move or double pawn push
            // if (flag == FLAG_QUIET || flag == FLAG_DOUBLE_PAWN_PUSH || flag == FLAG_EN_PASSANT) return;
        }
        if (!found) throw new RuntimeException("Error: Piece encoding not found");
        // this.sideToMove = !sideToMove; // update side to play
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
        int flag = getFlags(move);
        int to = getTargetSquare(move);
        int from = getFromSquare(move);
        int capturedPiece = getCapturedPiece(move);
        int promotion = getPromotionPiece(move);
        PieceType piece = board64[to]; // piece has moved to target square
        assert(ply != RANK_1);
        assert(board64[to] != EMPTY);
        int checkPly = ply - 1;
        assert(playHistory[checkPly] == move);
        unaddIrreversibleAspect();
        updatePly(false);
        if (flag == FLAG_PROMOTION) {
            if (sideToMove == WHITE)
                decrementalUpdate(move, WHITE_PAWN, from, to, flag);
            else
                decrementalUpdate(move, BLACK_PAWN, from, to, flag);
        }
        else if(flag != FLAG_CASTLE) {
            decrementalUpdate(move, piece, from, to, flag);
        }

        switch (flag) {
            case FLAG_QUIET, FLAG_DOUBLE_PAWN_PUSH -> {
                assert(board64[from] == EMPTY);
                makeMove(to, from, piece);
            }
            case FLAG_EN_PASSANT -> {
                makeMove(to, from, piece);
                PieceType cap = getPieceType(capturedPiece);
                if (cap.isWhite()) board64[enPassant + RANK_8] = cap; // captured piece is a square above enpassant
                else board64[enPassant - RANK_8] = cap;
            }
            case FLAG_CAPTURE -> {
                assert(board64[from] == EMPTY);
                makeMove(to, from, piece);
                board64[to] = getPieceType(capturedPiece);
            }
            case FLAG_PROMOTION -> {
                assert(board64[from] == EMPTY);
                if (sideToMove == WHITE) {
                    makeMove(to, from, WHITE_PAWN);
                } else makeMove(to, from, BLACK_PAWN);
                if (capturedPiece != RANK_1) board64[to] = getPieceType(capturedPiece); // replace piece on board
            }
            case FLAG_CASTLE -> unmakeCastle(from, to, piece, move);
            default -> throw new IllegalArgumentException();
        }
        if (piece.isBlack()) fullMoveCounter--;
        // update side to play
        alternateSide();
    }

    private void unmakeCastle(int from, int to, PieceType piece, int move) {
        assert(from == E_8);
        int rookFr = 0;
        int rookTo = 0;
        if (piece.isWhite()) {
            if (to == C_1) { // long castles
                board64[from] = getPieceOnBoard(C_1);
                board64[C_1] = EMPTY;
                assert(getPieceOnBoard(D_1) == WHITE_ROOK);
                board64[A_1] = getPieceOnBoard(D_1);
                board64[D_1] = EMPTY;
                rookFr = A_1;
                rookTo = D_1;
            }
            else if (to == G_1) { // short castles
                board64[from] = getPieceOnBoard(G_1);
                board64[G_1] = EMPTY;
                assert(getPieceOnBoard(F_1) == WHITE_ROOK);
                board64[H_1] = getPieceOnBoard(F_1);
                board64[F_1] = EMPTY;
                rookFr = H_1;
                rookTo = F_1;
            }
        }
        else {
            if (to == C_8) { // long castles
                board64[from] = getPieceOnBoard(C_8);
                board64[C_8] = EMPTY;
                board64[A_8] = getPieceOnBoard(D_8);
                board64[D_8] = EMPTY;
                rookFr = A_8;
                rookTo = D_8;
            }
            else if (to == G_8) { // short castles
                board64[from] = getPieceOnBoard(G_8);
                board64[G_8] = EMPTY;
                board64[H_8] = getPieceOnBoard(F_8);
                board64[F_8] = EMPTY;
                rookFr = H_8;
                rookTo = F_8;
            }
        }
       // int move, PieceType piece, int from, int to, int flag
        decrementalUpdate(move, piece, from, to,FLAG_CASTLE); // dec king
        decrementalUpdate(move, getPieceOnBoard(rookFr), rookFr, rookTo, FLAG_CASTLE);
    }

    private void makeMove(int from, int to, PieceType p) {
        board64[from] = EMPTY;
        board64[to] = p;
    }

    //
    private void decrementalUpdate(int move, PieceType piece, int from, int to, int flag) {
        if (piece == null) throw new IllegalArgumentException("decremental updated invoked with null piece");
        int floor = getPieceListFloor(piece);
        int ceil = getPieceListCeiling(piece);
        boolean sideToPlay = piece.isWhite();
        int side[] = (sideToPlay) ? whitePieceList : blackPieceList;
        int xside[] = (sideToPlay) ? blackPieceList : whitePieceList;
        int promotedPiece = getPromotionPiece(move);
        PieceType capturedPiece = getPieceType(getCapturedPiece(move));

        // update opponent list
        if (capturedPiece != EMPTY) {
            int start = getPieceListFloor(capturedPiece);
            int end = getPieceListCeiling(capturedPiece);
            for (; start < end; start++) {
                // put encoded piece in the first empty spot found;
                if (xside[start] == RANK_1) {
                    xside[start] = ((capturedPiece.getValue() << RANK_8) | to);
                    break;
                }
            }
        }
        // update current side list
            for (int index = floor; index < ceil; index++) {
                if (flag != FLAG_PROMOTION) {
                    // encode piece at next available index as long as it is not a promotion
                    int tile = side[index] & 0xff;
                    if (tile == to) {
                        side[index] = ((piece.getValue() << RANK_8) | from); // encode piece
                        break;
                    }
                }
                else {
                    if (side[index] == RANK_1) {
                        side[index] = ((piece.getValue() << RANK_8) | from);
                        break;
                    }
                }

        }
            // second update required for rook
        if (flag == FLAG_CASTLE) { return; }

        // second update required when promotion is available promotion piece and pawn
        if (flag == FLAG_PROMOTION) {
            int start = getPieceListFloor(getPieceType(promotedPiece));
            int end = getPieceListCeiling(getPieceType(promotedPiece));
            for (; start < end; start++) {
                if (side[start] != RANK_1) {
                    int tile = side[start] & 0xff;
                    if (tile == to) {
                        side[start] = RANK_1; // remove from piecelist
                        break;
                    }
                }
            }
        }
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
    public static String getEnpassantString(byte sq) {
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


    public static void main(String[] args) {

        //int rooknum = (WHITE_ROOK.getValue() << 8 | 32);
        //System.out.println(rooknum);
        //int piece  = rooknum >> 8;
        //System.out.println("Piece val " + piece);
        //int square  = rooknum & 0xff;
        //System.out.println("square" + square);
        //System.out.println();
//
        //int[] offset = { -10,  -1,  1, 10, 0,  0,  0,  0 };
        //int mailbox64, mail120;
        //for (int i : offset) {
            //System.out.printf("for %d\n", i);
            //mailbox64 = getMailbox64Number(square);
            //System.out.println("Mail 64 :  " + mailbox64);
            //System.out.println("Mail 64 + off : " + (mailbox64 + i));
            //mail120 = mailbox64 + i;
            //System.out.println("mial 120  " + mail120);
            //System.out.println(getMailbox120Number(mail120));
        //}

        //String[] fens = {FEN_1, FEN_2, FEN_3, FEN_4};
        //Board board;
        //for (String str : fens) {
            //board = FENParser.parseFENotation(str);
            //System.out.println();
            //System.out.println(board);
            //System.out.printf("\n %s", FENParser.getFENotation(board));
            //System.out.println();
        //}
//
        //System.out.println();
        //board = new Board();
        //System.out.println(board.whiteListe);
        //for (int i = 0; i < board.whitePieceList.length; i++) {
            //System.out.printf("%d : \t%d\n", i, board.whitePieceList[i]);
        //}
        //System.out.println(board.whiteListe.size());
        //System.out.println(board.blackListe);
        //System.out.println(Arrays.toString(board.blackPieceList));
        //System.out.println(board.blackListe.size());
//
        Board board;
        board = FENParser.parseFENotation(FEN_CASTLE_RIGHTS_KING_MOVE);
        System.out.println(board);
        // move testing
        System.out.println("MOVE GENERATION TEST");
        System.out.println(FENParser.getFENotation(board));
        int count = 0;
         boolean control = BLACK;
        // boolean control = WHITE;
        Collection<Integer> omelist = King.possibleMoves(board, control);
        //Collection<Integer> somelist = PieceMove.validateMoves(board, (List<Integer>) omelist);
        System.out.println("Moves available: " + omelist.size() + "\n");
        for (int m : omelist) {
            // if (count++ != 9) continue;
            System.out.printf(++count +"\t" + printMove(m) + "\n");
            board.make(m);
            System.out.println(board);
            // System.out.println("ENpanssant " + board.getEnPassant() + "\t" + getEnpassantSquare(board.getEnPassant()));
            System.out.println(
                    "Kingside castle " +
                            ((control) ?  board.canWhiteCastleKingside(board.getCastlingRights()) :
                                    board.canBlackCastleKingside(board.getCastlingRights()))   +
                            "\tqueen side castle " +
                            ((control) ? board.canWhiteCastleQueenside(board.getCastlingRights()) : board.canBlackCastleQueenside(board.getCastlingRights())));

            board.unmake(m);
            System.out.println("\n");
            System.out.println(board);
            System.out.println("\n");
            System.out.println(
                    "Kingside castle " +
                            ((control) ? board.canWhiteCastleKingside(board.getCastlingRights()) : board.canBlackCastleKingside(board.getCastlingRights())) +
                            "\tqueen side castle " +
                            ((control) ? board.canWhiteCastleQueenside(board.getCastlingRights()) : board.canBlackCastleQueenside(board.getCastlingRights()))
            );
        }
    }


}