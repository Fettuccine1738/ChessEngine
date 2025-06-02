package com.github.fehinti.perft;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.github.fehinti.board.Board;
import com.github.fehinti.board.Move;
import com.github.fehinti.board.FENParser;
import com.github.fehinti.piece.PieceMove;
import com.github.fehinti.piece.AttackMap;

/***********************************************************************************
 * Perft, (performance test, move path enumeration)
 * a debugging function to walk the move generation tree of strictly legal moves
 * to count all the leaf nodes of a certain depth, which can be compared to
 * predetermined values and used to isolate bugs. In perft, nodes are only counted
 * at the end after the last makemove. Thus "higher" terminal nodes
 * (e.g. mate or stalemate) are not counted, instead the number of move paths of a
 * certain depth. Perft ignores draws by repetition, by the fifty-move rule and by
 * insufficient material. By recording the amount of time taken for each iteration,
 * it's possible to compare the performance of different move generators or the same
 * generator on different machines, though this must be done with caution since
 * there are variations to perft.
 **********************************************************************************/
public class Perft {
   static long startTime;
   static long endTime;
   static Board board;
  // static String FILEPATH = "C:\\Users\\favya\\IdeaProjects\\ChessEngine\\src\\test\\perft_init.txt";
   static String FILEPATH = "C:\\Users\\favya\\IdeaProjects\\ChessEngine\\src\\main\\java\\com\\github\\fehinti\\perft\\dummy.txt";
   static File file;
   static BufferedWriter bufferedWriter; //  = new BufferedWriter(new FileWriter(file));
   static int COUNT = 0;
   static int CHECKS = 0;
   static int EnP = 0;
   static int castles = 0;

   static {
       //board = FENParser.parseFENotation("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1" ); // comment out when debugging with perftree
       board = FENParser.startPos();
       System.out.println(board.print());
       System.out.println(board.getBoardData());
       file = new File(FILEPATH);

       try {
           bufferedWriter = Files.newBufferedWriter(Paths.get(FILEPATH));
       } catch(IOException e) {
           System.out.println(e.getMessage());
       }
   }

   static void writeFENToFile(String fen) {
       if (bufferedWriter == null) {
           throw new NullPointerException("bufferedWriter is null");
       }
       if (fen == null) throw new IllegalArgumentException("fen is null");
       // bufferedWriter = new BufferedWriter(new OutputStreamWriter(System.out));
       try {
           bufferedWriter.write(fen);
           bufferedWriter.newLine();
           bufferedWriter.flush();
       }
       catch(IOException e) {
           System.out.println(e.getMessage());
       }
   }


   static long divide(int currentDepth, int originalDepth) {
       if (originalDepth > 8) {
           throw new IllegalArgumentException("depth must be between 1 and 8");
       }
       if (currentDepth == 0) { // returns on leaf nodes
           return 1;
       }
       long nodes = 0L;
       List<Integer> moveList = PieceMove.generatePseudoLegal(board);
      // List<Integer> moveList = PieceMove.generateLegalMoves(board, list);
       int N = moveList.size();
       int move, i;

       for(i = 0; i < N; i++) {
           long nodeCount = 0L; // leaf node counts
           move = moveList.get(i);
           board.make(move);
           if (!AttackMap.isKingInCheck(board)) {
               // writeFENToFile(FENParser.getFENotation(board));
               int captures = Move.getCapturedPiece(move);
               if (captures != 0) {
                   if (currentDepth == 1) {
                       COUNT++;
                   }
               }
               int flag = Move.getFlag(move);
               if (currentDepth == 1 && flag == Move.FLAG_EN_PASSANT) EnP++;
               else if (currentDepth == 1 && flag == Move.FLAG_CASTLE) castles++;
               if (AttackMap.isSquareAttacked(board,
                       (board.getSideToMove()) ? board.getWhiteKingSq() : board.getBlackKingSq(),
                        AttackMap.BEFORE) && currentDepth == 1) CHECKS++;

               nodeCount += divide(currentDepth - 1, originalDepth); // advance to child node
               nodes += nodeCount;
           }
           board.unmake(move);
           if (currentDepth == originalDepth) {
               System.out.println(Move.printMove(move) + " :\t" + nodeCount);
               //writeFENToFile(Move.printMove(move) + " :\t" + nodeCount);
           }
       }
       return nodes;
   }

   static void closeWriter() {
       if (bufferedWriter != null) {
           try {
               bufferedWriter.close();
           }
           catch(IOException e) {
               System.out.println(e.getMessage());
           }
       }
   }


    public static void main(String[] args) {
       startTime = System.currentTimeMillis();
       if (args.length != 1) { // adjust length to 2 when debugging with perftree
           System.out.println("Provide a depth please");
       }
       // board = FENParser.parseFENotation(args[1]); // instatiate board with 2 when debugging with perftree
       int depth = Integer.parseInt(args[0]);
       System.out.println("go perft " + depth);
       long total = divide(depth, depth);
       endTime = System.currentTimeMillis();
        System.out.println("EndTime " + endTime +
                "\n" + total + " nodes in " + (endTime - startTime) + "ms");
        System.out.println("captures " + COUNT);
        System.out.println("Checks: " + CHECKS);
        System.out.println("Enpassant " + EnP);
        System.out.println("Castles: " + castles);
       closeWriter();
   //     long total = pseudoPerformanceTest(depth);
    }
}
