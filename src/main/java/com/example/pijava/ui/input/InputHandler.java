package com.example.pijava.ui.input;

/**
 * Translates key events into semantic {@link Action}s.
 * Currently unused - input is handled directly in MainScreen.
 */
public class InputHandler {

    public Action handle(int key) {
        return Action.CONTINUE;
    }
}
