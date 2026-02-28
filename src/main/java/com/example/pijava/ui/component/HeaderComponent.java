package com.example.pijava.ui.component;

import com.googlecode.lanterna.TextColor;

/**
 * Renders a centred title bar and subtitle at the top of the screen.
 *
 * <p>Reusable: pass any title/subtitle via the constructor.</p>
 */
public class HeaderComponent implements Component {

    private final String title;
    private final String subtitle;

    public HeaderComponent(String title, String subtitle) {
        this.title = title;
        this.subtitle = subtitle;
    }

    @Override
    public void render(RenderContext ctx) {
        var tg = ctx.graphics();
        var width = ctx.width();

        // Centre the title
        var padded = title;
        var padding = (width - title.length()) / 2;
        if (padding > 0) {
            padded = " ".repeat(padding) + title;
        }

        tg.setForegroundColor(TextColor.ANSI.GREEN);
        tg.putString(0, 0, padded);

        tg.setForegroundColor(TextColor.ANSI.CYAN);
        tg.putString(0, 1, subtitle);

        tg.setForegroundColor(TextColor.ANSI.DEFAULT);
        tg.putString(0, 2, "=".repeat(width));
    }
}
