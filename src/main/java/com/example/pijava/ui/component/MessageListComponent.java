package com.example.pijava.ui.component;

import com.example.pijava.model.Message;
import com.googlecode.lanterna.TextColor;
import java.util.List;

/**
 * Scrollable list of chat messages - vim style.
 *
 * <p>Messages flow naturally from top to bottom. Scroll to view older/newer
 * messages. The view window moves through the message history.</p>
 */
public class MessageListComponent implements Component {

    private final List<Message> messages;
    
    /** View offset - which message index to start rendering from (0 = first message). */
    private int viewOffset = 0;

    public MessageListComponent(List<Message> messages) {
        this.messages = messages;
    }

    /**
     * Scroll up (show older messages).
     * @param amount number of lines to scroll up
     */
    public void scrollUp(int amount) {
        viewOffset = Math.max(0, viewOffset - amount);
    }

    /**
     * Scroll down (show newer messages).
     * @param amount number of lines to scroll down
     */
    public void scrollDown(int amount) {
        // Will be clamped in render based on total lines
        viewOffset += amount;
    }

    /**
     * Scroll to the end (latest messages).
     */
    public void scrollToBottom() {
        viewOffset = Integer.MAX_VALUE; // Will be clamped in render
    }

    @Override
    public void render(RenderContext ctx) {
        var tg = ctx.graphics();
        int height = ctx.height();
        int startRow = 3; // After header (rows 0-2)
        int visibleLines = height - 6; // Reserve rows for header(3) + input(2) + status(1)
        
        // Clamp viewOffset to valid range
        int maxOffset = Math.max(0, countTotalLines(messages) - visibleLines);
        viewOffset = Math.min(viewOffset, maxOffset);
        
        int screenRow = startRow;
        int lineCount = 0; // Total lines processed (for scrolling)
        
        for (int i = 0; i < messages.size(); i++) {
            var msg = messages.get(i);
            var isUser = msg.type() == Message.MessageType.USER;

            tg.setForegroundColor(isUser ? TextColor.ANSI.BLUE : TextColor.ANSI.YELLOW);
            
            var prefix = isUser ? "> " : "< ";
            var lines = msg.content().split("\n", -1);
            
            for (var line : lines) {
                // Skip lines before our view offset
                if (lineCount < viewOffset) {
                    lineCount++;
                    continue;
                }
                
                // Stop if we've filled the screen
                if (screenRow >= height - 3) {
                    break;
                }
                
                tg.putString(0, screenRow++, prefix + line);
                lineCount++;
            }
            
            if (screenRow >= height - 3) {
                break;
            }
        }
        
        // Show scroll position indicator at bottom right (above input area)
        int totalLines = countTotalLines(messages);
        if (totalLines > visibleLines) {
            tg.setForegroundColor(TextColor.ANSI.WHITE);
            int percent = (int) ((float) (viewOffset + visibleLines) / totalLines * 100);
            var indicator = String.format("%d%%", Math.min(100, percent));
            tg.putString(ctx.width() - indicator.length(), height - 4, indicator);
        }
    }
    
    private int countTotalLines(List<Message> msgs) {
        int count = 0;
        for (var msg : msgs) {
            count += msg.content().split("\n", -1).length;
        }
        return count;
    }
}
