package board;

import java.util.Arrays;

import static board.Board.*;
import static board.BoardUtilities.*;
import static board.PieceType.EMPTY;

public class FENParser {

    /**
     * @param board board to scan for piece and corresponding position
     * @return a string that is the Forsyth Edward Notation of the board
     */
    public static String getFENotation(Board board) {
        StringBuilder notation;
        int emptyCount = 0; // counts empty cells
        PieceType piece;
        notation = new StringBuilder();
        // start from black side
        for (int i = BoardUtilities.RANK_8; i > BoardUtilities.RANK_1; i--) {
            emptyCount = 0;
            for (int j = 0; j < BoardUtilities.FILE_H; j++) {
                piece = board.getPieceOnBoard((i -  1) * BoardUtilities.FILE_H + j);
                if (piece == EMPTY) emptyCount++;
                else {
                    if (emptyCount > 0) notation.append(emptyCount);
                    notation.append(piece.getName());
                    emptyCount = 0;
                }
            }
            if (emptyCount > 0) notation.append(emptyCount);
            if (i > 1) notation.append('/');
        }
        notation.append(' ');
        notation.append(getSideToMove(board)).append(' ');
        // castling ability
        notation.append(getCastlingRightsFENotation(board));
        notation.append(' ');
        notation.append(getEnPassantFENotation(board));
        notation.append(' ').append(board.getHalfMoveClock());
        notation.append(' ').append(board.getFullMoveCounter());
        return notation.toString();
    }


    private static String getCastlingRightsFENotation(Board board) {
        if (board == null) throw new IllegalArgumentException("Enpassant invoked with null board");
        StringBuilder notation = new StringBuilder();

        boolean castlingRigts = false;
        byte  castle = board.getCastlingRights();
        if (canWhiteCastleKingside(castle)) {
            notation.append('K');
            castlingRigts = true;
        }
        if (canWhiteCastleQueenside(castle)) {
            notation.append('Q');
            castlingRigts = true;
        }
        if (canBlackCastleKingside(castle)) {
            notation.append('k');
            castlingRigts = true;
        }
        if (canBlackCastleQueenside(castle)) {
            notation.append('q');
            castlingRigts = true;
        }
        if (!castlingRigts) notation.append('-');
        return notation.toString();
    }


    private static String getEnPassantFENotation(Board board) {
        if (board == null) throw new IllegalArgumentException("Enpassant invoked with null board");
        StringBuilder notation = new StringBuilder();
        byte enPassant = board.getEnPassant();
        if (enPassant == OFF_BOARD) return notation.append('-').toString();
        int rank = enPassant >> 3; // divide by 8
        int file = enPassant  & 7; // modulo 8
        if (rank == 0 && file == 0) { // no en passant available
            notation.append('-');
            return notation.toString();
        }
        // convert file to a - h
        char chFile = (char) ('a' + rank);
        notation.append(chFile).append(file);
        return notation.toString();
    }


    private static char getSideToMove(Board board) {
        if (board == null) throw new IllegalArgumentException("Side to move invoked with null board");
        boolean fullCounts = board.getSideToMove();
        if (fullCounts) return 'w';
        else return 'b';
    }

    /**
     * @param fenotation FEN Notation of a chess position
     * @return Board instance of exact position from a FEN string
     */
    public static Board parseFENotation(String fenotation) {
        if (fenotation == null) throw new IllegalArgumentException("Null string in FEN" + fenotation);
        PieceType[] pieces;
        int halfMoveClock, fullMoveCounter;
        byte enPassant, castlingRights;
        boolean side; // side to move
        // fill pieces with empty pieces
        pieces = new PieceType[64];
        Arrays.fill(pieces, EMPTY);
        String[] tokens = fenotation.split("/");
        // all 8 ranks of the board must be present
        if (tokens.length != BoardUtilities.RANK_8) throw new IllegalArgumentException();
        char ch;
        int index = 0;
        int N = BoardUtilities.RANK_8 - 1;
        // loop through first 7 ranks / string representation starting from top
        for (int i = N; i > 0; i--) {
            index = N - i;
            parseRankandFile(tokens[index], pieces, i);
        }
        // parse last token of FEN string
        String[] lastToken = tokens[BoardUtilities.RANK_8 - 1].split(" ");
        if (lastToken.length != 6) throw new IllegalArgumentException("Last row invalid in FEN" + lastToken.length);
        // parse first rank of the board
        parseRankandFile(lastToken[0], pieces, RANK_1);
        // side to move not required
        side             = parseSideToMove(lastToken[1]);
        castlingRights   = parseCastlingRights(lastToken[2]);
        enPassant        = parseEnPassant(lastToken[3]);
        halfMoveClock    = parseHalfMoveClock(lastToken[4]);
        fullMoveCounter  = parseFullMoveCounter(lastToken[5]);
        return new Board(pieces, side, fullMoveCounter, halfMoveClock, castlingRights, enPassant);
    }

    // parse string tokens of each rank
    private static void parseRankandFile(String rankAndFile, PieceType[] pieceTypes, int index) {
        if (rankAndFile == null || pieceTypes == null)
            throw new IllegalArgumentException("Null rank and file notation");
        if (index < RANK_1 || index >= RANK_8) throw new IllegalArgumentException("Invalid rank: " + index);
        int file = 0;
        for (char ch : rankAndFile.toCharArray()) {
            if (Character.isDigit(ch)) {
                file += ch - '0'; // advance to next file, square already filled with EMPTY
            } else {
                PieceType piece = PieceType.getPieceType(ch);
                pieceTypes[index * BoardUtilities.FILE_H + file] = piece;
                file++;
            }
        }
    }

    private static byte parseCastlingRights(String rights) {
        if (rights == null) throw new IllegalArgumentException("Null rights");
        byte encodeRights = 0;
        for (char ch : rights.toCharArray()) {
            switch (ch) {
                case '-':  return 0; // no castling rights
                case 'K':  encodeRights |= WHITE_KINGSIDE;  break;
                case 'Q':  encodeRights |= WHITE_QUEENSIDE; break;
                case 'k':  encodeRights |= BLACK_KINGSIDE;  break;
                case 'q':  encodeRights |= BLACK_QUEENSIDE; break;
                default: throw new IllegalArgumentException("Invalid castling right " + ch);
            }
        }
        return encodeRights;
    }

    private static boolean parseSideToMove(String sideToMove) {
        if (sideToMove.equals("w")) return WHITE;
        else if (sideToMove.equals("b")) return BLACK;
        else throw new IllegalArgumentException("Invalid side " + sideToMove);
    }

    private static byte parseEnPassant(String enPassant) {
        if (enPassant == null) throw new IllegalArgumentException("Null enPassant");
        byte encodeEnPass = 0;
        if (enPassant.equals("-")) return OFF_BOARD; // no enpassant available
        int rank = enPassant.charAt(0) - 'a';
        int file = Integer.parseInt(enPassant.charAt(1) + "");
        return ((byte) (rank * BoardUtilities.FILE_H + file));
    }

    private static int parseHalfMoveClock(String halfMoveClock) {
        if (halfMoveClock == null) throw new IllegalArgumentException("halfMoveClock is null");
        return Integer.parseInt(halfMoveClock);
    }

    private static int parseFullMoveCounter(String fullMove) {
        if (fullMove == null) throw new IllegalArgumentException("halfMoveClock is null");
        return Integer.parseInt(fullMove);
    }

}
