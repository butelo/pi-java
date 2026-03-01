package com.example.pijava.agent;

import com.example.pijava.agent.tool.ToolRegistry;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

/**
 * LLM client that delegates to a provider-specific implementation.
 *
 * <p>This class provides a unified interface for interacting with different
 * LLM providers (OpenAI, Anthropic, etc.) through the {@link LlmProvider}
 * abstraction.</p>
 */
public class LlmClient {

    private final LlmProvider provider;
    private final ToolRegistry tools;

    /**
     * Create a client with the given provider.
     *
     * @param provider the LLM provider implementation
     * @param tools    the tool registry for building tool definitions
     */
    public LlmClient(LlmProvider provider, ToolRegistry tools) {
        this.provider = provider;
        this.tools = tools;
    }

    /**
     * Send the conversation context to the LLM and return the parsed response.
     *
     * @param context ordered list of context messages
     * @return the parsed {@link LlmResponse}
     * @throws IOException if the API call fails
     */
    public LlmResponse chat(List<ContextMessage> context) throws IOException {
        return chat(context, null);
    }

    /**
     * Send the conversation context to the LLM and return the parsed response,
     * optionally receiving streamed assistant text updates.
     *
     * @param context     ordered list of context messages
     * @param onTextDelta callback that receives progressively accumulated assistant text
     * @return the parsed {@link LlmResponse}
     * @throws IOException if the API call fails
     */
    public LlmResponse chat(
            List<ContextMessage> context,
            Consumer<String> onTextDelta) throws IOException {
        return provider.chat(context, tools, onTextDelta);
    }

    /**
     * Returns the provider name for identification/logging.
     *
     * @return the provider name (e.g., "openai", "anthropic")
     */
    public String getProviderName() {
        return provider.getProviderName();
    }
}