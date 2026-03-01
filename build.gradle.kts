plugins {
    java
    application
    id("com.gradleup.shadow") version "8.3.5"
    id("org.graalvm.buildtools.native") version "0.10.4"
    id("com.github.spotbugs") version "6.1.4"
    id("checkstyle")
    id("pmd")
}

group = "com.example"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    // CLI argument parsing
    implementation("info.picocli:picocli:4.7.6")
    
    // TUI Library - JLine 3 (modern, feature-rich)
    implementation("org.jline:jline:3.26.2")
    
    // HTTP Client (Java 21 has built-in, but this is useful)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // JSON processing
    implementation("com.google.code.gson:gson:2.11.0")
    
    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.12")
    
    // SpotBugs annotations for suppressing false positives
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.8.6")
    
    // Annotation processor for picocli
    annotationProcessor("info.picocli:picocli-codegen:4.7.6")
}

application {
    mainClass.set("com.example.pijava.App")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.example.pijava.App"
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf(
        "-Aproject=${project.group}/${project.name}",
        "-Xlint:all",
        "-Xlint:-processing",
        "-Werror"
    ))
    options.encoding = "UTF-8"
}

// SpotBugs configuration
spotbugs {
    effort.set(com.github.spotbugs.snom.Effort.MAX)
    reportLevel.set(com.github.spotbugs.snom.Confidence.MEDIUM)
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask> {
    reports {
        create("html") { required.set(true) }
        create("xml") { required.set(false) }
    }
}

// Checkstyle configuration
checkstyle {
    toolVersion = "10.21.0"
    configFile = file("config/checkstyle/checkstyle.xml")
    isShowViolations = true
}

tasks.withType<Checkstyle> {
    reports {
        xml.required.set(false)
        html.required.set(true)
    }
}

// PMD configuration
pmd {
    toolVersion = "7.7.0"
    isConsoleOutput = true
    rulesMinimumPriority.set(3)
}

tasks.withType<Pmd> {
    reports {
        xml.required.set(false)
        html.required.set(true)
    }
}

graalvmNative {
    binaries {
        named("main") {
            javaLauncher.set(javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(21))
                vendor.set(JvmVendorSpec.matching("GraalVM Community"))
            })
            imageName.set("pi-java")
        }
    }
}
