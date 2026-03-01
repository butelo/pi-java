package com.example.pijava.ui.component;

import org.jline.utils.AttributedStyle;

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
        int width = ctx.width();

        // Centre the title
        String padded = title;
        int padding = (width - title.length()) / 2;
        if (padding > 0) {
            padded = " ".repeat(padding) + title;
        }

        // Row 0: Title line (green)
        ctx.putString(0, 0, padded, AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
        
        // Row 1: Subtitle line (cyan)
        ctx.putString(1, 0, subtitle, AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN));
        
        // Row 2: Separator line (default color)
        for (int i = 0; i < width; i++) {
            ctx.putString(2, i, "=", AttributedStyle.DEFAULT);
        }
        
        // Set current line to row 3 for next component
        ctx.setCurrentLine(3);
    }
}
