package com.example.pijava.agent;

import java.util.List;

/**
 * Represents a single message in the LLM conversation context,
 * matching the OpenAI Chat Completions API wire format.
 *
 * @param role       the message role ({@code system}, {@code user},
 *                   {@code assistant}, or {@code tool})
 * @param content    the text content (may be {@code null} for assistant
 *                   messages that only contain tool calls)
 * @param toolCalls  tool invocation requests from the assistant
 * @param toolCallId the ID linking a tool result to its originating call
 */
public record ContextMessage(
        String role,
        String content,
        List<ToolCallData> toolCalls,
        String toolCallId) {

    /** Create a system-prompt message. */
    public static ContextMessage system(String content) {
        return new ContextMessage("system", content, null, null);
    }

    /** Create a user message. */
    public static ContextMessage user(String content) {
        return new ContextMessage("user", content, null, null);
    }

    /** Create an assistant text-reply message. */
    public static ContextMessage assistant(String content) {
        return new ContextMessage("assistant", content, null, null);
    }

    /** Create an assistant message that requests tool invocations. */
    public static ContextMessage assistantWithToolCalls(List<ToolCallData> toolCalls) {
        return new ContextMessage("assistant", null, toolCalls, null);
    }

    /** Create a tool-result message linked to the originating call. */
    public static ContextMessage toolResult(String toolCallId, String content) {
        return new ContextMessage("tool", content, null, toolCallId);
    }

    /**
     * A single tool-call request returned by the LLM.
     *
     * @param id       unique call identifier assigned by the LLM
     * @param type     always {@code "function"} in the current API
     * @param function the function name and serialised arguments
     */
    public record ToolCallData(String id, String type, FunctionData function) { }

    /**
     * Function name and JSON-encoded arguments inside a tool call.
     *
     * @param name      the tool/function name
     * @param arguments JSON string of the arguments object
     */
    public record FunctionData(String name, String arguments) { }
}
