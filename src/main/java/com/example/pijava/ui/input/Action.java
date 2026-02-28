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

    // -- convenience singletons for the stateless variants --

    Action CONTINUE = new Continue();
    Action QUIT     = new Quit();
}
