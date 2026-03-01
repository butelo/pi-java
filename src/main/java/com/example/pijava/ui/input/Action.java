package com.example.pijava.ui.input;

/**
 * Sealed result type returned by {@link InputHandler} after processing a keystroke.
 *
 * <p>Using a sealed interface lets the compiler enforce exhaustive {@code switch}
 * expressions when consuming actions.</p>
 */
public sealed interface Action {

    /** Continue the main loop — nothing special happened. */
    record Continue() implements Action {}

    /** The user wants to quit the application. */
    record Quit() implements Action {}

    /** The user submitted text. */
    record Submit(String text) implements Action {}

    /** Scroll up in the message list. */
    record ScrollUp(int amount) implements Action {}

    /** Scroll down in the message list. */
    record ScrollDown(int amount) implements Action {}

    /** Move cursor left in the input line. */
    record CursorLeft() implements Action {}

    /** Move cursor right in the input line. */
    record CursorRight() implements Action {}

    /** Move cursor to the beginning of the input line. */
    record CursorHome() implements Action {}

    /** Move cursor to the end of the input line. */
    record CursorEnd() implements Action {}

    /** Delete the character before the cursor. */
    record Backspace() implements Action {}

    /** Clear the entire input line. */
    record ClearLine() implements Action {}

    /** Insert a character at the cursor position. */
    record InsertChar(char ch) implements Action {}

    // -- convenience singletons for the stateless variants --

    Action CONTINUE = new Continue();
    Action QUIT     = new Quit();
    Action SCROLL_UP_ONE = new ScrollUp(1);
    Action SCROLL_DOWN_ONE = new ScrollDown(1);
    Action SCROLL_UP_PAGE = new ScrollUp(5);
    Action SCROLL_DOWN_PAGE = new ScrollDown(5);
    Action CURSOR_LEFT = new CursorLeft();
    Action CURSOR_RIGHT = new CursorRight();
    Action CURSOR_HOME = new CursorHome();
    Action CURSOR_END = new CursorEnd();
    Action BACKSPACE = new Backspace();
    Action CLEAR_LINE = new ClearLine();
}
