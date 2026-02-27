# pi-java

A TUI (Terminal User Interface) code agent built with Java.

## Quick Start

### Using JBang (Recommended for testing)

```bash
# Run directly without building
jbang pi-java@butelo/pi-java

# Or from local source
jbang src/main/java/com/example/pijava/PiJavaApp.java --help
```

### Using Gradle

```bash
# Build the project
./gradlew build

# Run the application
./gradlew run

# Build native image (requires GraalVM)
./gradlew nativeCompile
```

## Features

- 🖥️ Modern TUI interface
- 🚀 Native image support via GraalVM
- 📦 Easy distribution with JReleaser
- 🔧 CLI argument parsing with Picocli

## Development

### Prerequisites

- Java 21+
- GraalVM (optional, for native images)

### Build

```bash
./gradlew clean build
```

### Run

```bash
./gradlew run --args="--help"
```

### Native Image

```bash
# Requires GraalVM 21+
./gradlew nativeCompile

# The binary will be at build/graal/pi-java
./build/graal/pi-java --help
```

## Distribution

### GitHub Release

```bash
# Tag a release
git tag v1.0.0
git push origin v1.0.0

# The GitHub Actions workflow will build and release
```

### Homebrew

After release, users can install via:

```bash
brew install butelo/pi-java/pi-java
```

### SDKMAN

```bash
sdk install pi-java 1.0.0
```

## Tech Stack

| Component | Technology |
|-----------|------------|
| CLI Parsing | [Picocli](https://picocli.info/) |
| TUI Library | [Lanterna](https://github.com/mabe02/lanterna) |
| Native Image | [GraalVM](https://www.graalvm.org/) |
| Build Tool | Gradle (Kotlin DSL) |
| Distribution | [JReleaser](https://jreleaser.org/) |
| Scripting | [JBang](https://www.jbang.dev/) |

## License

MIT License - See LICENSE file for details.

## Inspired By

- [2026: The Year of Java in the Terminal](https://xam.dk/blog/lets-make-2026-the-year-of-java-in-the-terminal/) by Max Rydahl Andersen
