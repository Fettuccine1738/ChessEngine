package com.github.fehinti;

import com.github.fehinti.board.Board120;
import com.github.fehinti.board.FENParser;
import com.github.fehinti.engine.Engine;
import com.github.fehinti.engine.SimpleEvaluator;
import com.github.fehinti.piece.Move;
import com.github.fehinti.piece.MoveGenerator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Main {

    static final Board120 board = FENParser.startPos120();
    static final Engine engine = new Engine(board, SimpleEvaluator.getInstance());
    static final BufferedReader buffReader = new BufferedReader(new InputStreamReader(System.in));
    static final BufferedWriter buffWriter = new BufferedWriter(new OutputStreamWriter(System.out));
    static final  Lock lock = new ReentrantLock();
    static final Condition condition = lock.newCondition();
    static final ArrayList<String> buffer = new ArrayList<>();

    static boolean isGameDrawn = false;
    static boolean noMoveFound = false;

    static void lazyValidate(Board120 board)  throws IOException {
        List<Integer> moves = MoveGenerator.generateLegal(board);
        if (moves.isEmpty()) noMoveFound = true;

        System.out.println("Choose move index from the move List.. ");
        int counter = 0;
        for (int valid : moves) {
            String move = Move.asString(valid);
            System.out.printf("move_no:: %d :: %s\n", counter++, move);
        }
        validate(board, moves);
    }

    static void validate (Board120 board, List<Integer> moves) throws IOException {
        int index;
        String choice =  buffReader.readLine();
        if (choice != null && !choice.isBlank()) {
            index = Integer.parseInt(choice);
            if (index >= 0 && index < moves.size()) {
                board.make(moves.get(index));
                board.display();
            } else {
                System.out.println("Invalid input: ");
                validate(board, moves);
            }
        }
    }

    public static void runConsole() {
        try {
            while (!noMoveFound) {
                System.out.println(FENParser.getFENotation(board));
                lazyValidate(board);
                board.make(engine.think(1));
            }
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }
    }

    public static void takeAction() {
        System.out.println(noMoveFound);
    }

    public static void readBuffer() {
        lock.lock();
        try {
            while (buffer.isEmpty()) {
                condition.awaitUninterruptibly();
            }
            while (buffReader.ready()) {
                buffer.add(buffReader.readLine());
            }
            takeAction();
            condition.signalAll();
        } catch (Exception e) {
            System.out.println(e.getCause());
        }
    }

    public static void writeBuffer() {
        try {
            lock.lock();
            takeAction();
            buffWriter.write(engine.think(1));
            condition.signalAll();
        } catch (IOException ie) {
            System.out.println(ie.getMessage());
        }
    }



    public static void main(String[] args) {
        if (args.length == 1) {
            String argument = args[0];
            new Thread(Main::readBuffer).start();
            new Thread(Main::writeBuffer).start();
        } else {
            runConsole();
        }

    }
}