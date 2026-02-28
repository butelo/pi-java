package com.example.pijava.ui.component;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.graphics.TextGraphics;

/**
 * Shared rendering state passed to every {@link Component}.
 * Provides access to the drawing surface and terminal dimensions so that
 * components can render themselves without coupling to the screen directly.
 *
 * @param graphics the Lanterna {@link TextGraphics} surface to draw on
 * @param size     the current terminal size
 */
public record RenderContext(TextGraphics graphics, TerminalSize size) {

    /** Convenience accessor for terminal width. */
    public int width() {
        return size.getColumns();
    }

    /** Convenience accessor for terminal height. */
    public int height() {
        return size.getRows();
    }
}
