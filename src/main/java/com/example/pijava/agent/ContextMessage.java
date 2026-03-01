package com.example.pijava.agent;

import java.util.List;

/**
 * Represents a single message in the LLM conversation context,
 * matching the OpenAI Chat Completions API wire format.
 *
 * @param role       the message role ({@code system}, {@code user},
 *                   {@code assistant}, or {@code tool})
 * @param content    the text content (may be empty for assistant
 *                   messages that only contain tool calls)
 * @param toolCalls  tool invocation requests from the assistant
 * @param toolCallId the ID linking a tool result to its originating call
 */
public record ContextMessage(
        String role,
        String content,
        List<ToolCallData> toolCalls,
        String toolCallId) {

    /**
     * Compact constructor that creates a defensive copy of the toolCalls list.
     */
    public ContextMessage {
        toolCalls = toolCalls != null ? List.copyOf(toolCalls) : List.of();
        content = content != null ? content : "";
        toolCallId = toolCallId != null ? toolCallId : "";
    }

    /**
     * Returns an unmodifiable view of the tool calls.
     * @return an unmodifiable list of tool calls, empty if none
     */
    @Override
    public List<ToolCallData> toolCalls() {
        return toolCalls; // Already unmodifiable from List.copyOf
    }

    /** Create a system-prompt message. */
    public static ContextMessage system(String content) {
        return new ContextMessage("system", content, List.of(), "");
    }

    /** Create a user message. */
    public static ContextMessage user(String content) {
        return new ContextMessage("user", content, List.of(), "");
    }

    /** Create an assistant text-reply message. */
    public static ContextMessage assistant(String content) {
        return new ContextMessage("assistant", content, List.of(), "");
    }

    /** Create an assistant message that requests tool invocations. */
    public static ContextMessage assistantWithToolCalls(
            String content, List<ToolCallData> toolCalls) {
        return new ContextMessage("assistant", content, toolCalls, "");
    }

    /** Create a tool-result message linked to the originating call. */
    public static ContextMessage toolResult(String toolCallId, String content) {
        return new ContextMessage("tool", content, List.of(), toolCallId);
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