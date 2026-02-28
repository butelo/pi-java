package com.example.pijava.model;

import java.time.Instant;

/**
 * Immutable message record representing a single chat message.
 *
 * @param content   the text content of the message
 * @param type      whether this is a USER or ASSISTANT message
 * @param timestamp when the message was created
 */
public record Message(String content, MessageType type, Instant timestamp) {

    /** The origin of a message. */
    public enum MessageType { USER, ASSISTANT }

    /** Create a user message with the current timestamp. */
    public static Message user(String content) {
        return new Message(content, MessageType.USER, Instant.now());
    }

    /** Create an assistant message with the current timestamp. */
    public static Message assistant(String content) {
        return new Message(content, MessageType.ASSISTANT, Instant.now());
    }
}
