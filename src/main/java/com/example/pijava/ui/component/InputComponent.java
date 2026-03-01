package com.example.pijava.ui.component;

import org.jline.utils.AttributedStyle;

/**
 * Text-input area with a prompt character, drawn near the bottom of the screen.
 *
 * <p>The component is a pure renderer — the actual buffer mutation happens in
 * {@link com.example.pijava.ui.input.InputHandler}.</p>
 */
public class InputComponent implements Component {

    private final CharSequence buffer;
    private final String prompt;

    public InputComponent(CharSequence buffer, String prompt) {
        this.buffer = buffer;
        this.prompt = prompt;
    }

    public InputComponent(CharSequence buffer) {
        this(buffer, "> ");
    }

    @Override
    public void render(RenderContext ctx) {
        int width = ctx.width();
        int inputY = ctx.height() - 3; // Two rows: separator at -3, input at -2

        // Separator line at row height-3
        for (int i = 0; i < width; i++) {
            ctx.putString(inputY, i, "-", AttributedStyle.DEFAULT);
        }

        // Prompt at row height-2 (green)
        ctx.putString(inputY + 1, 0, prompt, AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));

        // User text at row height-2 (default color)
        ctx.putString(inputY + 1, prompt.length(), buffer.toString(), AttributedStyle.DEFAULT);
    }

    /** The column where the cursor should be placed. */
    public int cursorColumn() {
        return prompt.length() + buffer.length();
    }

    /** The row where the cursor should be placed (relative to terminal height). */
    public static int cursorRow(int terminalHeight) {
        return Math.max(0, terminalHeight - 2);
    }
}
