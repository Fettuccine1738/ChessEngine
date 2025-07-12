package com.github.fehinti;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Main {

    record StockFishResult(String pos, List<String> result, int nodes) {
        int getNodes() {
            return nodes;
        }
        List<String> getResult() {
            return result;
        }
    }

    static void printResult(StockFishResult s) {
        System.out.println("Total Nodes searched " + s.nodes);
        for (String string : s.getResult()) {
            System.out.println(string);
        }
    }

    public static void main(String[] args) {
        ProcessBuilder pb = new ProcessBuilder("Stockfish");
        pb.directory(new File(System.getProperty("user.dir")));
        pb.redirectErrorStream(true);
        //pb.redirectOutput(ProcessBuilder.Redirect.appendTo(log));
        // File log = new File("src/main/java/com/github/fehinti/sample.txt");


        try {
            Process p = pb.start();

            // write to stream
            BufferedWriter bw  = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
            List<String> input = List.of("position startpos", "go perft 2");
            int i = 0;
            while (i < input.size()) {
                bw.write(input.get(i));
                bw.newLine();
                i++;
            }
            bw.flush();
            // read from stream
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            for (int m = 0; m < 5; m++) br.readLine(); // first 5 lines not needed
            List<String> result = new ArrayList<>();
            String line;
            int num = 0;

            while ((line = br.readLine()) != null) {
                if (line.startsWith("Nodes")) {
                    String[] split = line.trim().split("\\s+");
                    num = Integer.parseInt(split[split.length - 1]);
                    bw.write("quit");
                    bw.newLine();
                    bw.flush();
                    continue;
                }
                result.add(line);
            }
            Collections.sort(result);
            // cleanup
            br.close();
            bw.close();
            p.destroy();

            StockFishResult sfr = new StockFishResult(input.getFirst(), result, num);
            printResult(sfr);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

    }
}