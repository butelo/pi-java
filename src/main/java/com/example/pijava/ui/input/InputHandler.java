package com.example.pijava.ui.input;

import com.googlecode.lanterna.input.KeyStroke;

/**
 * Translates raw Lanterna keystrokes into semantic {@link Action}s.
 *
 * <p>This class owns the mutable input buffer so that neither the screen nor
 * the components need to know about raw keyboard handling.</p>
 */
public class InputHandler {

    private final StringBuilder buffer = new StringBuilder();

    /** Current content of the input buffer (read-only view for rendering). */
    public CharSequence buffer() {
        return buffer;
    }

    /**
     * Process a single keystroke and return the resulting action.
     *
     * @param key the keystroke (may be {@code null} if no input was available)
     * @return a semantic {@link Action}
     */
    public Action handle(KeyStroke key) {
        if (key == null) {
            return Action.CONTINUE;
        }

        return switch (key.getKeyType()) {
            case Escape -> Action.QUIT;
            case Enter -> {
                if (buffer.isEmpty()) {
                    yield Action.CONTINUE;
                }
                var text = buffer.toString();
                buffer.setLength(0);
                yield new Action.Submit(text);
            }
            case Backspace -> {
                if (!buffer.isEmpty()) {
                    buffer.deleteCharAt(buffer.length() - 1);
                }
                yield Action.CONTINUE;
            }
            case Character -> {
                var c = key.getCharacter();
                if (c >= 32 && c < 127) {
                    buffer.append(c);
                }
                yield Action.CONTINUE;
            }
            default -> Action.CONTINUE;
        };
    }
}
