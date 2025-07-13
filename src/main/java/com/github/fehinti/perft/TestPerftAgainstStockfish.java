package com.github.fehinti.perft;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestPerftAgainstStockfish {

  static final File LOG = new  File("src/main/java/com/github/fehinti/perft/perft_log.txt");
  static final String NON_MATCHES = "src/main/java/com/github/fehinti/perft/dummy.txt";
  static final String TEST ="src/main/java/com/github/fehinti/perft/fenway.txt";
  static final String STOCKFISH = "Stockfish";
  static final int DEPTH = 6;
  static final String S_DEPTH = "go perft " + DEPTH; // generate moves up to depth

  static final ProcessBuilder processBuilder = new ProcessBuilder(STOCKFISH)
          .directory(new File(System.getProperty("user.dir"))); // pwd
  static final Process process;
  static final BufferedWriter bw;
  static final BufferedWriter nmatch;
  static final BufferedReader br;
  static final BufferedReader stockfishReader;

   static {
       processBuilder.redirectErrorStream(true);
        try {
            process = processBuilder.start();
            bw = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            stockfishReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            br = Files.newBufferedReader(Paths.get(TEST));
            nmatch = Files.newBufferedWriter(Paths.get(NON_MATCHES));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
   }

   record StockFishResult(List<String> result, long nodes) {
   }

   // readfrom file, run stockfish and compare against us
   static void compareStockfishVsEngine() {
        // ** boolean[] init = {true}; // ** Java does not allow modification of values in lamdas
        // ** since the reference is final, java allows this
        boolean init = true;
        String line;
        try {
            while ((line = br.readLine()) != null) {
                try {
                    bw.write("position fen " + line);
                    bw.newLine();
                    bw.write(S_DEPTH);
                    bw.newLine();
                    bw.flush();

                    StockFishResult sfish = getStockFishResultFromProcess(init);
                    init = false;
                    if (sfish.nodes == 0) System.out.println("Stockfish could not generate " + line);

                    StockFishResult engine = Perft.getPerftResult(line);

                    // compare results and write erros to file
                    boolean isLineCorrect = compareLines(sfish, engine);
                    nmatch.write(line + ((isLineCorrect) ? "\tGG" : "\tXX"));
                    nmatch.newLine();
                    nmatch.flush();
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        cleanUpResources();
    }

   static StockFishResult getStockFishResultFromProcess(Boolean init) throws IOException {
       // read from input
       List<String> result = new ArrayList<>();
       String line;
       long num = 0;
       // discard stockfish process info
       for (int m = 0; m < 5; m++) stockfishReader.readLine(); // first 5 lines not needed

      while((line = stockfishReader.readLine()) != null) {
          if (line.startsWith("Nodes")) {
              String[] split = line.trim().split("\\s+");
              num = Long.parseLong(split[split.length - 1]);
              break;
          }
          result.add(line);
      }

      result.removeIf(String::isEmpty);
      Collections.sort(result);

      return new StockFishResult(result, num);
   }

   static boolean compareLines(StockFishResult s1, StockFishResult s2) {
       if (s1.nodes() != s2.nodes()) return false;
       else {
           for (int i = 0; i < s1.result.size(); i++) {
               if (!s1.result.get(i).equalsIgnoreCase(s2.result.get(i))) return false;
           }
           return true;
       }
   }

   static void cleanUpResources() {
       try {
           bw.write("quit");
           bw.newLine();
           bw.flush();
           br.close();
           bw.close();
           stockfishReader.close();
           nmatch.close();
           process.destroy();
       } catch (IOException e) {
           System.out.println(e.getMessage());
       }
   }

    public static void main(String[] args) {
       compareStockfishVsEngine();
    }
}
