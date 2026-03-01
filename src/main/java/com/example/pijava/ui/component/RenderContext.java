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
    private int cursorLine;

    public RenderContext(Size size) {
        // Store immutable copies of the dimensions
        this.rows = size.getRows();
        this.columns = size.getColumns();
        this.lines = new ArrayList<>();
        this.cursorLine = 0;
        // Pre-initialize all lines
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
    
    /** Get current line index. */
    public int currentLine() {
        return cursorLine;
    }

    /** Set current line index. */
    public void setCurrentLine(int line) {
        this.cursorLine = Math.max(0, Math.min(line, height() - 1));
    }
    
    /**
     * Append a styled string at the current cursor position on the current line.
     * @param text the text to append
     * @param style the style to apply
     */
    public void appendStyled(String text, AttributedStyle style) {
        if (cursorLine >= 0 && cursorLine < lines.size()) {
            lines.get(cursorLine).style(style);
            lines.get(cursorLine).append(text);
        }
    }

    /**
     * Append a plain string at the current cursor position on the current line.
     * @param text the text to append
     */
    public void append(String text) {
        if (cursorLine >= 0 && cursorLine < lines.size()) {
            lines.get(cursorLine).append(text);
        }
    }

    /**
     * Move to the next line.
     */
    public void newline() {
        cursorLine++;
    }

    /**
     * Fill the current line with a character up to the terminal width.
     * @param ch the character to repeat
     */
    public void fillLine(char ch) {
        if (cursorLine >= 0 && cursorLine < lines.size()) {
            for (int i = 0; i < width(); i++) {
                lines.get(cursorLine).append(ch);
            }
        }
    }
    
    /**
     * Set text at a specific line and column with styling.
     * @param line the line number (0-indexed)
     * @param column the column number (0-indexed)
     * @param text the text to set
     * @param style the style to apply
     */
    public void putString(int line, int column, String text, AttributedStyle style) {
        if (line >= 0 && line < lines.size()) {
            AttributedStringBuilder lineBuilder = lines.get(line);
            // Pad with spaces if needed to reach the column
            while (lineBuilder.length() < column) {
                lineBuilder.append(' ');
            }
            lineBuilder.style(style);
            lineBuilder.append(text);
        }
    }
    
    /**
     * Build the final AttributedString from all lines.
     * @return the complete screen content
     */
    public AttributedString build() {
        AttributedStringBuilder result = new AttributedStringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            AttributedStringBuilder line = lines.get(i);
            // Ensure each line is exactly width characters
            while (line.length() < width()) {
                line.append(' ');
            }
            result.append(line);
            if (i < lines.size() - 1) {
                result.append('\n');
            }
        }
        return result.toAttributedString();
    }
    
    /**
     * Clear a specific line.
     * @param line the line number to clear
     */
    public void clearLine(int line) {
        if (line >= 0 && line < lines.size()) {
            lines.set(line, new AttributedStringBuilder());
        }
    }
    
    /**
     * Clear all lines.
     */
    public void clear() {
        for (int i = 0; i < lines.size(); i++) {
            lines.set(i, new AttributedStringBuilder());
        }
        cursorLine = 0;
    }
}
