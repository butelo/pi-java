package com.example.pijava.ui.input;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.IOException;
import java.io.Reader;
import java.util.logging.Logger;

/**
 * Translates raw key events into semantic {@link Action}s.
 *
 * <p>Handles escape sequences for arrow keys, mouse SGR events, and
 * common control keys. The caller reads a single byte, then delegates
 * to {@link #handle(int, Reader)} which may consume additional bytes
 * from the reader for multi-byte sequences.</p>
 */
public class InputHandler {

    private static final Logger LOG = Logger.getLogger(InputHandler.class.getName());

    // Key codes
    private static final int KEY_ESC = 27;
    private static final int KEY_CTRL_A = 1;
    private static final int KEY_CTRL_C = 3;
    private static final int KEY_CTRL_E = 5;
    private static final int KEY_CTRL_U = 21;
    private static final int KEY_ENTER = 13;
    private static final int KEY_LF = 10;
    private static final int KEY_BACKSPACE = 127;
    private static final int KEY_BACKSPACE_ALT = 8;
    private static final int KEY_PRINTABLE_MIN = 32;

    // Escape sequence codes
    private static final int ESC_SEQ_BRACKET = 91;  // '['
    private static final int ESC_SEQ_UP = 65;       // 'A'
    private static final int ESC_SEQ_DOWN = 66;     // 'B'
    private static final int ESC_SEQ_RIGHT = 67;    // 'C'
    private static final int ESC_SEQ_LEFT = 68;     // 'D'
    private static final int ESC_SEQ_HOME = 72;     // 'H'
    private static final int ESC_SEQ_END = 70;      // 'F'
    private static final int ESC_SEQ_LT = 60;       // '<' (SGR mouse)

    // Mouse button codes for SGR encoding
    private static final int MOUSE_SCROLL_UP = 64;
    private static final int MOUSE_SCROLL_DOWN = 65;

    private final StringBuilder inputLine;

    // Mouse scroll event callback
    private MouseScrollHandler mouseScrollHandler;

    /** Callback for mouse scroll events parsed from SGR sequences. */
    @FunctionalInterface
    public interface MouseScrollHandler {
        void onScroll(boolean up, int amount);
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "StringBuilder is intentionally shared for live input mutation")
    public InputHandler(StringBuilder inputLine) {
        this.inputLine = inputLine;
    }

    public void setMouseScrollHandler(MouseScrollHandler handler) {
        this.mouseScrollHandler = handler;
    }

    /**
     * Process a key event and return the corresponding Action.
     *
     * @param key    the first byte already read from the terminal
     * @param reader the reader to consume additional bytes for escape sequences
     * @return the semantic action
     */
    public Action handle(int key, Reader reader) throws IOException {
        if (key == KEY_ESC) {
            return handleEscSequence(reader);
        }
        if (key == KEY_CTRL_C) {
            return Action.QUIT;
        }
        if (key == KEY_ENTER || key == KEY_LF) {
            if (inputLine.length() > 0) {
                String text = inputLine.toString();
                inputLine.setLength(0);
                return new Action.Submit(text);
            }
            return Action.CONTINUE;
        }
        if (key == KEY_BACKSPACE || key == KEY_BACKSPACE_ALT) {
            return Action.BACKSPACE;
        }
        if (key == KEY_CTRL_U) {
            return Action.CLEAR_LINE;
        }
        if (key == KEY_CTRL_A) {
            return Action.CURSOR_HOME;
        }
        if (key == KEY_CTRL_E) {
            return Action.CURSOR_END;
        }
        if (key >= KEY_PRINTABLE_MIN && !Character.isISOControl(key)) {
            return new Action.InsertChar((char) key);
        }
        return Action.CONTINUE;
    }

    private Action handleEscSequence(Reader reader) throws IOException {
        // Peek with 50ms timeout to distinguish standalone ESC from sequences
        int b2 = peekWithTimeout(reader, 50);
        if (b2 < 0) {
            return Action.QUIT; // Standalone ESC
        }
        b2 = reader.read();
        if (b2 != ESC_SEQ_BRACKET) {
            return Action.CONTINUE; // Unknown sequence
        }
        int b3 = reader.read();
        return switch (b3) {
            case ESC_SEQ_UP -> Action.SCROLL_UP_ONE;
            case ESC_SEQ_DOWN -> Action.SCROLL_DOWN_ONE;
            case ESC_SEQ_RIGHT -> Action.CURSOR_RIGHT;
            case ESC_SEQ_LEFT -> Action.CURSOR_LEFT;
            case ESC_SEQ_HOME -> Action.CURSOR_HOME;
            case ESC_SEQ_END -> Action.CURSOR_END;
            case ESC_SEQ_LT -> {
                handleMouseSgr(reader);
                yield Action.CONTINUE;
            }
            default -> {
                // Could be extended sequences like ESC[1;5C (Ctrl+Right) etc.
                // Consume remaining chars of the sequence
                consumeEscSequenceTail(reader, b3);
                yield Action.CONTINUE;
            }
        };
    }

    /** Consume remaining characters of an unrecognized escape sequence. */
    private void consumeEscSequenceTail(Reader reader, int firstChar) throws IOException {
        // Extended sequences end with an alpha char (64-126)
        // e.g., ESC[1;5C, ESC[3~
        if (firstChar >= 64 && firstChar <= 126) {
            return; // Already consumed the terminator
        }
        // Read until we get the terminator
        while (true) {
            int c = reader.read();
            if (c == -1 || (c >= 64 && c <= 126)) {
                break;
            }
        }
    }

    /** Parse and handle SGR mouse events. */
    private void handleMouseSgr(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        while (true) {
            int c = reader.read();
            if (c == -1 || c == 'M' || c == 'm') {
                break;
            }
            sb.append((char) c);
        }
        String[] parts = sb.toString().split(";");
        if (parts.length >= 3 && mouseScrollHandler != null) {
            try {
                int button = Integer.parseInt(parts[0]);
                int baseButton = button & ~0x20;
                if (baseButton == MOUSE_SCROLL_UP) {
                    mouseScrollHandler.onScroll(true, 2);
                } else if (baseButton == MOUSE_SCROLL_DOWN) {
                    mouseScrollHandler.onScroll(false, 2);
                }
            } catch (NumberFormatException e) {
                LOG.fine("Malformed mouse event: " + e.getMessage());
            }
        }
    }

    /**
     * Peek at the reader with a timeout (ms). Returns -1 if no data available.
     * Relies on JLine's NonBlockingReader.peek() when available.
     */
    @SuppressWarnings("PMD.CloseResource") // Reader is not owned by this method
    private int peekWithTimeout(Reader reader, int timeoutMs) throws IOException {
        if (reader instanceof org.jline.utils.NonBlockingReader nbr) {
            return nbr.peek(timeoutMs);
        }
        // Fallback: just try to read (may block)
        return reader.ready() ? reader.read() : -1;
    }
}
