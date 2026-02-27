package com.example.pijava;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Command(
    name = "pi-java",
    mixinStandardHelpOptions = true,
    version = "pi-java 1.0",
    description = "A TUI code agent in Java"
)
public class PiJavaApp implements Callable<Integer> {

    @Option(names = {"-v", "--verbose"}, description = "Enable verbose output")
    private boolean verbose;

    @Option(names = {"-p", "--port"}, description = "Port to listen on")
    private int port = 8080;

    @Override
    public Integer call() throws Exception {
        if (verbose) {
            System.out.println("Starting pi-java in verbose mode on port " + port);
        }
        
        System.out.println("pi-java - TUI Code Agent");
        System.out.println("========================");
        System.out.println("Use --help for available options");
        
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new PiJavaApp()).execute(args);
        System.exit(exitCode);
    }
}
