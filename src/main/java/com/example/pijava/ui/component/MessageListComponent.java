package com.example.pijava.ui.component;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jline.utils.AttributedStyle;

import com.example.pijava.model.Message;

import java.util.ArrayList;
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

    /**
     * Create a message list component.
     * @param messages the shared message list (intentionally not copied - 
     *                 live updates are needed for real-time display)
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", 
            justification = "Messages list is intentionally shared for live updates")
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
        int height = ctx.height();
        int width = ctx.width();
        int startRow = 3; // After header (rows 0-2)
        int endRow = height - 4; // Before separator (height-3) and status (height-1)
        int visibleLines = endRow - startRow; // Available lines for messages
        
        if (visibleLines <= 0) {
            return; // No space to render messages
        }
        
        // Clamp viewOffset to valid range
        int totalLines = countTotalLines(messages, width);
        int maxOffset = Math.max(0, totalLines - visibleLines);
        viewOffset = Math.min(viewOffset, maxOffset);
        
        int screenRow = startRow;
        int lineCount = 0; // Total lines processed (for scrolling)
        
        for (int i = 0; i < messages.size(); i++) {
            var msg = messages.get(i);
            var isUser = msg.type() == Message.MessageType.USER;

            AttributedStyle style = isUser 
                ? AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE)
                : AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW);
            
            String prefix = isUser ? "> " : "< ";
            String[] originalLines = msg.content().split("\n", -1);
            
            for (String originalLine : originalLines) {
                // Wrap the line to fit terminal width (accounting for prefix)
                List<String> wrappedLines = wrapLine(originalLine, width - prefix.length());
                
                for (String wrappedLine : wrappedLines) {
                    // Skip lines before our view offset
                    if (lineCount < viewOffset) {
                        lineCount++;
                        continue;
                    }
                    
                    // Stop if we've filled the message area
                    if (screenRow > endRow) {
                        break;
                    }
                    
                    // Render the line at the specific row
                    ctx.putString(screenRow, 0, prefix, style);
                    ctx.putString(screenRow, prefix.length(), wrappedLine, style);
                    screenRow++;
                    lineCount++;
                }
                
                if (screenRow > endRow) {
                    break;
                }
            }
            
            if (screenRow > endRow) {
                break;
            }
        }
        
        // Show scroll position indicator at bottom right of message area
        if (totalLines > visibleLines && totalLines > 0) {
            int endPos = Math.min(viewOffset + visibleLines, totalLines);
            int percent = (int) ((float) endPos * 100 / totalLines);
            percent = Math.max(0, Math.min(100, percent));
            String indicator = String.format("%d%%", percent);
            
            // Position at bottom right of message area (row = endRow)
            int paddingNeeded = Math.max(0, width - indicator.length());
            for (int i = 0; i < paddingNeeded; i++) {
                ctx.putString(endRow, i, " ", AttributedStyle.DEFAULT);
            }
            ctx.putString(endRow, paddingNeeded, indicator, AttributedStyle.DEFAULT.foreground(AttributedStyle.WHITE));
        }
        
        // Set current line for next component (status bar will be at height-1)
        ctx.setCurrentLine(height - 1);
    }
    
    /**
     * Wrap a line to fit within the given width, breaking at word boundaries.
     */
    private List<String> wrapLine(String line, int maxWidth) {
        List<String> result = new ArrayList<>();
        
        if (maxWidth <= 0) {
            result.add(line);
            return result;
        }
        
        if (line.length() <= maxWidth) {
            result.add(line);
            return result;
        }
        
        int start = 0;
        while (start < line.length()) {
            int end = Math.min(start + maxWidth, line.length());
            
            // If we're not at the end of the line, try to break at a word boundary
            if (end < line.length()) {
                // Look for the last space within the limit
                int lastSpace = line.lastIndexOf(' ', end);
                if (lastSpace > start) {
                    end = lastSpace;
                }
                // If no space found, just break at maxWidth (hard break)
            }
            
            result.add(line.substring(start, end).trim());
            start = end;
            
            // Skip any spaces after the break point
            while (start < line.length() && line.charAt(start) == ' ') {
                start++;
            }
        }
        
        return result;
    }
    
    private int countTotalLines(List<Message> msgs, int width) {
        int count = 0;
        for (var msg : msgs) {
            String[] originalLines = msg.content().split("\n", -1);
            for (String line : originalLines) {
                count += wrapLine(line, width - 3).size(); // -3 for prefix and padding
            }
        }
        return count;
    }
}
