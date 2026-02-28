package com.example.pijava.agent.tool;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
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
     * Build the {@code tools} JSON array expected by the OpenAI API.
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
}
