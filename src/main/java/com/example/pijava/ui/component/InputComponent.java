package com.example.pijava.ui.component;

import com.googlecode.lanterna.TextColor;

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
        var tg = ctx.graphics();
        var width = ctx.width();
        var inputY = ctx.height() - 3;

        // Separator line
        tg.setForegroundColor(TextColor.ANSI.DEFAULT);
        tg.putString(0, inputY - 1, "-".repeat(width));

        // Prompt
        tg.setForegroundColor(TextColor.ANSI.GREEN);
        tg.putString(0, inputY, prompt);

        // User text
        tg.setForegroundColor(TextColor.ANSI.DEFAULT);
        tg.putString(prompt.length(), inputY, buffer.toString());
    }

    /** The column where the cursor should be placed. */
    public int cursorColumn() {
        return prompt.length() + buffer.length();
    }

    /** The row where the cursor should be placed (relative to terminal height). */
    public static int cursorRow(int terminalHeight) {
        return terminalHeight - 3;
    }
}
