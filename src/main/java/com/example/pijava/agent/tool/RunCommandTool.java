package com.example.pijava.agent.tool;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Tool that runs a shell command and returns its combined stdout/stderr.
 *
 * <p>A safety timeout prevents runaway processes from blocking the agent
 * indefinitely.</p>
 */
public class RunCommandTool implements Tool {

    private static final int TIMEOUT_SECONDS = 30;
    private static final int MAX_OUTPUT_CHARS = 10_000;

    @Override
    public String name() {
        return "run_command";
    }

    @Override
    public String description() {
        return "Run a shell command and return its output. "
                + "Use for compilation, testing, or inspecting the system.";
    }

    @Override
    public JsonObject parametersSchema() {
        var commandProp = new JsonObject();
        commandProp.addProperty("type", "string");
        commandProp.addProperty("description",
                "The shell command to execute");

        var properties = new JsonObject();
        properties.add("command", commandProp);

        var required = new JsonArray();
        required.add("command");

        var schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", properties);
        schema.add("required", required);
        return schema;
    }

    @Override
    public String execute(JsonObject arguments) {
        var command = arguments.get("command").getAsString();
        Process process;
        try {
            process = new ProcessBuilder("sh", "-c", command)
                    .redirectErrorStream(true)
                    .start();
        } catch (IOException e) {
            return "Error running command: " + e.getMessage();
        }

        // Read output on a virtual thread to prevent buffer deadlocks
        var input = process.getInputStream();
        var outputFuture = new CompletableFuture<String>();
        Thread.startVirtualThread(() -> {
            try {
                outputFuture.complete(
                        new String(input.readAllBytes(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                outputFuture.complete("(error reading output)");
            } finally {
                try {
                    input.close();
                } catch (IOException e) {
                    // Log but ignore close errors - we're in a finally anyway
                    System.err.println("Warning: error closing process input: " + e.getMessage());
                }
            }
        });

        try {
            var completed = process.waitFor(
                    TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!completed) {
                process.destroyForcibly();
                var partial = outputFuture.getNow("");
                return partial + "\n(timed out after "
                        + TIMEOUT_SECONDS + "s)";
            }

            var output = outputFuture.get(5, TimeUnit.SECONDS);
            return formatOutput(output, process.exitValue());

        } catch (Exception e) {
            return "Error running command: " + e.getMessage();
        } finally {
            process.destroyForcibly();
        }
    }

    private static String formatOutput(String output, int exitCode) {
        var result = output;
        if (result.length() > MAX_OUTPUT_CHARS) {
            result = result.substring(0, MAX_OUTPUT_CHARS)
                    + "\n... (truncated)";
        }
        return result.isEmpty()
                ? "(no output, exit code " + exitCode + ")"
                : result + "\n(exit code " + exitCode + ")";
    }
}
