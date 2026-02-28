package com.example.pijava.agent;

import com.example.pijava.agent.tool.ToolRegistry;
import com.google.gson.JsonParser;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Core agent loop: accepts user input, calls the LLM, executes any
 * requested tools, and returns the final assistant response.
 *
 * <p>The loop will keep calling the LLM until it produces a plain-text
 * reply (no more tool calls) or the safety limit is reached.</p>
 */
public class AgentLoop {

    private static final Logger LOG =
            LoggerFactory.getLogger(AgentLoop.class);

    /** Maximum number of LLM ↔ tool round-trips per user message. */
    private static final int MAX_TOOL_ROUNDS = 10;

    private final LlmClient client;
    private final ContextManager context;
    private final ToolRegistry tools;

    /**
     * Create an agent loop.
     *
     * @param client  the LLM HTTP client
     * @param context the conversation context manager
     * @param tools   the registry of available tools
     */
    public AgentLoop(LlmClient client, ContextManager context,
                     ToolRegistry tools) {
        this.client = client;
        this.context = context;
        this.tools = tools;
    }

    /**
     * Process a single user message through the agent loop.
     *
     * @param userInput the user's text
     * @return the assistant's final text response
     * @throws IOException if an LLM API call fails
     */
    public String process(String userInput) throws IOException {
        context.addUser(userInput);

        for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
            var response = client.chat(context.messages(), tools);

            if (!response.hasToolCalls()) {
                var text = response.content() != null
                        ? response.content() : "(no response)";
                context.addAssistant(text);
                return text;
            }

            context.addAssistantToolCalls(
                    response.content(), response.toolCalls());
            executeToolCalls(response);
        }

        var fallback = "Stopped after " + MAX_TOOL_ROUNDS + " tool rounds.";
        context.addAssistant(fallback);
        return fallback;
    }

    private void executeToolCalls(LlmResponse response) {
        for (var call : response.toolCalls()) {
            var name = call.function().name();
            LOG.info("Tool call: {} ({})", name, call.id());

            var args = JsonParser.parseString(call.function().arguments())
                    .getAsJsonObject();
            var result = tools.execute(name, args);

            LOG.info("Tool result for {}: {} chars", name, result.length());
            context.addToolResult(call.id(), result);
        }
    }
}
