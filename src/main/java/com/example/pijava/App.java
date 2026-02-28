///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.7.6
//DEPS com.googlecode.lanterna:lanterna:3.1.1
//DEPS com.google.code.gson:gson:2.11.0
//DEPS ch.qos.logback:logback-classic:1.5.12
//SOURCES model/Message.java
//SOURCES ui/component/Component.java
//SOURCES ui/component/RenderContext.java
//SOURCES ui/component/HeaderComponent.java
//SOURCES ui/component/MessageListComponent.java
//SOURCES ui/component/InputComponent.java
//SOURCES ui/component/StatusBarComponent.java
//SOURCES ui/input/Action.java
//SOURCES ui/input/InputHandler.java
//SOURCES ui/screen/MainScreen.java
//JAVA 21+

package com.example.pijava;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;

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

    @Option(names = {"-p", "--port"}, description = "Port to listen on")
    private int port = 8080;

    @Override
    public Integer call() throws Exception {
        if (verbose) {
            System.out.printf("Starting pi-java in verbose mode on port %d%n", port);
        }

        new MainScreen().run();
        return 0;
    }

    public static void main(String[] args) {
        var exitCode = new CommandLine(new App()).execute(args);
        System.exit(exitCode);
    }
}
