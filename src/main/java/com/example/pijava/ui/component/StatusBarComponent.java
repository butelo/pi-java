package com.example.pijava.ui.component;

import com.googlecode.lanterna.TextColor;

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
        var tg = ctx.graphics();
        tg.setForegroundColor(TextColor.ANSI.CYAN);
        tg.putString(0, ctx.height() - 1, text);
    }
}
