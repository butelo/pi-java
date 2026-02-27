plugins {
    java
    application
    id("com.gradleup.shadow") version "8.3.5"
    id("org.graalvm.buildtools.native") version "0.10.4"
}

group = "com.example"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    // CLI argument parsing
    implementation("info.picocli:picocli:4.7.6")
    
    // TUI Library - Lanterna (stable, mature)
    implementation("com.googlecode.lanterna:lanterna:3.1.1")
    
    // HTTP Client (Java 21 has built-in, but this is useful)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // JSON processing
    implementation("com.google.code.gson:gson:2.11.0")
    
    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.12")
    
    // Annotation processor for picocli
    annotationProcessor("info.picocli:picocli-codegen:4.7.6")
}

application {
    mainClass.set("com.example.pijava.PiJavaApp")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.example.pijava.PiJavaApp"
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Aproject=${project.group}/${project.name}")
}

graalvmNative {
    binaries {
        named("main") {
            javaLauncher.set(javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(17))
                vendor.set(JvmVendorSpec.matching("GraalVM Community"))
            })
            imageName.set("pi-java")
        }
    }
}
