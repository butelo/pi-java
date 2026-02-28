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

    /** {@code true} when the LLM wants to invoke one or more tools. */
    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }
}
