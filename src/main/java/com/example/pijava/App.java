///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.7.6
//DEPS org.jline:jline:3.26.2
//DEPS com.google.code.gson:gson:2.11.0
//DEPS ch.qos.logback:logback-classic:1.5.12
//DEPS com.github.spotbugs:spotbugs-annotations:4.8.6
//SOURCES model/Message.java
//SOURCES ui/component/Component.java
//SOURCES ui/component/Layout.java
//SOURCES ui/component/RenderContext.java
//SOURCES ui/component/HeaderComponent.java
//SOURCES ui/component/MessageListComponent.java
//SOURCES ui/component/InputComponent.java
//SOURCES ui/component/StatusBarComponent.java
//SOURCES ui/input/Action.java
//SOURCES ui/input/InputHandler.java
//SOURCES ui/screen/MainScreen.java
//SOURCES agent/ContextMessage.java
//SOURCES agent/ContextManager.java
//SOURCES agent/LlmClient.java
//SOURCES agent/LlmResponse.java
//SOURCES agent/AgentLoop.java
//SOURCES agent/tool/Tool.java
//SOURCES agent/tool/ToolRegistry.java
//SOURCES agent/tool/ReadFileTool.java
//SOURCES agent/tool/ListFilesTool.java
//SOURCES agent/tool/RunCommandTool.java
//JAVA 21+

package com.example.pijava;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;

import com.example.pijava.agent.AgentLoop;
import com.example.pijava.agent.ContextManager;
import com.example.pijava.agent.LlmClient;
import com.example.pijava.agent.tool.ListFilesTool;
import com.example.pijava.agent.tool.ReadFileTool;
import com.example.pijava.agent.tool.RunCommandTool;
import com.example.pijava.agent.tool.ToolRegistry;
import com.example.pijava.ui.screen.MainScreen;
import java.util.concurrent.Callable;
import picocli.CommandLine;

/**
 * CLI entry point. Parses arguments via picocli and delegates to
 * {@link MainScreen} for the interactive TUI.
 */
@Command(
    name = "pi-java",
    mixinStandardHelpOptions = true,
    version = "pi-java 1.0",
    description = "A TUI code agent in Java"
)
public class App implements Callable<Integer> {

    @Option(names = {"-v", "--verbose"}, description = "Enable verbose output")
    private boolean verbose;

    @Option(names = {"--api-key"}, description = "OpenAI API key (default: $OPENAI_API_KEY env var)")
    private String apiKey;

    @Option(names = {"-m", "--model"}, description = "LLM model to use",
            defaultValue = "gpt-4o")
    private String model;

    @Override
    public Integer call() throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = System.getenv("OPENAI_API_KEY");
        }

        AgentLoop agent = null;
        if (apiKey != null && !apiKey.isBlank()) {
            var tools = new ToolRegistry();
            tools.register(new ReadFileTool());
            tools.register(new ListFilesTool());
            tools.register(new RunCommandTool());
            agent = new AgentLoop(
                    new LlmClient(apiKey, model),
                    new ContextManager(), tools);
        }

        if (verbose) {
            System.out.printf("Starting pi-java (model=%s, agent=%s)%n",
                    model, agent != null ? "enabled" : "echo-mode");
        }

        new MainScreen(agent).run();
        return 0;
    }

    public static void main(String[] args) {
        var exitCode = new CommandLine(new App()).execute(args);
        System.exit(exitCode);
    }
}
