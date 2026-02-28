package com.example.pijava.agent.tool;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

/**
 * Tool that lists files and directories at a given path.
 */
public class ListFilesTool implements Tool {

    @Override
    public String name() {
        return "list_files";
    }

    @Override
    public String description() {
        return "List files and directories at the given path.";
    }

    @Override
    public JsonObject parametersSchema() {
        var pathProp = new JsonObject();
        pathProp.addProperty("type", "string");
        pathProp.addProperty("description", "Directory path to list");

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
        var dirPath = arguments.get("path").getAsString();
        try (var entries = Files.list(Path.of(dirPath))) {
            return entries
                    .map(p -> (Files.isDirectory(p) ? "[DIR]  " : "[FILE] ")
                            + p.getFileName())
                    .sorted()
                    .collect(Collectors.joining("\n"));
        } catch (IOException e) {
            return "Error listing directory: " + e.getMessage();
        }
    }
}
