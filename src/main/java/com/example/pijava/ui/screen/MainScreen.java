package com.example.pijava.ui.screen;

import com.example.pijava.agent.AgentLoop;
import com.example.pijava.model.Message;
import com.example.pijava.ui.component.*;
import com.example.pijava.ui.input.Action;
import com.example.pijava.ui.input.InputHandler;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.InfoCmp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The main application screen.
 */
public class MainScreen {

    // Throttle rapid scroll events (e.g., from touchpads)
    private long lastScrollTime = 0;
    private static final long SCROLL_THROTTLE_MS = 16; // ~60fps

    // Braille spinner frames for "Thinking" animation
    private static final String[] SPINNER = {
        "\u280b", "\u2819", "\u2839", "\u2838", "\u283c", "\u2834", "\u2826", "\u2827", "\u2807", "\u280f"
    };
    private static final String DEFAULT_STATUS =
        " \u2191\u2193 scroll  |  ESC quit  |  Enter send  |  Ctrl-U clear  |  Shift+drag select ";

    private final List<Message> messages = new ArrayList<>();
    private final AgentLoop agentLoop;
    private final HeaderComponent header;
    private final StatusBarComponent statusBar;
    private MessageListComponent messageList;

    /** Cursor position within the input line (0-based index). */
    private int cursorPos = 0;

    public MainScreen(AgentLoop agentLoop) {
        this.agentLoop = agentLoop;
        String mode = agentLoop != null ? "LLM" : "Echo";
        this.header = new HeaderComponent(
            "  \u2728 pi-java \u2014 AI Code Assistant (" + mode + ")  ",
            "Type a message below  \u00b7  ESC to quit"
        );
        this.statusBar = new StatusBarComponent(DEFAULT_STATUS);
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
            // Enter alternate screen buffer (restores previous content on exit)
            terminal.writer().write("\033[?1049h");
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
            // Leave alternate screen buffer
            terminal.writer().write("\033[?1049l");
            terminal.writer().flush();
            terminal.close();
        }
    }

    @SuppressWarnings("PMD.CloseResource") // reader is closed in finally block
    private void loop(Terminal terminal) throws IOException {
        StringBuilder inputLine = new StringBuilder();
        var reader = terminal.reader();
        var handler = new InputHandler(inputLine);

        // Wire mouse scroll to message list with throttling
        handler.setMouseScrollHandler((up, amount) -> {
            long now = System.currentTimeMillis();
            if (now - lastScrollTime < SCROLL_THROTTLE_MS) {
                return;
            }
            lastScrollTime = now;
            if (up) {
                messageList.scrollUp(amount);
            } else {
                messageList.scrollDown(amount);
            }
        });

        try {
            boolean inputOnlyRender = false;
            while (true) {
                if (inputOnlyRender) {
                    renderInputOnly(terminal, inputLine.toString());
                } else {
                    render(terminal, inputLine.toString());
                }
                inputOnlyRender = false;

                int key = reader.read();
                Action action = handler.handle(key, reader);

                switch (action) {
                    case Action.Quit ignored -> {
                        return;
                    }
                    case Action.Submit s -> {
                        cursorPos = 0;
                        messages.add(Message.user(s.text()));
                        messageList.scrollToBottom();

                        if (agentLoop != null) {
                            // Run LLM call on background thread with spinner animation
                            AtomicReference<String> result = new AtomicReference<>();
                            AtomicReference<Exception> error = new AtomicReference<>();

                            // Full render once to show the user message before spinning
                            statusBar.setText(" " + SPINNER[0] + " Thinking\u2026");
                            render(terminal, "");

                            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                                try {
                                    result.set(agentLoop.process(s.text()));
                                } catch (Exception e) {
                                    error.set(e);
                                }
                            });

                            int frame = 1;
                            while (!future.isDone()) {
                                String spin = SPINNER[frame % SPINNER.length];
                                statusBar.setText(" " + spin + " Thinking\u2026");
                                renderStatusBarOnly(terminal);
                                frame++;
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                    break;
                                }
                            }

                            if (error.get() != null) {
                                messages.add(Message.assistant("Error: " + error.get().getMessage()));
                            } else if (result.get() != null) {
                                messages.add(Message.assistant(result.get()));
                            }
                            messageList.scrollToBottom();
                            statusBar.setText(DEFAULT_STATUS);
                        } else {
                            messages.add(Message.assistant(s.text()));
                            messageList.scrollToBottom();
                        }
                    }
                    case Action.ScrollUp su -> messageList.scrollUp(su.amount());
                    case Action.ScrollDown sd -> messageList.scrollDown(sd.amount());
                    case Action.CursorLeft ignored -> {
                        if (cursorPos > 0) {
                            cursorPos--;
                        }
                        inputOnlyRender = true;
                    }
                    case Action.CursorRight ignored -> {
                        if (cursorPos < inputLine.length()) {
                            cursorPos++;
                        }
                        inputOnlyRender = true;
                    }
                    case Action.CursorHome ignored -> {
                        cursorPos = 0;
                        inputOnlyRender = true;
                    }
                    case Action.CursorEnd ignored -> {
                        cursorPos = inputLine.length();
                        inputOnlyRender = true;
                    }
                    case Action.Backspace ignored -> {
                        if (cursorPos > 0) {
                            inputLine.deleteCharAt(cursorPos - 1);
                            cursorPos--;
                        }
                        inputOnlyRender = true;
                    }
                    case Action.ClearLine ignored -> {
                        inputLine.setLength(0);
                        cursorPos = 0;
                        inputOnlyRender = true;
                    }
                    case Action.InsertChar ic -> {
                        inputLine.insert(cursorPos, ic.ch());
                        cursorPos++;
                        inputOnlyRender = true;
                    }
                    case Action.Continue ignored -> { /* no-op */ }
                }
            }
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                System.err.println("Warning: error closing reader: " + e.getMessage());
            }
        }
    }

    private void render(Terminal terminal, String inputLine) throws IOException {
        RenderContext ctx = new RenderContext(terminal.getSize());

        header.render(ctx);
        messageList.render(ctx);
        statusBar.render(ctx);

        var input = new InputComponent(inputLine);
        input.render(ctx);

        List<AttributedString> screenLines = ctx.buildLines();
        // Hide cursor to prevent flicker during redraw
        terminal.writer().write("\033[?25l");
        // Write each line with explicit cursor positioning to avoid
        // raw-mode \n issues that cause header/content duplication.
        for (int i = 0; i < screenLines.size(); i++) {
            terminal.writer().write("\033[" + (i + 1) + ";1H");
            terminal.writer().write(screenLines.get(i).toAnsi(terminal));
        }

        positionInputCursor(terminal);
        // Show cursor and flush once
        terminal.writer().write("\033[?25h");
        terminal.writer().flush();
    }

    /**
     * Fast path: only re-render the input text row and reposition the cursor.
     * Avoids rebuilding the entire screen buffer for every keystroke.
     */
    private void renderInputOnly(Terminal terminal, String inputLine) throws IOException {
        int width = terminal.getWidth();
        int textRow = Layout.inputTextRow(terminal.getHeight());

        terminal.writer().write("\033[?25l");
        // Move to input text row (ANSI rows are 1-based)
        terminal.writer().write("\033[" + (textRow + 1) + ";1H");

        AttributedStringBuilder builder = new AttributedStringBuilder();
        String prompt = "> ";
        builder.style(AttributedStyle.BOLD.foreground(AttributedStyle.GREEN));
        builder.append(prompt);

        String displayText = inputLine;
        int maxTextWidth = width - prompt.length();
        if (displayText.length() > maxTextWidth && maxTextWidth > 0) {
            displayText = displayText.substring(displayText.length() - maxTextWidth);
        }
        builder.style(AttributedStyle.DEFAULT);
        builder.append(displayText);

        // Pad to full width to overwrite any stale characters
        while (builder.length() < width) {
            builder.append(' ');
        }

        terminal.writer().write(builder.toAttributedString().toAnsi(terminal));

        positionInputCursor(terminal);
        terminal.writer().write("\033[?25h");
        terminal.writer().flush();
    }

    /**
     * Fast path: only re-render the status bar row (used during spinner animation).
     * Avoids rebuilding the entire screen buffer every 100ms while waiting on the LLM.
     */
    private void renderStatusBarOnly(Terminal terminal) throws IOException {
        int width = terminal.getWidth();
        int height = terminal.getHeight();
        int statusRow = Layout.statusBarRow(height);

        terminal.writer().write("\033[?25l");
        // Move to status bar row (ANSI rows are 1-based)
        terminal.writer().write("\033[" + (statusRow + 1) + ";1H");

        AttributedStringBuilder builder = new AttributedStringBuilder();
        // Fill with inverse background then overlay text
        String displayText = statusBar.getText();
        if (displayText.length() > width) {
            displayText = displayText.substring(0, width);
        }
        builder.style(AttributedStyle.DEFAULT.inverse());
        builder.append(displayText);
        while (builder.length() < width) {
            builder.append(' ');
        }

        terminal.writer().write(builder.toAttributedString().toAnsi(terminal));
        terminal.writer().write("\033[?25h");
        terminal.writer().flush();
    }

    /** Move the terminal cursor to the current edit position in the input line. */
    private void positionInputCursor(Terminal terminal) {
        int cursorRow = Math.max(0, InputComponent.cursorRow(terminal.getHeight()));
        int promptLen = 2; // "> "
        int cursorCol = Math.max(promptLen, cursorPos + promptLen);
        terminal.puts(InfoCmp.Capability.cursor_address, cursorRow, cursorCol);
    }
}
