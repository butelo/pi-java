package com.example.pijava.agent;

import com.example.pijava.agent.tool.ToolRegistry;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessageFunctionToolCall;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionMessageToolCall;
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam;
import com.openai.models.chat.completions.ChatCompletionToolMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OpenAI SDK client for chat-completion APIs.
 *
 * <p>Sends the full conversation context plus tool definitions on every
 * call and parses the response into an {@link LlmResponse}.</p>
 */
public class LlmClient {

    private static final Logger LOG = LoggerFactory.getLogger(LlmClient.class);

    private final OpenAIClient client;
    private final Optional<ChatModel> chatModel;
    private final String modelName;
    private final ToolRegistry tools;

    /**
     * Create a client targeting the OpenAI API.
     *
     * @param apiKey bearer token for authentication
     * @param model  model identifier (e.g. {@code gpt-4o})
     * @param tools  the tool registry for building tool definitions
     */
    public LlmClient(String apiKey, String model, ToolRegistry tools) {
        this.chatModel = resolveModel(model);
        this.modelName = model;
        this.tools = tools;
        this.client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }

    /**
     * Create a client with a custom base URL (for OpenAI-compatible APIs).
     *
     * @param apiKey  bearer token for authentication
     * @param baseUrl base URL for the API endpoint
     * @param model   model identifier
     * @param tools   the tool registry for building tool definitions
     */
    public LlmClient(String apiKey, String baseUrl, String model, ToolRegistry tools) {
        this.chatModel = resolveModel(model);
        this.modelName = model;
        this.tools = tools;
        this.client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * Send the conversation context to the LLM and return the parsed response.
     *
     * @param context ordered list of context messages
     * @return the parsed {@link LlmResponse}
     * @throws IOException if the API call fails
     */
    public LlmResponse chat(List<ContextMessage> context) throws IOException {
        try {
            var paramsBuilder = ChatCompletionCreateParams.builder();

            // Set model - use ChatModel enum if known, otherwise use string
            chatModel.ifPresentOrElse(
                    paramsBuilder::model,
                    () -> paramsBuilder.model(modelName)
            );

            // Add all messages from context
            for (var msg : context) {
                paramsBuilder.addMessage(buildMessageParam(msg));
            }

            // Add tool definitions if any
            var toolDefs = tools.toSdkTools();
            for (var tool : toolDefs) {
                paramsBuilder.addTool(tool);
            }

            LOG.debug("Sending chat request with {} messages", context.size());
            var chatCompletion = client.chat().completions().create(paramsBuilder.build());
            return parseResponse(chatCompletion);

        } catch (Exception e) {
            LOG.error("LLM API error: {}", e.getMessage(), e);
            throw new IOException("LLM API error: " + e.getMessage(), e);
        }
    }

    private static Optional<ChatModel> resolveModel(String modelName) {
        return Optional.ofNullable(switch (modelName.toLowerCase(Locale.ROOT)) {
            case "gpt-4o" -> ChatModel.GPT_4O;
            case "gpt-4o-mini" -> ChatModel.GPT_4O_MINI;
            case "gpt-4-turbo" -> ChatModel.GPT_4_TURBO;
            case "gpt-4" -> ChatModel.GPT_4;
            case "gpt-3.5-turbo" -> ChatModel.GPT_3_5_TURBO;
            case "o1" -> ChatModel.O1;
            case "o1-mini" -> ChatModel.O1_MINI;
            case "o1-preview" -> ChatModel.O1_PREVIEW;
            case "o3-mini" -> ChatModel.O3_MINI;
            default -> null; // Use as string for custom models
        });
    }

    private ChatCompletionMessageParam buildMessageParam(ContextMessage msg) {
        return switch (msg.role()) {
            case "system" -> ChatCompletionMessageParam.ofSystem(
                    ChatCompletionSystemMessageParam.builder()
                            .content(msg.content())
                            .build());

            case "user" -> ChatCompletionMessageParam.ofUser(
                    ChatCompletionUserMessageParam.builder()
                            .content(msg.content())
                            .build());

            case "assistant" -> {
                var assistantBuilder = ChatCompletionAssistantMessageParam.builder();

                if (msg.content() != null) {
                    assistantBuilder.content(msg.content());
                }

                if (msg.toolCalls() != null && !msg.toolCalls().isEmpty()) {
                    var sdkToolCalls = new ArrayList<ChatCompletionMessageToolCall>();
                    for (var tc : msg.toolCalls()) {
                        var functionToolCall = ChatCompletionMessageFunctionToolCall.builder()
                                .id(tc.id())
                                .function(ChatCompletionMessageFunctionToolCall.Function.builder()
                                        .name(tc.function().name())
                                        .arguments(tc.function().arguments())
                                        .build())
                                .build();
                        sdkToolCalls.add(ChatCompletionMessageToolCall.ofFunction(functionToolCall));
                    }
                    assistantBuilder.toolCalls(sdkToolCalls);
                }

                yield ChatCompletionMessageParam.ofAssistant(assistantBuilder.build());
            }

            case "tool" -> ChatCompletionMessageParam.ofTool(
                    ChatCompletionToolMessageParam.builder()
                            .toolCallId(msg.toolCallId())
                            .content(msg.content())
                            .build());

            default -> throw new IllegalArgumentException("Unknown message role: " + msg.role());
        };
    }

    private LlmResponse parseResponse(ChatCompletion completion) {
        if (completion.choices().isEmpty()) {
            return new LlmResponse("No response from LLM.", List.of());
        }

        var choice = completion.choices().get(0);
        var message = choice.message();

        String content = message.content().orElse("");

        List<ContextMessage.ToolCallData> toolCalls = List.of();
        if (message.toolCalls().isPresent() && !message.toolCalls().get().isEmpty()) {
            var calls = new ArrayList<ContextMessage.ToolCallData>();
            for (var tc : message.toolCalls().get()) {
                // Tool calls can be function calls or other types
                if (tc.isFunction()) {
                    var fn = tc.asFunction();
                    calls.add(new ContextMessage.ToolCallData(
                            fn.id(),
                            "function",
                            new ContextMessage.FunctionData(
                                    fn.function().name(),
                                    fn.function().arguments())));
                }
            }
            toolCalls = calls;
        }

        LOG.debug("LLM response: content={}, hasToolCalls={}",
                content.isEmpty() ? "(empty)" : content.substring(0, Math.min(100, content.length())) + "...",
                !toolCalls.isEmpty());

        return new LlmResponse(content, toolCalls);
    }
}