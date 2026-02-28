///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.7.6
//DEPS com.googlecode.lanterna:lanterna:3.1.1
//DEPS com.google.code.gson:gson:2.11.0
//DEPS ch.qos.logback:logback-classic:1.5.12
//JAVA 21+

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
import java.time.Instant;
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

    // Java 21: Record for immutable message data
    public record Message(String content, MessageType type, Instant timestamp) {
        public enum MessageType { USER, ASSISTANT }
        
        // Factory methods with text blocks
        public static Message user(String content) {
            return new Message(content, MessageType.USER, Instant.now());
        }
        
        public static Message assistant(String content) {
            return new Message(content, MessageType.ASSISTANT, Instant.now());
        }
    }

    @Override
    public Integer call() throws Exception {
        if (verbose) {
            System.out.println("""
                Starting pi-java in verbose mode on port %d
                """.formatted(port));
        }
        
        startTUI();
        return 0;
    }

    private void startTUI() throws IOException {
        var terminal = new DefaultTerminalFactory().createTerminal();
        var screen = new TerminalScreen(terminal);
        screen.startScreen();

        var messages = new ArrayList<Message>();
        var inputBuffer = new StringBuilder();

        try {
            while (true) {
                screen.clear();
                var tg = screen.newTextGraphics();

                var size = screen.getTerminalSize();
                var width = size.getColumns();
                var height = size.getRows();

                drawHeader(tg, width);
                drawMessages(tg, messages, height);
                drawInputArea(tg, inputBuffer, width, height);
                drawStatus(tg, height);

                screen.setCursorPosition(new TerminalPosition(2 + inputBuffer.length(), height - 3));
                screen.refresh();

                var handled = handleInput(screen.readInput(), inputBuffer, messages);
                if (handled == Action.QUIT) break;
            }
        } finally {
            screen.stopScreen();
        }
    }

    // Java 21: Pattern matching for switch on KeyType
    private Action handleInput(KeyStroke key, StringBuilder inputBuffer, List<Message> messages) {
        if (key == null) return Action.CONTINUE;
        
        return switch (key.getKeyType()) {
            case Escape -> Action.QUIT;
            case Enter -> {
                if (inputBuffer.isEmpty()) yield Action.CONTINUE;
                var msg = inputBuffer.toString();
                messages.add(Message.user(msg));
                messages.add(Message.assistant(msg)); // Echo
                inputBuffer.setLength(0);
                yield Action.CONTINUE;
            }
            case Backspace -> {
                if (!inputBuffer.isEmpty()) {
                    inputBuffer.deleteCharAt(inputBuffer.length() - 1);
                }
                yield Action.CONTINUE;
            }
            case Character -> {
                var c = key.getCharacter();
                if (c >= 32 && c < 127) {
                    inputBuffer.append(c);
                }
                yield Action.CONTINUE;
            }
            default -> Action.CONTINUE;
        };
    }

    private void drawHeader(TextGraphics tg, int width) {
        var header = "=== pi-java - AI Code Assistant (Echo) ===";
        var headerPad = (width - header.length()) / 2;
        if (headerPad > 0) {
            header = " ".repeat(headerPad) + header;
        }
        
        tg.setForegroundColor(TextColor.ANSI.GREEN);
        tg.putString(0, 0, header);
        
        tg.setForegroundColor(TextColor.ANSI.CYAN);
        tg.putString(0, 1, "Enter message below (ESC to quit)");

        tg.setForegroundColor(TextColor.ANSI.DEFAULT);
        tg.putString(0, 2, "=".repeat(width));
    }

    private void drawMessages(TextGraphics tg, List<Message> messages, int height) {
        var msgStart = 4;
        var maxMessages = height - 8;
        var startIdx = Math.max(0, messages.size() - maxMessages);
        
        for (var i = startIdx; i < messages.size(); i++) {
            var msg = messages.get(i);
            var color = msg.type() == Message.MessageType.USER 
                ? TextColor.ANSI.BLUE 
                : TextColor.ANSI.YELLOW;
            
            var prefix = msg.type() == Message.MessageType.USER ? "> " : "< ";
            tg.setForegroundColor(color);
            tg.putString(0, msgStart + (i - startIdx), prefix + msg.content());
        }
    }

    private void drawInputArea(TextGraphics tg, StringBuilder inputBuffer, int width, int height) {
        var inputY = height - 3;
        
        tg.setForegroundColor(TextColor.ANSI.DEFAULT);
        tg.putString(0, inputY - 1, "-".repeat(width));
        
        tg.setForegroundColor(TextColor.ANSI.GREEN);
        tg.putString(0, inputY, "> ");
        
        tg.setForegroundColor(TextColor.ANSI.DEFAULT);
        tg.putString(2, inputY, inputBuffer.toString());
    }

    private void drawStatus(TextGraphics tg, int height) {
        tg.setForegroundColor(TextColor.ANSI.CYAN);
        tg.putString(0, height - 1, "ESC=quit | Enter=send");
    }

    // Java 21: Sealed interface for action results
    private sealed interface Result permits Action {}
    private enum Action implements Result { CONTINUE, QUIT }

    public static void main(String[] args) {
        var exitCode = new CommandLine(new App()).execute(args);
        System.exit(exitCode);
    }
}
