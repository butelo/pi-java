package com.example.pijava.agent;

import com.example.pijava.agent.tool.ToolRegistry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP client for OpenAI-compatible chat-completion APIs.
 *
 * <p>Sends the full conversation context plus tool definitions on every
 * call and parses the response into an {@link LlmResponse}.</p>
 */
public class LlmClient {

    private static final Logger LOG = LoggerFactory.getLogger(LlmClient.class);
    private static final String DEFAULT_URL =
            "https://api.openai.com/v1/chat/completions";
    private static final int CONNECT_TIMEOUT_SECS = 30;
    private static final int READ_TIMEOUT_SECS = 120;

    private final String apiKey;
    private final String model;
    private final HttpClient http;
    private final Gson gson;

    /**
     * Create a client targeting the OpenAI API.
     *
     * @param apiKey bearer token for authentication
     * @param model  model identifier (e.g. {@code gpt-4o})
     */
    public LlmClient(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECS))
                .build();
        this.gson = new GsonBuilder().create();
    }

    /**
     * Send the conversation context to the LLM and return the parsed response.
     *
     * @param context ordered list of context messages
     * @param tools   available tool definitions
     * @return the parsed {@link LlmResponse}
     * @throws IOException if the HTTP call fails or the API returns an error
     */
    public LlmResponse chat(List<ContextMessage> context, ToolRegistry tools)
            throws IOException {

        var body = buildRequestBody(context, tools);
        LOG.debug("LLM request body: {}", body);

        var request = HttpRequest.newBuilder()
                .uri(URI.create(DEFAULT_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(READ_TIMEOUT_SECS))
                .build();

        try {
            var response = http.send(request,
                    HttpResponse.BodyHandlers.ofString());
            LOG.debug("LLM response status: {}", response.statusCode());

            if (response.statusCode() != 200) {
                throw new IOException("LLM API error "
                        + response.statusCode() + ": " + response.body());
            }
            return parseResponse(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("LLM request interrupted", e);
        }
    }

    // ---- request building -------------------------------------------------

    private String buildRequestBody(List<ContextMessage> context,
                                    ToolRegistry tools) {
        var root = new JsonObject();
        root.addProperty("model", model);
        root.add("messages", buildMessages(context));

        var toolDefs = tools.toJsonSchema();
        if (toolDefs.size() > 0) {
            root.add("tools", toolDefs);
        }
        return gson.toJson(root);
    }

    private JsonArray buildMessages(List<ContextMessage> context) {
        var array = new JsonArray();
        for (var msg : context) {
            var obj = new JsonObject();
            obj.addProperty("role", msg.role());
            addIfPresent(obj, "content", msg.content());
            addIfPresent(obj, "tool_call_id", msg.toolCallId());
            if (msg.toolCalls() != null && !msg.toolCalls().isEmpty()) {
                obj.add("tool_calls", gson.toJsonTree(msg.toolCalls()));
            }
            array.add(obj);
        }
        return array;
    }

    private static void addIfPresent(JsonObject obj, String key, String val) {
        if (val != null) {
            obj.addProperty(key, val);
        }
    }

    // ---- response parsing -------------------------------------------------

    private LlmResponse parseResponse(String body) {
        var root = JsonParser.parseString(body).getAsJsonObject();
        var choices = root.getAsJsonArray("choices");
        if (choices == null || choices.isEmpty()) {
            return new LlmResponse("No response from LLM.", null);
        }

        var message = choices.get(0).getAsJsonObject()
                .getAsJsonObject("message");

        String content = null;
        if (message.has("content") && !message.get("content").isJsonNull()) {
            content = message.get("content").getAsString();
        }

        List<ContextMessage.ToolCallData> toolCalls = null;
        if (message.has("tool_calls")
                && !message.get("tool_calls").isJsonNull()) {
            toolCalls = gson.fromJson(
                    message.getAsJsonArray("tool_calls"),
                    new TypeToken<List<ContextMessage.ToolCallData>>() { }
                            .getType());
        }
        return new LlmResponse(content, toolCalls);
    }
}
