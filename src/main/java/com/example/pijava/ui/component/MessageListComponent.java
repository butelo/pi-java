package com.example.pijava.ui.component;

import com.example.pijava.model.Message;
import com.googlecode.lanterna.TextColor;
import java.util.List;

/**
 * Scrollable list of chat messages.
 *
 * <p>Displays the most recent messages that fit between the header area and
 * the input area. Each message is colour-coded by type.</p>
 */
public class MessageListComponent implements Component {

    /** Row where the first message is drawn (below the header). */
    private static final int MSG_START_ROW = 4;

    /** Rows reserved at the bottom for input + status. */
    private static final int BOTTOM_RESERVED = 8;

    private final List<Message> messages;

    public MessageListComponent(List<Message> messages) {
        this.messages = messages;
    }

    @Override
    public void render(RenderContext ctx) {
        var tg = ctx.graphics();
        var maxVisible = ctx.height() - BOTTOM_RESERVED;
        var startIdx = Math.max(0, messages.size() - maxVisible);

        for (var i = startIdx; i < messages.size(); i++) {
            var msg = messages.get(i);
            var isUser = msg.type() == Message.MessageType.USER;

            tg.setForegroundColor(isUser ? TextColor.ANSI.BLUE : TextColor.ANSI.YELLOW);
            tg.putString(0, MSG_START_ROW + (i - startIdx),
                         (isUser ? "> " : "< ") + msg.content());
        }
    }
}
