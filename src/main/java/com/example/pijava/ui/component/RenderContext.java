package com.example.pijava.ui.component;

import org.jline.terminal.Size;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared rendering state passed to every {@link Component}.
 * Provides access to the drawing surface and terminal dimensions so that
 * components can render themselves without coupling to the screen directly.
 *
 * Uses a line-based buffer to properly position content at specific rows.
 */
public class RenderContext {

    private final List<AttributedStringBuilder> lines;
    private final int rows;
    private final int columns;

    public RenderContext(Size size) {
        this.rows = size.getRows();
        this.columns = size.getColumns();
        this.lines = new ArrayList<>();
        for (int i = 0; i < rows; i++) {
            lines.add(new AttributedStringBuilder());
        }
    }

    /** Convenience accessor for terminal width. */
    public int width() {
        return columns;
    }

    /** Convenience accessor for terminal height. */
    public int height() {
        return rows;
    }

    /** Legacy setter used by components to signal layout offsets. */
    public void setCurrentLine(int line) {
        // Intentionally kept for API compatibility — value is not stored.
    }

    /**
     * Set text at a specific line and column with styling.
     * Text that would overflow the terminal width is truncated.
     *
     * @param line   the line number (0-indexed)
     * @param column the column number (0-indexed)
     * @param text   the text to set
     * @param style  the style to apply
     */
    public void putString(int line, int column, String text, AttributedStyle style) {
        if (line < 0 || line >= lines.size() || column >= columns) {
            return;
        }
        // Truncate text that would overflow the terminal width
        int maxLen = columns - column;
        String display = text.length() > maxLen ? text.substring(0, maxLen) : text;

        AttributedStringBuilder lineBuilder = lines.get(line);
        while (lineBuilder.length() < column) {
            lineBuilder.append(' ');
        }
        lineBuilder.style(style);
        lineBuilder.append(display);
    }

    /**
     * Build each line as a separate {@link AttributedString}, padded to full
     * terminal width and truncated if over-width.
     *
     * <p>Callers should write each line to the terminal with explicit cursor
     * positioning (e.g. {@code \033[<row>;1H}) to avoid raw-mode {@code \n}
     * issues that cause content duplication.</p>
     *
     * @return one {@code AttributedString} per terminal row
     */
    public List<AttributedString> buildLines() {
        List<AttributedString> result = new ArrayList<>();
        for (AttributedStringBuilder line : lines) {
            // Pad short lines to full width
            while (line.length() < width()) {
                line.append(' ');
            }
            // Truncate lines that exceed terminal width to prevent scroll/wrap
            if (line.length() > width()) {
                result.add(line.toAttributedString().subSequence(0, width()));
            } else {
                result.add(line.toAttributedString());
            }
        }
        return result;
    }
}
