package com.example.pijava.agent;

import com.example.pijava.agent.tool.ToolRegistry;
import java.io.IOException;
import java.util.List;

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
     * Returns the provider name for identification/logging.
     *
     * @return the provider name (e.g., "openai", "anthropic")
     */
    String getProviderName();
}
