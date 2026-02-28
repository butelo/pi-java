package com.example.pijava.ui.screen;

import com.example.pijava.agent.AgentLoop;
import com.example.pijava.model.Message;
import com.example.pijava.ui.component.*;
import com.example.pijava.ui.input.Action;
import com.example.pijava.ui.input.InputHandler;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The main application screen that composes UI components and drives the
 * event loop.
 *
 * <p>Components are assembled here and rendered each frame. New screens can
 * reuse the same components in different layouts.</p>
 */
public class MainScreen {

    private final List<Message> messages = new ArrayList<>();
    private final InputHandler inputHandler = new InputHandler();
    private final AgentLoop agentLoop;

    // -- reusable components --
    private final HeaderComponent header;
    private final StatusBarComponent statusBar;
    private MessageListComponent messageList;

    /**
     * Create the main screen.
     *
     * @param agentLoop the agent loop, or {@code null} for echo mode
     */
    public MainScreen(AgentLoop agentLoop) {
        this.agentLoop = agentLoop;
        var mode = agentLoop != null ? "LLM" : "Echo";
        this.header = new HeaderComponent(
            "=== pi-java - AI Code Assistant (" + mode + ") ===",
            "Enter message below (ESC to quit)"
        );
        this.statusBar = new StatusBarComponent("↑↓/PgUp/PgDn=scroll | ESC=quit | Enter=send");
        if (agentLoop == null) {
            messages.add(Message.assistant(
                "No API key. Running in echo mode. "
                + "Set OPENAI_API_KEY or use --api-key to enable the agent."));
        }
        this.messageList = new MessageListComponent(messages);
    }

    /**
     * Open the terminal, run the TUI event loop, and clean up on exit.
     */
    public void run() throws IOException {
        var terminal = new DefaultTerminalFactory().createTerminal();
        try (var screen = new TerminalScreen(terminal)) {
            screen.startScreen();
            loop(screen);
        } finally {
            terminal.close();
        }
    }

    // -- internals ----------------------------------------------------------

    private void loop(TerminalScreen screen) throws IOException {
        while (true) {
            render(screen);

            var action = inputHandler.handle(screen.readInput());

            switch (action) {
                case Action.Quit ignored -> {
                    return;
                }
                case Action.Submit submit -> {
                    messages.add(Message.user(submit.text()));
                    messageList.scrollToBottom();
                    if (agentLoop != null) {
                        statusBar.setText("Thinking...");
                        render(screen);
                        try {
                            var reply = agentLoop.process(submit.text());
                            messages.add(Message.assistant(reply));
                            messageList.scrollToBottom();
                        } catch (Exception e) {
                            messages.add(Message.assistant(
                                "Error: " + e.getMessage()));
                            messageList.scrollToBottom();
                        }
                        statusBar.setText("↑↓ scroll | PgUp/Dn page | ESC=quit | Enter=send");
                    } else {
                        messages.add(Message.assistant(submit.text()));
                        messageList.scrollToBottom();
                    }
                }
                case Action.ScrollUp(int amount) -> {
                    if (messageList != null) {
                        messageList.scrollUp(amount);
                    }
                }
                case Action.ScrollDown(int amount) -> {
                    if (messageList != null) {
                        messageList.scrollDown(amount);
                    }
                }
                case Action.Continue ignored -> { /* no-op */ }
            }
        }
    }

    private void render(TerminalScreen screen) throws IOException {
        screen.clear();

        var tg  = screen.newTextGraphics();
        var ctx = new RenderContext(tg, screen.getTerminalSize());
        int height = ctx.height();

        // Render header at top
        header.render(ctx);

        // Render messages - they fill the available space
        messageList.render(ctx);

        // Render status bar at bottom-2
        statusBar.render(ctx);

        // Render input at bottom-1
        var input = new InputComponent(inputHandler.buffer());
        input.render(ctx);

        // Position cursor inside the input area (second to last row)
        screen.setCursorPosition(new TerminalPosition(
            input.cursorColumn(),
            height - 3
        ));

        screen.refresh();
    }
}
