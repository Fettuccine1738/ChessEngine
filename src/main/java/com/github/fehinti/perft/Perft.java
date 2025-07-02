package com.github.fehinti.perft;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.github.fehinti.board.Board120;
import com.github.fehinti.piece.Move;
import com.github.fehinti.board.FENParser;
import com.github.fehinti.piece.MoveGenerator;
import com.github.fehinti.piece.VectorAttack120;

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
   static Board120 board;
  // static String FILEPATH = "C:\\Users\\favya\\IdeaProjects\\ChessEngine\\src\\test\\perft_init.txt";
   static String FILEPATH = "C:\\Users\\favya\\IdeaProjects\\ChessEngine\\src\\main\\java\\com\\github\\fehinti\\perft\\dummy.txt";
   static File file;
   static BufferedWriter bufferedWriter; //  = new BufferedWriter(new FileWriter(file));
   static int COUNT = 0;
   static long CHECKS = 0;
   static long EnP = 0;
   static long castles = 0;
   static long CAPTURE = 0;

   static {
       // comment out when debugging with perftree
       // board = FENParser.parseFENotation120("rnbqkbnr/ppppp1pp/8/8/4p3/8/PPPPKPPP/RNBQ1BNR w kq - 0 3" );
       board = FENParser.startPos120();
       System.out.println(board.print8x8());
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
       List<Integer> moveList = MoveGenerator.generatePseudoLegal(board);

       int N = moveList.size();
       int move, i;

       for(i = 0; i < N; i++) {
           long nodeCount = 0L; // leaf node counts
           move = moveList.get(i);
           board.make(move);
           // System.out.println(board.print8x8() + "\n" + board.getBoardData());
           // writeFENToFile(FENParser.getFENotation(board) + "\t" + (board.lastEntry & 0xff) + "\t"
             // + (board.lastEntry >> 8 & 0xff));
           if (!VectorAttack120.isKingInCheck(board)) {
               int flag = Move.getFlag(move);
               if (currentDepth == 1 && (flag == Move.FLAG_PROMOTION_CAPTURE || flag == Move.FLAG_CAPTURE)) {
                   CAPTURE++;
               }
               if (currentDepth == 1 && flag == Move.FLAG_EN_PASSANT) EnP++;
               else if (currentDepth == 1 && flag == Move.FLAG_CASTLE) castles++;
               // checks to see if move leaves us in check
               boolean side = board.getSideToMove();
               // checks if last move played is a checking move
               if (VectorAttack120.isSquareChecked(board,
                       side,
                       (side) ? board.getWhiteKingSq() : board.getBlackKingSq())
                       && currentDepth == 1) CHECKS++;
               // writeFENToFile(Move.printMove(move) + " :\t" + FENParser.getFENotation(board));
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
       if (args.length != 1) { // adjust length to 2 when debugging with perftree
           System.out.println("Provide a depth please");
       }

       // board = FENParser.sparseFENotation(args[1]); // instatiate board with 2 when debugging with perftree
       int depth = Integer.parseInt(args[0]);
       System.out.println("go perft " + depth);

       Instant st = Instant.now();
       long total = divide(depth, depth);
       System.out.println("TOtal " + total);
       Instant end = Instant.now();
       Duration duration = Duration.between(st, end);

        System.out.println("Duration : " + duration.getSeconds() + " seconds");
        System.out.println( duration.getSeconds() / total  + " seconds");
        System.out.println("captures " + (CAPTURE + EnP));
        System.out.println("Checks: " + CHECKS);
        System.out.println("Enpassant " + EnP);
        System.out.println("Castles: " + castles);
   //     long total = pseudoPerformanceTest(depth);
        closeWriter();
    }
}
