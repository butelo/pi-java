package com.example.pijava.agent.tool;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.openai.core.JsonValue;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.models.chat.completions.ChatCompletionFunctionTool;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of {@link Tool}s available to the agent.
 *
 * <p>Handles tool registration, JSON-schema generation for the LLM API,
 * and dispatching execution by tool name.</p>
 */
public class ToolRegistry {

    private final Map<String, Tool> tools = new LinkedHashMap<>();

    /**
     * Register a tool. Overwrites any previous tool with the same name.
     *
     * @param tool the tool to register
     */
    public void register(Tool tool) {
        tools.put(tool.name(), tool);
    }

    /**
     * Execute a tool by name.
     *
     * @param name      the tool name
     * @param arguments parsed JSON arguments
     * @return the tool's output, or an error string if the tool is unknown
     */
    public String execute(String name, JsonObject arguments) {
        var tool = tools.get(name);
        if (tool == null) {
            return "Error: unknown tool '" + name + "'";
        }
        try {
            return tool.execute(arguments);
        } catch (RuntimeException e) {
            return "Error executing " + name + ": " + e.getMessage();
        }
    }

    /**
     * Build the {@code tools} JSON array expected by the OpenAI API (legacy format).
     *
     * @return a {@link JsonArray} of tool definitions
     */
    public JsonArray toJsonSchema() {
        var array = new JsonArray();
        for (var tool : tools.values()) {
            var fn = new JsonObject();
            fn.addProperty("name", tool.name());
            fn.addProperty("description", tool.description());
            fn.add("parameters", tool.parametersSchema());

            var wrapper = new JsonObject();
            wrapper.addProperty("type", "function");
            wrapper.add("function", fn);
            array.add(wrapper);
        }
        return array;
    }

    /**
     * Build SDK {@link ChatCompletionFunctionTool} objects for the OpenAI SDK.
     *
     * @return a list of ChatCompletionFunctionTool definitions
     */
    public List<ChatCompletionFunctionTool> toSdkTools() {
        return tools.values().stream()
                .map(tool -> {
                    var jsonSchema = tool.parametersSchema();
                    
                    // Build properties map from JSON schema
                    Map<String, JsonValue> propertiesMap = new LinkedHashMap<>();
                    if (jsonSchema.has("properties")) {
                        var props = jsonSchema.getAsJsonObject("properties");
                        for (String key : props.keySet()) {
                            propertiesMap.put(key, convertToJsonValue(props.get(key)));
                        }
                    }
                    
                    // Build required list
                    List<String> requiredList = List.of();
                    if (jsonSchema.has("required")) {
                        requiredList = jsonArrayToStringList(jsonSchema.getAsJsonArray("required"));
                    }
                    
                    // Build additionalProperties
                    boolean additionalProps = true;
                    if (jsonSchema.has("additionalProperties")) {
                        additionalProps = jsonSchema.get("additionalProperties").getAsBoolean();
                    }
                    
                    return ChatCompletionFunctionTool.builder()
                            .function(FunctionDefinition.builder()
                                    .name(tool.name())
                                    .description(tool.description())
                                    .parameters(FunctionParameters.builder()
                                            .putAdditionalProperty("type", JsonValue.from("object"))
                                            .putAdditionalProperty("properties", JsonValue.from(propertiesMap))
                                            .putAdditionalProperty("required", JsonValue.from(requiredList))
                                            .putAdditionalProperty("additionalProperties", JsonValue.from(additionalProps))
                                            .build())
                                    .build())
                            .build();
                })
                .toList();
    }

    private static JsonValue convertToJsonValue(com.google.gson.JsonElement element) {
        if (element.isJsonNull()) {
            return JsonValue.from(null);
        } else if (element.isJsonPrimitive()) {
            var primitive = element.getAsJsonPrimitive();
            if (primitive.isBoolean()) {
                return JsonValue.from(primitive.getAsBoolean());
            } else if (primitive.isNumber()) {
                return JsonValue.from(primitive.getAsNumber());
            } else {
                return JsonValue.from(primitive.getAsString());
            }
        } else if (element.isJsonArray()) {
            var array = element.getAsJsonArray();
            List<JsonValue> list = new java.util.ArrayList<>();
            for (var item : array) {
                list.add(convertToJsonValue(item));
            }
            return JsonValue.from(list);
        } else if (element.isJsonObject()) {
            var obj = element.getAsJsonObject();
            Map<String, JsonValue> map = new LinkedHashMap<>();
            for (String key : obj.keySet()) {
                map.put(key, convertToJsonValue(obj.get(key)));
            }
            return JsonValue.from(map);
        }
        return JsonValue.from(null);
    }

    private static List<String> jsonArrayToStringList(JsonArray array) {
        return array.asList().stream()
                .map(e -> e.getAsString())
                .toList();
    }
}