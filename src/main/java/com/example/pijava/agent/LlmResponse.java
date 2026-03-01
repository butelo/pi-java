package com.example.pijava.agent;

import java.util.List;

/**
 * Parsed response from the LLM chat-completion API.
 *
 * @param content   the assistant's text reply (may be {@code null}
 *                  when the response contains only tool calls)
 * @param toolCalls tool invocations requested by the assistant
 */
public record LlmResponse(
        String content,
        List<ContextMessage.ToolCallData> toolCalls) {

    /**
     * Compact constructor that creates a defensive copy of the toolCalls list.
     */
    public LlmResponse {
        toolCalls = toolCalls != null ? List.copyOf(toolCalls) : null;
    }

    /**
     * Returns an unmodifiable view of the tool calls.
     * @return an unmodifiable list of tool calls, or null if none
     */
    @Override
    public List<ContextMessage.ToolCallData> toolCalls() {
        return toolCalls; // Already unmodifiable from List.copyOf
    }

    /** {@code true} when the LLM wants to invoke one or more tools. */
    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }
}
