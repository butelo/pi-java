package com.example.pijava.agent.tool;

import com.google.gson.JsonObject;

/**
 * A tool that the LLM agent can invoke during the conversation.
 *
 * <p>Implementations describe themselves via {@link #name()},
 * {@link #description()}, and {@link #parametersSchema()}, and perform
 * work in {@link #execute(JsonObject)}.</p>
 */
public interface Tool {

    /** Unique name used by the LLM to reference this tool. */
    String name();

    /** Human-readable description included in the LLM's tool catalogue. */
    String description();

    /** JSON Schema object describing the parameters this tool accepts. */
    JsonObject parametersSchema();

    /**
     * Execute the tool with the given arguments.
     *
     * @param arguments parsed JSON arguments matching {@link #parametersSchema()}
     * @return a plain-text result that will be sent back to the LLM
     */
    String execute(JsonObject arguments);
}
