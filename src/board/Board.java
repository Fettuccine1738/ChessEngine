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
package board;

import piece.Bishop;
import piece.Pawn;
import piece.Queen;
import piece.Rook;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static board.BoardUtilities.*;
import static board.PieceType.*;


public class Board {

    // tests functions
    public  ArrayList<PieceType> blackListe = new ArrayList<>();
    public  ArrayList<PieceType> whiteListe = new ArrayList<>();
    // test strings for FEN Notation
    static String FEN_1 = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    static String FEN_2 = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1";
    static String FEN_3 = "rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq c6 0 2";
    static String FEN_4 = "rnbqkbnr/pp1ppppp/8/2p5/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2";
    static String FEN_5 =  "8/8/8/3K4/8/8/8/8 w - - 0 1"; // single king 8 coordinates test
    static String FEN_6 =  "8/8/8/8/8/8/8/K7 w - - 0 1"; // single king offboard test
    static String FEN_7 =  "8/8/3p4/2p1B3/3b1P2/4P3/8/8 w - - 0 1"; // bishop off board test
    static String FEN_8 =  "8/8/8/3p4/2r1R3/4P3/8/8 w - - 0 1"; // rook move generation test
    static String FEN_9 =  "8/8/8/3p1q2/2Q3P1/4P3/8/8 w - - 0 1"; // queen move generation test
    static String FEN_10 =  "8/8/3p4/2P1p3/8/3P4/4P3/8 w - - 0 1"; // pawn move testing


    // sentinel and blocking piece
    private final static byte OFF_BOARD = -1;
    private final static byte WHITE_KINGSIDE = 1; // 0001
    private final static byte WHITE_QUEENSIDE = 2; // 0010
    private final static byte BLACK_KINGSIDE = 4; // 0100
    private final static byte BLACK_QUEENSIDE = 8; // 1000

    // look up table for offboard move generation;
    private final static int[] MAILBOX_64 = new int[BoardUtilities.BOARD_SIZE]; // maps from 64 to 120
    private final static int[] MAILBOX_120 = new int[BoardUtilities.BOARD_SIZE_120]; // maps from 120 to 64

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
    int captures;

    public Board() {
        board64 = initializeBoard64();
        sideToMove = WHITE;
        fullMoveCounter = 1;
        halfMoveClock = 0;
        enPassant = OFF_BOARD; // modifiable
        castlingRights = encodeCastlingRigts(true, true, true, true);
        whitePieceList = new int[MAX_MAX];
        blackPieceList = new int[MAX_MAX];
        playHistory    = new int[1000];
        captures = 0;
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
        if (pieces == null || pieces.length != 64) throw new IllegalArgumentException("pieces must have 64 elements");
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
        captures = 0;
    }

    public int[] getWhitePieceList() {
        return whitePieceList;
    }

    public int[] getBlackPieceList() {
        return blackPieceList;
    }
    /**
     *
     * @return side to move
     */
    public boolean getSideToMove() {
        return sideToMove;
    }
    /**
     * @param fullMoves assigns field fullMoveCounter to fullMoves
     */
    public void setFullMoveCounter(int fullMoves) {
        this.fullMoveCounter = fullMoves;
    }

    /**
     * @param halfMove assigns field halfMoveClock to halfMove
     */
    public void setHalfMoveClock(int halfMove) {
        this.halfMoveClock = halfMove;
    }

    /**
     * @param rights byte encoding of queen side and kingside rights of black and white sides
     */
    public void setCastlingRights(byte rights) {
        boolean wk = canWhiteCastleKingside(rights);
        boolean wq = canWhiteCastleQueenside(rights);
        boolean bk = canBlackCastleQueenside(rights);
        boolean bq = canBlackCastleQueenside(rights);
        this.castlingRights = encodeCastlingRigts(wk, wq, bk, bq);
    }

    /**
     * @param eP is enPassant possible
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
    public static byte encodeCastlingRigts(boolean wk, boolean wq, boolean bk, boolean bq) {
        byte rights = 0;
        if (wk) rights |= WHITE_KINGSIDE;
        if (wq) rights |= WHITE_QUEENSIDE;
        if (wq) rights |= BLACK_KINGSIDE;
        if (wq) rights |= BLACK_QUEENSIDE;
        return rights;
    }

    // is king side castling right for white available
    public static boolean canWhiteCastleKingside(byte rights) {
        return (rights & WHITE_KINGSIDE) != 0;
    }

    // is queenside castling right for white available
    public static boolean canWhiteCastleQueenside(byte rights) {
        return (rights & WHITE_QUEENSIDE) != 0;
    }

    // is king side castling right for black available
    public static boolean canBlackCastleKingside(byte rights) {
        return (rights & BLACK_KINGSIDE) != 0;
    }

    public static boolean canBlackCastleQueenside(byte rights) {
        return (rights & BLACK_QUEENSIDE) != 0;
    }

    /**
     * @param index index
     * @throws IllegalArgumentException if index is not between 0 and 63.
     * @return piecetype enum on that index/ tile.
     */
    public PieceType getPieceOnBoard(int index) {
        if (index < 0 || index >= BoardUtilities.BOARD_SIZE) {
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


    private void make(Move move) {
        if (move == null) throw new IllegalArgumentException("moves must not be null");
        switch (move.getMoveType()) {
            case NORMAL -> {
                board64[move.getFrom()] = EMPTY;
                board64[move.getTo()]   = move.getPieceType();
                System.out.println(move.getPieceType() + " moves to empty " + move.getFrom() + " to " + move.getTo());
                if (Math.abs(move.getPieceType().getValue()) == 1) {
                    // record en Passant for double push
                    if (Math.abs(move.getFrom() - move.getTo()) == 16)
                        setEnPassant((byte) (move.getTo() - RANK_8));
                    else
                        setEnPassant(OFF_BOARD);
                }
            }
            case CAPTURE -> updatePieceLists(move, move.getSideToPlay());
        }
    }

    private void updatePieceLists(Move move, boolean sideToPlay) {
        int[] side = (sideToPlay) ? whitePieceList : blackPieceList;
        int[] xside = (sideToPlay) ? blackPieceList : whitePieceList;
        if (move.getMoveType() != MoveType.CAPTURE) return;
        // what is the captured piece
        PieceType pieceCaptured = getPieceOnBoard(move.getTo());
        assert(pieceCaptured != EMPTY);
        assert(pieceCaptured.isWhite() != sideToPlay);
        // capture
        board64[move.getFrom()] = EMPTY;
        board64[move.getTo()]   = move.getPieceType();
        // update captures
        playHistory[captures++] = (pieceCaptured.getValue() << 8 | move.getTo());
        System.out.println(move.getPieceType().getName() + " Captures " + pieceCaptured.getName() +
                           " on " + move.getTo());

        int floor   = getPieceListFloor(pieceCaptured);
        int ceiling = getPieceListSize(pieceCaptured);
        int encoding, square;
        int xencoding, xsquare;

        // update white and black's list
        for (int index = floor; index < ceiling; index++) {
            if (sideToPlay) { // if white update black;s lists
                if (blackPieceList[index] != 0) {
                    encoding = blackPieceList[index] >> 8;
                    square   = blackPieceList[index] & 0xff;
                    if (square == move.getTo()) {
                        blackPieceList[index] = 0;
                        break;
                    }
                }
            }
            else { // update white's lists
                if (whitePieceList[index] != 0) {
                    encoding = whitePieceList[index] >> 8;
                    square   = whitePieceList[index] & 0xff;
                    if (square == move.getTo()) {
                        whitePieceList[index] = 0;
                        break;
                    }
                }
            }
        }

    }


    private void unmake(Move move) {
        if (move == null) throw new IllegalArgumentException("moves must not be null");
        switch (move.getMoveType()) {
            case NULLMOVE ->
                    System.out.println(move.getPieceType() + " undo move " + move.getTo() + " to " + move.getFrom());
            case NORMAL -> {
                    board64[move.getTo()] = EMPTY;
                    board64[move.getFrom()]   = move.getPieceType();
                    System.out.println(move.getPieceType() + " undo move " + move.getTo() + " to " + move.getFrom());
            }
            case CAPTURE -> unmakeHistory(move);
        }
    }

    private void unmakeHistory(Move move) {
        boolean side = move.getSideToPlay();
        int from = move.getFrom();
        int to = move.getTo();
        // return piece back to from
        if (board64[to] != move.getPieceType()) throw new IllegalArgumentException("Piece is not on destination");
        board64[to] = EMPTY;
        board64[from] = move.getPieceType();

        // get most recent capture
        int captured = playHistory[--captures];
        int type = captured >> 8;
        int square = captured & 0xff;
        PieceType piece = PieceType.getPieceType(type);
        // put piece on board
        board64[to] = piece;
        // update piecelists
        int floor, ceiling, index;
        floor = getPieceListFloor(piece);
        ceiling = getPieceListSize(piece);
        for (index = floor; index < ceiling; index++) {
            if (side) { // white capture : update black's piece lists
                if (blackPieceList[index] == 0) blackPieceList[index] = captured;
                break;
            } else { // black capture : update white's piece lists
                if (whitePieceList[index] == 0) whitePieceList[index] = captured;
                break;
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

        for (int i = 0; i < BoardUtilities.RANK_8; i++) {
            for (int j = 0; j < BoardUtilities.FILE_H; j++) {
                index = mapIndex64To120(i, j);
                MAILBOX_64[i * BoardUtilities.FILE_H + j] = index;
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
        int max = getPieceListSize(piece);
        for (int square = 0, j = index; square < BOARD_SIZE && j <= max; square++) {
            if (board64[square] == piece) {
                // encode both square and piece on the square
                pieceList[j] = ((piece.getValue() << 8) | square);
                j++;
                if (piece == BLACK_KING && blackListe.contains(piece)) {
                    System.out.println();
                    System.out.println("Error " + square + " " + piece + "black king" + j);
                }
                if (piece.getValue() < 0) blackListe.add(piece);
                else whiteListe.add(piece);
            }
        }
    }

    // maps index in an 8 x8 board to 10 x 12 board
    private static int mapIndex64To120(int rank, int file) {
        return (rank * 10) + 21 + file;
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
        board = FENParser.parseFENotation(FEN_10);
        System.out.println(board);
        // move testing
        System.out.println("MOVE GENERATION TEST");
        System.out.println(FENParser.getFENotation(board));
        //int count = 0;
        for (Move m : Pawn.possibleMoves(board, WHITE)) {
            // if (count++ != 9) continue;
            board.make(m);
            System.out.println(board);
            System.out.println("ENpanssant " + board.getEnPassant());
            board.unmake(m);
            System.out.println("\n");
            System.out.println(board);
            System.out.println("\n");
        }
    }


}