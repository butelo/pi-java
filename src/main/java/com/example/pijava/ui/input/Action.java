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

    // -- convenience singletons for the stateless variants --

    Action CONTINUE = new Continue();
    Action QUIT     = new Quit();
    Action SCROLL_UP_ONE = new ScrollUp(1);
    Action SCROLL_DOWN_ONE = new ScrollDown(1);
    Action SCROLL_UP_PAGE = new ScrollUp(5);
    Action SCROLL_DOWN_PAGE = new ScrollDown(5);
}
