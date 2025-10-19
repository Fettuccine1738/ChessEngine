package com.github.fehinti;

import com.github.fehinti.engine.Engine;
import com.github.fehinti.engine.SimpleEvaluator;
import com.github.fehinti.piece.Move;
import org.slf4j.Logger;

// import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ArenaGUIMain {
    enum GameState {
        UNINITIALIZED, INITIALIZED, GAME_START, THINKING, GAME_END
    }

    static final Logger classLogger =  LogManager.getClassLogger(ArenaGUIMain.class);
    static Engine engine = null;
    static final InputStreamReader ioReader = new InputStreamReader(System.in);
    // test
    // static final BufferedReader buffReader = new BufferedReader(ioReader);
    static final BufferedWriter buffWriter = new BufferedWriter(new OutputStreamWriter(System.out));
    static final Lock lock = new ReentrantLock();
    static final Condition condition = lock.newCondition();
    static final ArrayList<String> buffer = new ArrayList<>();

    static GameState gameState = GameState.UNINITIALIZED;
    static boolean debugMode = false;
    static boolean noMoveFound = false;

    public static void takeAction() {
        System.out.println(noMoveFound);
    }

    public static String readAllBytes() throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read;
        while ((read = ioReader.read()) != -1) {
            byteArrayOutputStream.write(buffer, 0, read);
        }
        return byteArrayOutputStream.toString();
    }

    private static void parseGUICommand(String command) {
        String[] comms = command.split(" ");
        for (short i = 0; i < comms.length; i++) {
            String comm = comms[i];
            if (comm.equalsIgnoreCase("isready")) {
            } else if (comm.equalsIgnoreCase("setoption name")) {
            } else if (comm.equalsIgnoreCase("setoption value")) {

            } else if (comm.equalsIgnoreCase("register")) {

            } else if (comm.equalsIgnoreCase("ucinewgame")) {
                if (comms[++i].equalsIgnoreCase("")) {}

            } else if (comm.equalsIgnoreCase("position")) {
                if (comms[i + 1].equalsIgnoreCase("startpos")) {
                    engine = new Engine(SimpleEvaluator.getInstance());
                } else {
                    engine = new Engine(comms[i + 1], SimpleEvaluator.getInstance());
                }
                // send some success message
                i++;
                gameState = GameState.INITIALIZED;
            } else if (comm.equalsIgnoreCase("go")) {
                gameState = GameState.GAME_START;
            }
        }
        System.out.println();
    }

    public static void readUCIBuffer() {
        lock.lock();
        try {
            while (buffer.isEmpty()) {
                condition.awaitUninterruptibly();
            }
            parseGUICommand(readAllBytes());
            condition.signalAll();
        } catch (Exception e) {
            classLogger.error(String.valueOf(e.getCause()));
        }
    }

    public static void reply() {

    }

    public static void writeUCIBuffer() {
        try {
            lock.lock();
            takeAction();
            int move = engine.think(1);
            String mvString = Move.asString(move);
            buffWriter.write(mvString);
            condition.signalAll();
        } catch (IOException ie) {
            classLogger.error(ie.getMessage());
        }
    }

    public static void main(String[] args) {
        new Thread(ArenaGUIMain::readUCIBuffer).start();
        new Thread(ArenaGUIMain::writeUCIBuffer).start();
    }
}
