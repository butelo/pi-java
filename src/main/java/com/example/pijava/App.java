///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.7.6
//DEPS com.googlecode.lanterna:lanterna:3.1.1
//DEPS com.google.code.gson:gson:2.11.0
//DEPS ch.qos.logback:logback-classic:1.5.12
//JAVA 17+

package com.example.pijava;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.*;
import com.googlecode.lanterna.screen.*;
import com.googlecode.lanterna.terminal.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
    name = "pi-java",
    mixinStandardHelpOptions = true,
    version = "pi-java 1.0",
    description = "A TUI code agent in Java"
)
public class App implements Callable<Integer> {

    @Option(names = {"-v", "--verbose"}, description = "Enable verbose output")
    private boolean verbose;

    @Option(names = {"-p", "--port"}, description = "Port to listen on")
    private int port = 8080;

    @Override
    public Integer call() throws Exception {
        startTUI();
        return 0;
    }

    private void startTUI() throws IOException {
        Terminal terminal = new DefaultTerminalFactory().createTerminal();
        Screen screen = new TerminalScreen(terminal);
        screen.startScreen();

        List<String> messages = new ArrayList<>();
        StringBuilder inputBuffer = new StringBuilder();

        while (true) {
            // Clear screen
            screen.clear();
            TextGraphics tg = screen.newTextGraphics();

            TerminalSize size = screen.getTerminalSize();
            int width = size.getColumns();
            int height = size.getRows();

            // Draw header
            String header = "=== pi-java - AI Code Assistant (Echo) ===";
            int headerPad = (width - header.length()) / 2;
            if (headerPad > 0) {
                header = " ".repeat(headerPad) + header;
            }
            tg.setForegroundColor(TextColor.ANSI.GREEN);
            tg.putString(0, 0, header);
            
            tg.setForegroundColor(TextColor.ANSI.CYAN);
            tg.putString(0, 1, "Enter message below (ESC to quit)");

            // Draw separator
            tg.setForegroundColor(TextColor.ANSI.DEFAULT);
            tg.putString(0, 2, "=".repeat(width));

            // Draw messages (last 15 lines)
            int msgStart = 4;
            int maxMessages = height - 8;
            int startIdx = Math.max(0, messages.size() - maxMessages);
            
            for (int i = startIdx; i < messages.size(); i++) {
                String msg = messages.get(i);
                TextColor color = msg.startsWith(">") ? TextColor.ANSI.BLUE : TextColor.ANSI.YELLOW;
                tg.setForegroundColor(color);
                tg.putString(0, msgStart + (i - startIdx), msg);
            }

            // Draw input area
            int inputY = height - 3;
            tg.setForegroundColor(TextColor.ANSI.DEFAULT);
            tg.putString(0, inputY - 1, "-".repeat(width));
            tg.setForegroundColor(TextColor.ANSI.GREEN);
            tg.putString(0, inputY, "> ");
            tg.setForegroundColor(TextColor.ANSI.DEFAULT);
            tg.putString(2, inputY, inputBuffer.toString());

            // Draw cursor
            screen.setCursorPosition(new TerminalPosition(2 + inputBuffer.length(), inputY));

            // Draw status
            tg.setForegroundColor(TextColor.ANSI.CYAN);
            tg.putString(0, height - 1, "ESC=quit | Enter=send");

            screen.refresh();

            // Read input
            KeyStroke key = screen.readInput();
            if (key == null) continue;

            if (key.getKeyType() == KeyType.Escape) {
                break;
            } else if (key.getKeyType() == KeyType.Enter) {
                if (inputBuffer.length() > 0) {
                    String msg = inputBuffer.toString();
                    messages.add("> " + msg);
                    messages.add("< " + msg); // Echo
                    inputBuffer.setLength(0);
                }
            } else if (key.getKeyType() == KeyType.Backspace) {
                if (inputBuffer.length() > 0) {
                    inputBuffer.deleteCharAt(inputBuffer.length() - 1);
                }
            } else if (key.getKeyType() == KeyType.Character) {
                char c = key.getCharacter();
                if (c >= 32 && c < 127) {
                    inputBuffer.append(c);
                }
            }
        }

        screen.stopScreen();
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new App()).execute(args);
        System.exit(exitCode);
    }
}
