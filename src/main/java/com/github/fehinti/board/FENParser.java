package com.github.fehinti.board;

import java.util.Arrays;

import static com.github.fehinti.board.Board120Utils.*;

public class FENParser {
    
    private static final String START_POS = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    /**
     * @return the starting position on a board, white to play,
     *  both side have long and short castles and no enpassant on the board
     */
    public static Board120 startPos120() {
        return parseFENotation120(START_POS);
    }

    public static String getFENotation(Board120 board) {
        StringBuilder notation = new StringBuilder();
        for (int i = RANK_8; i > 0; i--) {
            int emptyCount = 0;
            for (int j = 0; j < FILE_H; j++) {
                byte piece = board.getPieceOnSquare(Board120.getMailbox64Number((i - 1) * FILE_H + j));
                if (piece == 0) emptyCount++;
                else {
                    if (emptyCount > 0) notation.append(emptyCount);
                    notation.append(Board120.mapByteToChar(piece));
                    emptyCount = 0;
                }
            }
            if (emptyCount > 0) notation.append(emptyCount);
            if (i > 1) notation.append('/');
        }
        notation.append(' ');
        notation.append((board.getSideToMove() ? 'w' : 'b')).append(' ');
        // castling ability
        notation.append(getCastlingRightsFENotation(board));
        notation.append(' ');
        notation.append(getEnPassantFENotation(board));
        notation.append(' ').append(board.getHalfMoveClock());
        notation.append(' ').append(board.getFullMoveCounter());
        return notation.toString();
    }

    /**
     * @param board board to scan for piece and corresponding position
     * @return a string that is the Forsyth Edward Notation of the board
     */
    protected static String getCastlingRightsFENotation(Board120 board) {
        if (board == null) throw new IllegalArgumentException("Enpassant invoked with null board");
        StringBuilder notation = new StringBuilder();
        boolean castlingRigts = false;
        if (board.canWhiteCastleKingside()) {
            notation.append('K');
            castlingRigts = true;
        }
        if (board.canWhiteCastleQueenside()) {
            notation.append('Q');
            castlingRigts = true;
        }
        if (board.canBlackCastleKingside()) {
            notation.append('k');
            castlingRigts = true;
        }
        if (board.canBlackCastleQueenside()) {
            notation.append('q');
            castlingRigts = true;
        }
        if (!castlingRigts) notation.append('-');
        return notation.toString();
    }


    private static String getEnPassantFENotation(Board120 board) {
        if (board == null) throw new IllegalArgumentException("Enpassant invoked with null board");
        StringBuilder notation = new StringBuilder();
        int enPassant = (board.getEnPassant() == OFF_BOARD) ? OFF_BOARD :
                Board120.getMailbox120Number(board.getEnPassant());
        if (enPassant == OFF_BOARD) return "-";
        int rank = enPassant >> 3; // divide by 8
        int file = enPassant  & 7; // modulo 8
        // convert file to a - h
        notation.append((char) (file + 'a')).append(rank + 1);
        return notation.toString();
    }

    /**
     * @param notation FEN Notation of a chess position
     * @return Board120 instance of exact position from a FEN string
     */
    public static Board120 parseFENotation120(String notation) {
        if (notation == null || notation.isEmpty()) throw new IllegalArgumentException("Null string in FEN" + notation);
        byte[] array = new byte[BOARD_SIZE_120];
        int halfMoveClock, fullMoveCounter;
        byte enPassant, castlingRights;
        boolean side;
        Arrays.fill(array, (byte) 0);
        String[] tokens = notation.trim().split("/");
        if (tokens.length != FILE_H) throw new IllegalArgumentException( notation + "\tLast row invalid in FEN");
        char ch;
        int index = 0;
        int N = RANK_8 - 1;
        for (int i = N; i > 0; i--) {
            index = N - i;
            parseRankandFile(tokens[index], array, i);
        }
        // last token of board apsects etc
        String[] lastToken = tokens[RANK_8 - 1].split("\\s+");
        if (lastToken.length != 6) {// valid fen = first row,side, castling, enPassant, fullcount, halfmove
            throw new IllegalArgumentException(notation + "Last row invalid in FEN " + lastToken.length);
        }
        parseRankandFile(lastToken[0], array, 0);
        fillOffBoard(array);
        side = parseSideToMove(lastToken[1]);
        castlingRights   = parseCastlingRights(lastToken[2]);
        byte result = parseEnPassant(lastToken[3]);
        enPassant        =   (result == OFF_BOARD) ? OFF_BOARD : (byte) Board120.getMailbox64Number(result);
        halfMoveClock    = parseHalfMoveClock(lastToken[4]);
        fullMoveCounter  = parseFullMoveCounter(lastToken[5]);
        return new Board120(array, side, fullMoveCounter, halfMoveClock, castlingRights, enPassant);
    }

    private static void fillOffBoard(byte[] array) {
        for (int i = 0; i < 12; i++) {
            for (int j = 0; j < 10; j++) {
                if (Board120.getMailbox120Number(i * 10 + j) == OFF_BOARD) array[i * 10 + j] = (byte) OFF_BOARD;
            }
        }
    }

    private static void parseRankandFile(String fenotation, byte[] array, int index) {
        if (fenotation == null || array == null)
            throw new IllegalArgumentException("Null rank and file notation");
        if (index < EMPT_SQ || index >= RANK_8) throw new IllegalArgumentException("Invalid rank: " + index);
        int file = 0;
        for (char ch : fenotation.toCharArray()) {
            if (Character.isDigit(ch)) {
                file += ch - '0'; // advance to next file, square already filled with EMPTY
            }
            else {
                byte piece = Board120.mapCharToByte(ch);
                array[Board120.getMailbox64Number(index * FILE_H + file)] = piece;
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
        if (enPassant.equals("-")) return OFF_BOARD;
        int file = enPassant.charAt(0) - 'a';
        int rank = Integer.parseInt(enPassant.charAt(1) + "");
        return ((byte) ((rank - 1) * FILE_H + file));
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
