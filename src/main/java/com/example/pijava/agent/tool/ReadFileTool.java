package com.example.pijava.agent.tool;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tool that reads the contents of a file and returns them as text.
 */
public class ReadFileTool implements Tool {

    private static final int MAX_CHARS = 10_000;

    @Override
    public String name() {
        return "read_file";
    }

    @Override
    public String description() {
        return "Read the contents of a file at the given path.";
    }

    @Override
    public JsonObject parametersSchema() {
        var pathProp = new JsonObject();
        pathProp.addProperty("type", "string");
        pathProp.addProperty("description",
                "Absolute or relative file path to read");

        var properties = new JsonObject();
        properties.add("path", pathProp);

        var required = new JsonArray();
        required.add("path");

        var schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", properties);
        schema.add("required", required);
        return schema;
    }

    @Override
    public String execute(JsonObject arguments) {
        var filePath = arguments.get("path").getAsString();
        try {
            var content = Files.readString(Path.of(filePath));
            if (content.length() > MAX_CHARS) {
                return content.substring(0, MAX_CHARS)
                        + "\n... (truncated, file has "
                        + content.length() + " chars)";
            }
            return content;
        } catch (IOException e) {
            return "Error reading file: " + e.getMessage();
        }
    }
}
