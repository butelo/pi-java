package com.example.pijava.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages the ordered list of {@link ContextMessage}s that form the
 * LLM conversation context.
 *
 * <p>Each call to the LLM replays the full context, so this class is the
 * single source of truth for the conversation history.</p>
 */
public class ContextManager {

    private static final String DEFAULT_SYSTEM_PROMPT = """
            You are a helpful coding assistant running in a terminal \
            (pi-java). You can read files, list directories, and run \
            shell commands to help the user with their coding tasks. \
            Be concise in your responses. When asked to perform an \
            action, use the available tools.""";

    private final List<ContextMessage> messages = new ArrayList<>();

    /** Create a manager with the default system prompt. */
    public ContextManager() {
        messages.add(ContextMessage.system(DEFAULT_SYSTEM_PROMPT));
    }

    /** Create a manager with a custom system prompt. */
    public ContextManager(String systemPrompt) {
        messages.add(ContextMessage.system(systemPrompt));
    }

    /** Append a user message. */
    public void addUser(String content) {
        messages.add(ContextMessage.user(content));
    }

    /** Append a plain assistant reply. */
    public void addAssistant(String content) {
        messages.add(ContextMessage.assistant(content));
    }

    /** Append an assistant message that contains tool-call requests. */
    public void addAssistantToolCalls(
            String content,
            List<ContextMessage.ToolCallData> toolCalls) {
        messages.add(ContextMessage.assistantWithToolCalls(content, toolCalls));
    }

    /** Append a tool-result message. */
    public void addToolResult(String toolCallId, String content) {
        messages.add(ContextMessage.toolResult(toolCallId, content));
    }

    /** Return an unmodifiable view of the current context. */
    public List<ContextMessage> messages() {
        return Collections.unmodifiableList(messages);
    }
}
