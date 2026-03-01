package com.example.pijava.ui.component;

/**
 * Shared layout constants for the TUI component tree.
 *
 * <p>All row positions are derived from these values so that adding or
 * removing header/footer rows only requires changes here.</p>
 */
public final class Layout {

    private Layout() { /* utility */ }

    /** Number of rows reserved for the header (title + subtitle + separator). */
    public static final int HEADER_ROWS = 3;

    /** Number of rows reserved for the input area (separator + text). */
    public static final int INPUT_ROWS = 2;

    /** Number of rows reserved for the status bar. */
    public static final int STATUS_ROWS = 1;

    /** First row of the scrollable message area. */
    public static final int MESSAGE_START_ROW = HEADER_ROWS;

    /**
     * Last row (inclusive) of the scrollable message area, for a given
     * terminal height.
     */
    public static int messageEndRow(int terminalHeight) {
        return terminalHeight - INPUT_ROWS - STATUS_ROWS - 1;
    }

    /** Row of the input separator line. */
    public static int inputSeparatorRow(int terminalHeight) {
        return terminalHeight - INPUT_ROWS - STATUS_ROWS;
    }

    /** Row of the input text line. */
    public static int inputTextRow(int terminalHeight) {
        return inputSeparatorRow(terminalHeight) + 1;
    }

    /** Row of the status bar. */
    public static int statusBarRow(int terminalHeight) {
        return terminalHeight - STATUS_ROWS;
    }
}
