package com.example.pijava.agent;

import com.example.pijava.agent.tool.ToolRegistry;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

/**
 * Abstraction layer for LLM providers (OpenAI, Anthropic, etc.).
 *
 * <p>Implementations wrap provider-specific SDKs and translate between
 * provider-specific types and the common {@link ContextMessage}/{@link LlmResponse}
 * model used throughout the application.</p>
 */
public interface LlmProvider {

    /**
     * Send a chat request to the LLM provider.
     *
     * @param context ordered list of context messages
     * @param tools   the tool registry for building tool definitions
     * @return the parsed {@link LlmResponse}
     * @throws IOException if the API call fails
     */
    LlmResponse chat(List<ContextMessage> context, ToolRegistry tools) throws IOException;

    /**
     * Send a chat request to the LLM provider with optional streamed text updates.
     *
     * <p>The default implementation falls back to non-streaming behavior and invokes
     * {@link #chat(List, ToolRegistry)}.</p>
     *
     * @param context     ordered list of context messages
     * @param tools       the tool registry for building tool definitions
     * @param onTextDelta callback that receives progressively accumulated assistant text
     * @return the parsed {@link LlmResponse}
     * @throws IOException if the API call fails
     */
    default LlmResponse chat(
            List<ContextMessage> context,
            ToolRegistry tools,
            Consumer<String> onTextDelta) throws IOException {
        return chat(context, tools);
    }

    /**
     * Returns the provider name for identification/logging.
     *
     * @return the provider name (e.g., "openai", "anthropic")
     */
    String getProviderName();
}
