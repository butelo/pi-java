package com.example.pijava.ui.screen;

import com.example.pijava.agent.AgentLoop;
import com.example.pijava.model.Message;
import com.example.pijava.ui.component.*;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.InfoCmp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The main application screen.
 */
public class MainScreen {

    // Key codes for input handling
    private static final int KEY_ESC = 27;
    private static final int KEY_CTRL_C = 3;
    private static final int KEY_ENTER = 13;
    private static final int KEY_LF = 10;
    private static final int KEY_BACKSPACE = 127;
    private static final int KEY_BACKSPACE_ALT = 8;
    private static final int KEY_CTRL_U = 21;
    private static final int KEY_PRINTABLE_MIN = 32;
    private static final int KEY_PRINTABLE_MAX = 126;

    // Escape sequence parsing
    private static final int ESC_SEQ_BRACKET = 91;
    private static final int ESC_SEQ_UP_ARROW = 65;
    private static final int ESC_SEQ_DOWN_ARROW = 66;
    private static final int ESC_SEQ_LT = 60;

    private final List<Message> messages = new ArrayList<>();
    private final AgentLoop agentLoop;
    private final HeaderComponent header;
    private final StatusBarComponent statusBar;
    private MessageListComponent messageList;

    // Throttle rapid scroll events (e.g., from touchpads)
    private long lastScrollTime = 0;
    private static final long SCROLL_THROTTLE_MS = 16; // ~60fps
    // Mouse button codes for SGR mouse encoding
    private static final int MOUSE_BUTTON_SCROLL_UP = 64;
    private static final int MOUSE_BUTTON_SCROLL_DOWN = 65;

    public MainScreen(AgentLoop agentLoop) {
        this.agentLoop = agentLoop;
        String mode = agentLoop != null ? "LLM" : "Echo";
        this.header = new HeaderComponent(
            "=== pi-java - AI Code Assistant (" + mode + ") ===",
            "Enter message below (ESC to quit)"
        );
        this.statusBar = new StatusBarComponent("↑↓/wheel=scroll | ESC=quit | Enter=send | Shift+drag to select text");
        if (agentLoop == null) {
            messages.add(Message.assistant(
                "No API key. Running in echo mode. "
                + "Set OPENAI_API_KEY or use --api-key to enable the agent."));
        }
        this.messageList = new MessageListComponent(messages);
    }

    public void run() throws IOException {
        Terminal terminal = TerminalBuilder.builder()
            .system(true)
            .jna(true)
            .build();
        
        try {
            terminal.enterRawMode();
            // Enable mouse tracking for scroll wheel support
            // Note: To select text, hold Shift while dragging (works in most terminals)
            terminal.writer().write("\033[?1000h");  // Basic mouse tracking
            terminal.writer().write("\033[?1002h");  // Button-event tracking
            terminal.writer().write("\033[?1006h");  // SGR extended coordinates
            terminal.writer().flush();
            loop(terminal);
        } finally {
            // Disable mouse tracking on exit
            terminal.writer().write("\033[?1006l");
            terminal.writer().write("\033[?1002l");
            terminal.writer().write("\033[?1000l");
            terminal.writer().flush();
            terminal.close();
        }
    }

    private void loop(Terminal terminal) throws IOException {
        StringBuilder inputLine = new StringBuilder();
        var reader = terminal.reader();

        try {
            while (true) {
                render(terminal, inputLine.toString());

                // Read one byte - blocking
                int key = reader.read();

                // Check for escape sequence (arrow keys, mouse, etc.) FIRST
                // Arrow keys send ESC [ A/B/C/D sequences
                // Mouse wheel sends ESC [<64;x;yM (scroll down) or ESC[<65;x;yM (scroll up)
                // We need to peek with a small timeout to distinguish ESC key from escape sequences
                if (key == KEY_ESC) {
                    // Wait up to 50ms for next byte - if nothing, it's a standalone ESC
                    int b2 = reader.peek(50);
                    if (b2 >= 0) {
                        b2 = reader.read();
                        if (b2 == ESC_SEQ_BRACKET) { // '['
                            int b3 = reader.read();
                            if (b3 == ESC_SEQ_UP_ARROW) { // Up Arrow
                                messageList.scrollUp(1);
                                continue;
                            }
                            if (b3 == ESC_SEQ_DOWN_ARROW) { // Down Arrow
                                messageList.scrollDown(1);
                                continue;
                            }
                            // Check for mouse SGR format: ESC [<...
                            if (b3 == ESC_SEQ_LT) { // '<' - SGR mouse event
                                handleMouseEvent(reader, true);  // true = we already consumed '<'
                                continue;
                            }
                        }
                        // Unknown sequence, ignore and continue
                        continue;
                    }
                    // No follow-up byte within timeout - it's a standalone ESC, quit
                    break;
                }

                // Ctrl-C: quit
                if (key == KEY_CTRL_C) {
                    break;
                }

                // Enter: send message
                if (key == KEY_ENTER || key == KEY_LF) {
                    if (inputLine.length() > 0) {
                        String text = inputLine.toString();
                        inputLine.setLength(0);
                        messages.add(Message.user(text));
                        messageList.scrollToBottom();

                        if (agentLoop != null) {
                            statusBar.setText("Thinking...");
                            render(terminal, "");
                            try {
                                String reply = agentLoop.process(text);
                                messages.add(Message.assistant(reply));
                                messageList.scrollToBottom();
                            } catch (Exception e) {
                                messages.add(Message.assistant("Error: " + e.getMessage()));
                                messageList.scrollToBottom();
                            }
                            statusBar.setText("↑↓/wheel=scroll | ESC=quit | Enter=send");
                        } else {
                            messages.add(Message.assistant(text));
                            messageList.scrollToBottom();
                        }
                    }
                    continue;
                }

                // Backspace
                if (key == KEY_BACKSPACE || key == KEY_BACKSPACE_ALT) {
                    if (inputLine.length() > 0) {
                        inputLine.setLength(inputLine.length() - 1);
                    }
                    continue;
                }

                // Ctrl-U: clear line
                if (key == KEY_CTRL_U) {
                    inputLine.setLength(0);
                    continue;
                }

                // Printable characters
                if (key >= KEY_PRINTABLE_MIN && key <= KEY_PRINTABLE_MAX) {
                    inputLine.append((char) key);
                }
            }
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                // Log but don't throw - we're already in cleanup
                System.err.println("Warning: error closing reader: " + e.getMessage());
            }
        }
    }

    private void render(Terminal terminal, String inputLine) throws IOException {
        int height = terminal.getHeight();
        
        RenderContext ctx = new RenderContext(terminal.getSize());

        header.render(ctx);
        messageList.render(ctx);
        statusBar.render(ctx);
        
        var input = new InputComponent(inputLine);
        input.render(ctx);

        AttributedString screenContent = ctx.build();
        terminal.writer().write("\033[H");
        terminal.writer().write(screenContent.toAnsi(terminal));
        terminal.writer().flush();
        
        // Position cursor
        int cursorRow = Math.max(0, InputComponent.cursorRow(height));
        int cursorCol = Math.max(2, inputLine.length() + 2);
        terminal.puts(InfoCmp.Capability.cursor_address, cursorRow, cursorCol);
        terminal.writer().flush();
    }

    /**
     * Handle SGR mouse events (ESC [<button;x;yM or ESC[<button;x;ym).
     * Handles scroll wheel events including various terminal implementations.
     */
    private void handleMouseEvent(java.io.Reader reader, boolean alreadyConsumedLt) throws IOException {
        StringBuilder sb = new StringBuilder();
        
        // If we already consumed '<', add it back
        if (alreadyConsumedLt) {
            sb.append('<');
        }
        
        // Read until we get M (press) or m (release)
        while (true) {
            int c = reader.read();
            if (c == -1) {
                break;
            }
            if (c == 'M' || c == 'm') {
                break;
            }
            sb.append((char) c);
        }
        
        String rawEvent = sb.toString();
        debugLog("Raw mouse event: <" + rawEvent + "M");
        
        // Parse the button code
        // Format: <button;x;yM or <button;x;ym
        String[] parts = rawEvent.split(";");
        if (parts.length >= 3 && parts[0].startsWith("<")) {
            try {
                int button = Integer.parseInt(parts[0].substring(1));
                debugLog("Parsed button: " + button);
                
                // SGR mouse encoding for scroll wheel:
                // Button code & ~0x20 gives us the base button
                int baseButton = button & ~0x20;  // Clear shift bit
                debugLog("Base button: " + baseButton);
                
                // 64 = scroll up, 65 = scroll down
                if (baseButton == MOUSE_BUTTON_SCROLL_UP || baseButton == MOUSE_BUTTON_SCROLL_DOWN) {
                    // Throttle rapid scroll events from touchpads
                    long now = System.currentTimeMillis();
                    if (now - lastScrollTime < SCROLL_THROTTLE_MS) {
                        debugLog("Throttled");
                        return; // Skip this event, too soon
                    }
                    lastScrollTime = now;
                    
                    if (baseButton == MOUSE_BUTTON_SCROLL_UP) {
                        debugLog("Scrolling UP");
                        messageList.scrollUp(2);
                    } else {
                        debugLog("Scrolling DOWN");
                        messageList.scrollDown(2);
                    }
                }
            } catch (NumberFormatException e) {
                debugLog("Parse error: " + e.getMessage());
            }
        }
    }

    /** Debug helper - logs mouse events to /tmp/pi-java-mouse.log when enabled */
    private static final boolean DEBUG_MOUSE = false;
    
    private static void debugLog(String msg) {
        if (!DEBUG_MOUSE) {
            return;
        }
        try (java.io.FileWriter fw = new java.io.FileWriter("/tmp/pi-java-mouse.log", true)) {
            fw.write(System.currentTimeMillis() + ": " + msg + "\n");
        } catch (IOException e) {
            // Silently ignore logging errors to avoid disrupting the app
            System.err.println("Debug log error: " + e.getMessage());
        }
    }
}
