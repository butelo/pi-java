package com.example.pijava.agent.provider;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.ToolResultBlockParam;
import com.anthropic.models.messages.ToolUseBlockParam;
import com.example.pijava.agent.ContextMessage;
import com.example.pijava.agent.LlmProvider;
import com.example.pijava.agent.LlmResponse;
import com.example.pijava.agent.tool.ToolRegistry;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Anthropic SDK implementation of {@link LlmProvider}.
 *
 * <p>Uses the Messages API with tool support. Note that Anthropic's API
 * structure differs from OpenAI's - system messages are passed separately
 * and tool results use a different format.</p>
 */
public class AnthropicLlmProvider implements LlmProvider {

    private static final Logger LOG = LoggerFactory.getLogger(AnthropicLlmProvider.class);

    private final AnthropicClient client;
    private final Optional<Model> model;
    private final String modelName;

    /**
     * Create a provider targeting the Anthropic API.
     *
     * @param apiKey API key for authentication
     * @param model  model identifier (e.g. {@code claude-sonnet-4-5-20250929})
     */
    public AnthropicLlmProvider(String apiKey, String model) {
        this.model = resolveModel(model);
        this.modelName = model;
        this.client = AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }

    @Override
    public LlmResponse chat(List<ContextMessage> context, ToolRegistry tools) throws IOException {
        try {
            var paramsBuilder = MessageCreateParams.builder();

            // Set model
            model.ifPresentOrElse(
                    paramsBuilder::model,
                    () -> paramsBuilder.model(modelName)
            );

            // Anthropic requires max_tokens
            paramsBuilder.maxTokens(4096L);

            // Extract system message (Anthropic handles system separately)
            String systemPrompt = null;
            List<ContextMessage> conversationMessages = new ArrayList<>();
            for (var msg : context) {
                if ("system".equals(msg.role())) {
                    systemPrompt = msg.content();
                } else {
                    conversationMessages.add(msg);
                }
            }

            if (systemPrompt != null) {
                paramsBuilder.system(systemPrompt);
            }

            // Convert messages to Anthropic format
            // Anthropic alternates user/assistant messages
            List<MessageParam> messages = buildMessageParams(conversationMessages);
            for (var msg : messages) {
                paramsBuilder.addMessage(msg);
            }

            // Add tool definitions if any
            var toolDefs = tools.toAnthropicTools();
            for (var tool : toolDefs) {
                paramsBuilder.addTool(tool);
            }

            LOG.debug("Sending chat request with {} messages", messages.size());
            var message = client.messages().create(paramsBuilder.build());
            return parseResponse(message);

        } catch (Exception e) {
            LOG.error("Anthropic API error: {}", e.getMessage(), e);
            throw new IOException("Anthropic API error: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return "anthropic";
    }

    private static Optional<Model> resolveModel(String modelName) {
        return Optional.ofNullable(switch (modelName.toLowerCase(Locale.ROOT)) {
            case "claude-sonnet-4-5-20250929" -> Model.CLAUDE_SONNET_4_5_20250929;
            case "claude-sonnet-4-20250514" -> Model.CLAUDE_SONNET_4_20250514;
            case "claude-3-haiku-20240307" -> Model.CLAUDE_3_HAIKU_20240307;
            default -> null; // Use as string for custom models
        });
    }

    private List<MessageParam> buildMessageParams(List<ContextMessage> messages) {
        var result = new ArrayList<MessageParam>();

        for (var msg : messages) {
            switch (msg.role()) {
                case "user" -> {
                    var contentBlocks = new ArrayList<ContentBlockParam>();
                    var textBlock = com.anthropic.models.messages.TextBlockParam.builder()
                            .text(msg.content())
                            .build();
                    contentBlocks.add(ContentBlockParam.ofText(textBlock));
                    result.add(MessageParam.builder()
                            .role(MessageParam.Role.USER)
                            .contentOfBlockParams(contentBlocks)
                            .build());
                }

                case "assistant" -> {
                    var contentBlocks = new ArrayList<ContentBlockParam>();

                    // Add text content if present
                    if (msg.content() != null && !msg.content().isEmpty()) {
                        var textBlock = com.anthropic.models.messages.TextBlockParam.builder()
                                .text(msg.content())
                                .build();
                        contentBlocks.add(ContentBlockParam.ofText(textBlock));
                    }

                    // Add tool calls
                    if (msg.toolCalls() != null && !msg.toolCalls().isEmpty()) {
                        for (var tc : msg.toolCalls()) {
                            var inputMap = parseJsonToMap(tc.function().arguments());
                            var inputBuilder = ToolUseBlockParam.Input.builder();
                            for (var entry : inputMap.entrySet()) {
                                inputBuilder.putAdditionalProperty(entry.getKey(), 
                                    objectToJsonValue(entry.getValue()));
                            }
                            var toolUseBlock = ToolUseBlockParam.builder()
                                    .id(tc.id())
                                    .name(tc.function().name())
                                    .input(inputBuilder.build())
                                    .build();
                            contentBlocks.add(ContentBlockParam.ofToolUse(toolUseBlock));
                        }
                    }

                    if (!contentBlocks.isEmpty()) {
                        result.add(MessageParam.builder()
                                .role(MessageParam.Role.ASSISTANT)
                                .contentOfBlockParams(contentBlocks)
                                .build());
                    }
                }

                case "tool" -> {
                    // Anthropic tool results are sent as user messages with tool_result blocks
                    var contentBlocks = new ArrayList<ContentBlockParam>();
                    var toolResultBlock = ToolResultBlockParam.builder()
                            .toolUseId(msg.toolCallId())
                            .content(msg.content())
                            .build();
                    contentBlocks.add(ContentBlockParam.ofToolResult(toolResultBlock));
                    result.add(MessageParam.builder()
                            .role(MessageParam.Role.USER)
                            .contentOfBlockParams(contentBlocks)
                            .build());
                }

                default -> throw new IllegalArgumentException("Unknown message role: " + msg.role());
            }
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseJsonToMap(String json) {
        try {
            var gson = new com.google.gson.Gson();
            var obj = gson.fromJson(json, com.google.gson.JsonObject.class);
            Map<String, Object> result = new LinkedHashMap<>();
            for (String key : obj.keySet()) {
                result.put(key, gson.fromJson(obj.get(key), Object.class));
            }
            return result;
        } catch (Exception e) {
            return Map.of();
        }
    }

    private static com.anthropic.core.JsonValue objectToJsonValue(Object obj) {
        if (obj == null) {
            return com.anthropic.core.JsonValue.from(null);
        } else if (obj instanceof Boolean b) {
            return com.anthropic.core.JsonValue.from(b);
        } else if (obj instanceof Number n) {
            return com.anthropic.core.JsonValue.from(n);
        } else if (obj instanceof String s) {
            return com.anthropic.core.JsonValue.from(s);
        } else if (obj instanceof java.util.List<?> list) {
            return com.anthropic.core.JsonValue.from(list.stream()
                    .map(AnthropicLlmProvider::objectToJsonValue)
                    .toList());
        } else if (obj instanceof Map<?, ?> map) {
            Map<String, com.anthropic.core.JsonValue> result = new LinkedHashMap<>();
            for (var entry : map.entrySet()) {
                result.put(entry.getKey().toString(), objectToJsonValue(entry.getValue()));
            }
            return com.anthropic.core.JsonValue.from(result);
        } else {
            return com.anthropic.core.JsonValue.from(obj.toString());
        }
    }

    private LlmResponse parseResponse(Message message) {
        String content = "";
        List<ContextMessage.ToolCallData> toolCalls = new ArrayList<>();

        for (var block : message.content()) {
            if (block.isText()) {
                content = block.asText().text();
            } else if (block.isToolUse()) {
                var toolUse = block.asToolUse();
                toolCalls.add(new ContextMessage.ToolCallData(
                        toolUse.id(),
                        "function",
                        new ContextMessage.FunctionData(
                                toolUse.name(),
                                toolUse._input().toString())));
            }
        }

        LOG.debug("LLM response: content={}, hasToolCalls={}",
                content.isEmpty() ? "(empty)" : content.substring(0, Math.min(100, content.length())) + "...",
                !toolCalls.isEmpty());

        return new LlmResponse(content, toolCalls);
    }
}
