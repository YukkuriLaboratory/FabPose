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
        maven {
            name = "Modrinth"
            url = uri("https://api.modrinth.com/maven")
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.3"
    // Allow Gradle to auto-provision JDK 21 if the host environment lacks it.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    kotlinController = true
    centralScript = "build.fabric.gradle.kts"

    create(rootProject) {
        // PR1 scope: 1.21.7 only.
        // PR2 will add "1.21", PR3 will add "1.21.1", PR4 will add "1.21.4".
        versions("1.21.7")
        vcsVersion = "1.21.7"
    }
}

rootProject.name = "FabPose"
