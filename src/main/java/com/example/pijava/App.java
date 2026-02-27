///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.7.6
//DEPS com.googlecode.lanterna:lanterna:3.1.1
//DEPS com.google.code.gson:gson:2.11.0
//DEPS ch.qos.logback:logback-classic:1.5.12
//JAVA 17+

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
public class App implements Callable<Integer> {

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
        int exitCode = new CommandLine(new App()).execute(args);
        System.exit(exitCode);
    }
}
