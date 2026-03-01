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
        int height = ctx.height();
        int sepRow = Layout.inputSeparatorRow(height);
        int textRow = Layout.inputTextRow(height);

        // Separator line using box-drawing character
        ctx.putString(sepRow, 0, "─".repeat(width), AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));

        // Prompt (bold green)
        ctx.putString(textRow, 0, prompt, AttributedStyle.BOLD.foreground(AttributedStyle.GREEN));

        // User text — show only the tail that fits if too long
        String displayText = buffer.toString();
        int maxTextWidth = width - prompt.length();
        if (displayText.length() > maxTextWidth && maxTextWidth > 0) {
            displayText = displayText.substring(displayText.length() - maxTextWidth);
        }
        ctx.putString(textRow, prompt.length(), displayText, AttributedStyle.DEFAULT);
    }

    /** The row where the cursor should be placed (relative to terminal height). */
    public static int cursorRow(int terminalHeight) {
        return Layout.inputTextRow(terminalHeight);
    }
}
