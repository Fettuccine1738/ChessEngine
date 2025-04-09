package com.github.fehinti.perft;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Stack;

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
 *
 **********************************************************************************/
public class Perft {
   static long startTime;
   static long endTime;
   static Board board;
   static Stack<String> boardStack = new Stack<>();
  // static String FILEPATH = "C:\\Users\\favya\\IdeaProjects\\ChessEngine\\src\\test\\perft_init.txt";
   static String FILEPATH = "C:\\Users\\favya\\IdeaProjects\\ChessEngine\\src\\main\\java\\com\\github\\fehinti\\perft\\perft_iso2.txt";
   static File file;
   static BufferedWriter bufferedWriter; //  = new BufferedWriter(new FileWriter(file));

   static {
       // modified to debug with perftree
       board = new Board(); // comment out when debugging with perftree
       file = new File(FILEPATH); // exceptions not thrown when opening a file?

       try {
           bufferedWriter = Files.newBufferedWriter(Paths.get(FILEPATH));
       } catch(IOException e) {
           // e.printStackTrace();
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

   // this routine avoids checking legal moves twice, because our
    // program plays a move first to determine legality and undoes the move if it fails
    // the legality test.
   static long pseudoPerformanceTest(int depth) {
       if (depth > 8) {
           throw new IllegalArgumentException("depth must be between 1 and 8");
       }
       if (depth == 0) return 1; // leaf node reached
       List<Integer> moveList = PieceMove.pseudoLegal(board);
       long nodes = 0L;

       for (int move : moveList) {
           // long nodeCount = 0L;
           // avoid traveling down an Illegal node
           board.make(move);
           if (!AttackMap.isKingInCheck(board)) {
               nodes += pseudoPerformanceTest(depth - 1);
           }
           board.unmake(move);
       }
       return nodes;
   }

   static long divide(int currentDepth, int originalDepth) {
       if (originalDepth > 8) {
           throw new IllegalArgumentException("depth must be between 1 and 8");
       }
        // if (currentDepth == 1) {return size } ???
       if (currentDepth == 0) { // returns on leaf nodes
           return 1;
       }
       long nodes = 0L;
       // board.alternateSide();
       List<Integer> moveList = PieceMove.pseudoLegal(board);
       int N = moveList.size();
       int move, i;
       // generate legal moves
       for(i = 0; i < N; i++) {
           long nodeCount = 0L; // leaf node counts
           move = moveList.get(i);
           board.make(move);

           if (currentDepth == originalDepth - 2)  writeFENToFile(FENParser.getFENotation(board));

           // boardStack.push(FENParser.getFENotation(board));
           //System.out.println(board);
           //// System.out.println(nodeCount);
           //System.out.println(Move.printMove(move) + ":\t" + (board.getSideToMove() ? "WHITE" : "BLACK") + " to play"+
           //"\nEnPassant: \t"+ Board.getEnpassantString(board.getEnPassant()) +
                   //"\nCastling Rights: \t" + board.getCastlingRights()
            //+ "\nFull Move Counter\t" + board.getFullMoveCounter()
           //+ "\nHalf Move \t" + board.getHalfMoveClock());
           if (!AttackMap.isKingInCheck(board)) {
               // board.alternateSide();
               nodeCount += divide(currentDepth - 1, originalDepth); // advance to child node
               nodes += nodeCount;
           }
          // nodes += nodeCount;
           if (currentDepth == originalDepth) {
               System.out.println(Move.printMove(move) + " :\t" + nodeCount);
               writeFENToFile(Move.printMove(move) + " :\t" + nodeCount + "\n");
           }
           board.unmake(moveList.get(i));
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
       closeWriter();
   //     long total = pseudoPerformanceTest(depth);
       endTime = System.currentTimeMillis();
       System.out.println("EndTime " + endTime +
               "\n" + total + " nodes in " + (endTime - startTime) + "ms");
    }
}
