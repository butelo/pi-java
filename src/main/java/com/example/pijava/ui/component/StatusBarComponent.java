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

    /** Return the current status-bar text. */
    public String getText() {
        return text;
    }

    @Override
    public void render(RenderContext ctx) {
        int width = ctx.width();
        int statusRow = Layout.statusBarRow(ctx.height());
        // Truncate if needed, then pad to fill the whole row with inverse background
        String displayText = text.length() > width ? text.substring(0, width) : text;
        StringBuilder padded = new StringBuilder(displayText);
        while (padded.length() < width) {
            padded.append(' ');
        }
        ctx.putString(statusRow, 0, padded.toString(), AttributedStyle.DEFAULT.inverse());
    }
}
