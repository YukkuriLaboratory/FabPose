pluginManagement {
    repositories {
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        maven {
            name = "Stonecutter"
            url = uri("https://maven.kikugie.dev/releases")
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.3"
    // Allow Gradle to auto-provision JDKs the build requests (e.g. Java 25 for 26.1+).
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    kotlinController = true

    create(rootProject) {
        // See https://stonecutter.kikugie.dev/wiki/start/#choosing-minecraft-versions
        // 1.21.11 (and earlier) ships with intermediary mappings → uses fabric-loom remap pipeline.
        // 26.1+ is shipped un-obfuscated → uses the new fabric-loom (no remap).
        versions("1.21.11").buildscript("build.fabric.gradle.kts")
        version("26.1").buildscript("build.fabric.unobfuscated.gradle.kts")
        vcsVersion = "1.21.11"
    }
}

rootProject.name = "FabPose"
