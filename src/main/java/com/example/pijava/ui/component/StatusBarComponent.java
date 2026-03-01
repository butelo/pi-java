package com.example.pijava.ui.component;

import org.jline.utils.AttributedStyle;

/**
 * Single-line status / shortcut-hint bar at the very bottom of the screen.
 */
public class StatusBarComponent implements Component {

    private String text;

    public StatusBarComponent(String text) {
        this.text = text;
    }

    /** Update the status-bar text between frames. */
    public void setText(String text) {
        this.text = text;
    }

    @Override
    public void render(RenderContext ctx) {
        // Status bar is at height - 1 (last line)
        int statusRow = ctx.height() - 1;
        ctx.putString(statusRow, 0, text, AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN));
    }
}
