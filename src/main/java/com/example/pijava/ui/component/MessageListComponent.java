package com.example.pijava.ui.component;

import com.example.pijava.model.Message;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jline.utils.AttributedStyle;

/**
 * Scrollable list of chat messages - vim style.
 *
 * <p>Messages flow naturally from top to bottom. Scroll to view older/newer
 * messages. The view window moves through the message history.</p>
 */
public class MessageListComponent implements Component {

    private static final DateTimeFormatter TIME_FMT =
        DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    private static final Set<String> CODE_KEYWORDS = new HashSet<>(Arrays.asList(
        "if", "else", "while", "for", "return", "function", "var", "let", "const",
        "class", "public", "private", "protected", "static", "void", "new", "import",
        "package", "switch", "case", "break", "continue", "try", "catch", "finally",
        "throw", "throws", "true", "false", "null"
    ));

    private static final AttributedStyle CODE_BASE_STYLE =
        AttributedStyle.DEFAULT.foreground(AttributedStyle.WHITE);
    private static final AttributedStyle CODE_KEYWORD_STYLE =
        AttributedStyle.BOLD.foreground(AttributedStyle.MAGENTA);
    private static final AttributedStyle CODE_STRING_STYLE =
        AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN);
    private static final AttributedStyle CODE_NUMBER_STYLE =
        AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN);
    private static final AttributedStyle CODE_COMMENT_STYLE =
        AttributedStyle.DEFAULT.faint().foreground(AttributedStyle.BLACK);
    private static final AttributedStyle CODE_GUTTER_STYLE =
        AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN);

    private final List<Message> messages;
    
    /** View offset - which line index to start rendering from. */
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

    /** Compute the wrap width for message text given a terminal width. */
    private int wrapWidth(int terminalWidth) {
        // prefix is always 2 chars ("&gt; " or "&lt; "), timestamp takes ~7 chars ("HH:mm  ")
        return Math.max(1, terminalWidth - 2);
    }

    private int putMarkdownString(
            RenderContext ctx,
            int row,
            int col,
            String text,
            AttributedStyle baseStyle,
            AttributedStyle inlineCodeStyle,
            AttributedStyle boldStyle) {
        int outCol = col;
        int i = 0;
        boolean inBold = false;
        boolean inInlineCode = false;

        while (i < text.length()) {
            if (text.charAt(i) == '`') {
                inInlineCode = !inInlineCode;
                i++;
                continue;
            }
            if (i + 1 < text.length() && text.charAt(i) == '*' && text.charAt(i + 1) == '*') {
                inBold = !inBold;
                i += 2;
                continue;
            }

            int runStart = i;
            while (i < text.length()) {
                if (text.charAt(i) == '`') {
                    break;
                }
                if (i + 1 < text.length() && text.charAt(i) == '*' && text.charAt(i + 1) == '*') {
                    break;
                }
                i++;
            }

            String run = text.substring(runStart, i);
            if (inInlineCode) {
                outCol += putCodeString(ctx, row, outCol, run, inlineCodeStyle);
            } else {
                AttributedStyle style = inBold ? boldStyle : baseStyle;
                ctx.putString(row, outCol, run, style);
                outCol += run.length();
            }
        }

        return outCol - col;
    }

    private int putCodeString(RenderContext ctx, int row, int col, String text, AttributedStyle baseStyle) {
        int outCol = col;
        int i = 0;

        while (i < text.length()) {
            if (i + 1 < text.length() && text.charAt(i) == '/' && text.charAt(i + 1) == '/') {
                String comment = text.substring(i);
                ctx.putString(row, outCol, comment, CODE_COMMENT_STYLE);
                outCol += comment.length();
                break;
            }

            char ch = text.charAt(i);
            if (ch == '"' || ch == '\'') {
                int end = i + 1;
                boolean escaped = false;
                while (end < text.length()) {
                    char c = text.charAt(end);
                    if (escaped) {
                        escaped = false;
                    } else if (c == '\\') {
                        escaped = true;
                    } else if (c == ch) {
                        end++;
                        break;
                    }
                    end++;
                }
                String str = text.substring(i, Math.min(end, text.length()));
                ctx.putString(row, outCol, str, CODE_STRING_STYLE);
                outCol += str.length();
                i = end;
                continue;
            }

            if (Character.isDigit(ch)) {
                int end = i + 1;
                while (end < text.length()) {
                    char c = text.charAt(end);
                    if (!Character.isDigit(c) && c != '.') {
                        break;
                    }
                    end++;
                }
                String number = text.substring(i, end);
                ctx.putString(row, outCol, number, CODE_NUMBER_STYLE);
                outCol += number.length();
                i = end;
                continue;
            }

            if (Character.isLetter(ch) || ch == '_') {
                int end = i + 1;
                while (end < text.length()) {
                    char c = text.charAt(end);
                    if (!Character.isLetterOrDigit(c) && c != '_') {
                        break;
                    }
                    end++;
                }
                String word = text.substring(i, end);
                AttributedStyle style = CODE_KEYWORDS.contains(word) ? CODE_KEYWORD_STYLE : baseStyle;
                ctx.putString(row, outCol, word, style);
                outCol += word.length();
                i = end;
                continue;
            }

            String other = String.valueOf(ch);
            ctx.putString(row, outCol, other, baseStyle);
            outCol += 1;
            i++;
        }

        return outCol - col;
    }

    private List<String> wrapCodeLine(String line, int maxWidth) {
        List<String> result = new ArrayList<>();

        if (maxWidth <= 0) {
            result.add(line);
            return result;
        }

        if (line.isEmpty()) {
            result.add("");
            return result;
        }

        int start = 0;
        while (start < line.length()) {
            int end = Math.min(start + maxWidth, line.length());
            result.add(line.substring(start, end));
            start = end;
        }

        return result;
    }

    @Override
    public void render(RenderContext ctx) {
        int height = ctx.height();
        int width = ctx.width();
        int startRow = Layout.MESSAGE_START_ROW;
        int endRow = Layout.messageEndRow(height);
        int visibleLines = endRow - startRow;
        
        if (visibleLines <= 0) {
            return; // No space to render messages
        }

        int wrapW = wrapWidth(width);
        
        // Clamp viewOffset to valid range
        int totalLines = countTotalLines(messages, wrapW);
        int maxOffset = Math.max(0, totalLines - visibleLines);
        viewOffset = Math.min(viewOffset, maxOffset);
        
        int screenRow = startRow;
        int lineCount = 0; // Total lines processed (for scrolling)
        
        for (int i = 0; i < messages.size(); i++) {
            var msg = messages.get(i);
            var type = msg.type();

            AttributedStyle baseStyle;
            AttributedStyle prefixStyle;
            AttributedStyle boldStyle;
            AttributedStyle inlineCodeStyle;
            String prefix;

            if (type == Message.MessageType.USER) {
                baseStyle = AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE);
                prefixStyle = AttributedStyle.BOLD.foreground(AttributedStyle.BLUE);
                boldStyle = AttributedStyle.BOLD.foreground(AttributedStyle.BLUE);
                inlineCodeStyle = CODE_BASE_STYLE;
                prefix = "\u25b6 "; // ▶
            } else if (type == Message.MessageType.TOOL_CALL) {
                baseStyle = AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN);
                prefixStyle = AttributedStyle.BOLD.foreground(AttributedStyle.CYAN);
                boldStyle = AttributedStyle.BOLD.foreground(AttributedStyle.CYAN);
                inlineCodeStyle = CODE_BASE_STYLE;
                prefix = "\u2699 "; // ⚙
            } else if (type == Message.MessageType.TOOL_RESULT) {
                baseStyle = AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA);
                prefixStyle = AttributedStyle.BOLD.foreground(AttributedStyle.MAGENTA);
                boldStyle = AttributedStyle.BOLD.foreground(AttributedStyle.MAGENTA);
                inlineCodeStyle = CODE_BASE_STYLE;
                prefix = "\u2713 "; // ✓
            } else {
                baseStyle = AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW);
                prefixStyle = AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW);
                boldStyle = AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW);
                inlineCodeStyle = CODE_BASE_STYLE;
                prefix = "\u25c0 "; // ◀
            }

            String contPrefix = "  "; // continuation lines indented
            String timestamp = TIME_FMT.format(msg.timestamp());
            String[] originalLines = msg.content().split("\n", -1);
            boolean inCodeBlock = false;
            final String codeBlockPrefix = "│ ";

            boolean firstLineOfMessage = true;
            
            for (String originalLine : originalLines) {
                String trimmed = originalLine.trim();
                if (trimmed.startsWith("```")) {
                    inCodeBlock = !inCodeBlock;
                    continue;
                }

                List<String> wrappedLines;
                if (inCodeBlock) {
                    int codeWrapWidth = Math.max(1, wrapW - codeBlockPrefix.length());
                    wrappedLines = wrapCodeLine(originalLine, codeWrapWidth);
                } else {
                    wrappedLines = wrapLine(originalLine, wrapW);
                }
                
                for (String wrappedLine : wrappedLines) {
                    // Skip lines before our view offset
                    if (lineCount < viewOffset) {
                        lineCount++;
                        firstLineOfMessage = false;
                        continue;
                    }
                    
                    // Stop if we've filled the message area
                    if (screenRow > endRow) {
                        break;
                    }
                    
                    // Render the line at the specific row
                    if (firstLineOfMessage) {
                        ctx.putString(screenRow, 0, prefix, prefixStyle);
                        int renderedLen;
                        if (inCodeBlock) {
                            ctx.putString(screenRow, prefix.length(), codeBlockPrefix, CODE_GUTTER_STYLE);
                            int contentLen = putCodeString(
                                    ctx,
                                    screenRow,
                                    prefix.length() + codeBlockPrefix.length(),
                                    wrappedLine,
                                    CODE_BASE_STYLE);
                            renderedLen = codeBlockPrefix.length() + contentLen;
                        } else {
                            renderedLen = putMarkdownString(
                                    ctx,
                                    screenRow,
                                    prefix.length(),
                                    wrappedLine,
                                    baseStyle,
                                    inlineCodeStyle,
                                    boldStyle);
                        }
                        // Right-align timestamp on first line (faint style)
                        if (width > timestamp.length() + prefix.length() + renderedLen + 2) {
                            int tsCol = width - timestamp.length() - 1;
                            ctx.putString(screenRow, tsCol, timestamp,
                                AttributedStyle.DEFAULT.faint().foreground(AttributedStyle.WHITE));
                        }
                        firstLineOfMessage = false;
                    } else {
                        ctx.putString(screenRow, 0, contPrefix, baseStyle);
                        if (inCodeBlock) {
                            ctx.putString(screenRow, contPrefix.length(), codeBlockPrefix, CODE_GUTTER_STYLE);
                            putCodeString(
                                    ctx,
                                    screenRow,
                                    contPrefix.length() + codeBlockPrefix.length(),
                                    wrappedLine,
                                    CODE_BASE_STYLE);
                        } else {
                            putMarkdownString(
                                    ctx,
                                    screenRow,
                                    contPrefix.length(),
                                    wrappedLine,
                                    baseStyle,
                                    inlineCodeStyle,
                                    boldStyle);
                        }
                    }
                    screenRow++;
                    lineCount++;
                }
                
                if (screenRow > endRow) {
                    break;
                }
            }

            // Blank separator line between messages
            if (i < messages.size() - 1) {
                if (lineCount < viewOffset) {
                    lineCount++;
                } else if (screenRow <= endRow) {
                    screenRow++; // blank row
                    lineCount++;
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
            String indicator = String.format(" %d%% ", percent);
            
            // Position at bottom right of message area (row = endRow)
            int col = Math.max(0, width - indicator.length());
            ctx.putString(endRow, col, indicator,
                AttributedStyle.DEFAULT.inverse().foreground(AttributedStyle.WHITE));
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
    
    private int countTotalLines(List<Message> msgs, int wrapW) {
        int count = 0;
        for (int i = 0; i < msgs.size(); i++) {
            var msg = msgs.get(i);
            String[] originalLines = msg.content().split("\n", -1);
            boolean inCodeBlock = false;
            for (String line : originalLines) {
                String trimmed = line.trim();
                if (trimmed.startsWith("```")) {
                    inCodeBlock = !inCodeBlock;
                    continue;
                }

                if (inCodeBlock) {
                    int codeWrapWidth = Math.max(1, wrapW - 2);
                    count += wrapCodeLine(line, codeWrapWidth).size();
                } else {
                    count += wrapLine(line, wrapW).size();
                }
            }
            // Blank separator between messages
            if (i < msgs.size() - 1) {
                count++;
            }
        }
        return count;
    }
}
