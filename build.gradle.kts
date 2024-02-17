import org.jetbrains.kotlin.gradle.dsl.JvmTarget

buildscript {
    configurations.classpath {
        resolutionStrategy {
            force(
                "com.pinterest.ktlint:ktlint-rule-engine:1.0.0",
                "com.pinterest.ktlint:ktlint-rule-engine-core:1.0.0",
                "com.pinterest.ktlint:ktlint-cli-reporter-core:1.0.0",
                "com.pinterest.ktlint:ktlint-cli-reporter-checkstyle:1.0.0",
                "com.pinterest.ktlint:ktlint-cli-reporter-json:1.0.0",
                "com.pinterest.ktlint:ktlint-cli-reporter-html:1.0.0",
                "com.pinterest.ktlint:ktlint-cli-reporter-plain:1.0.0",
                "com.pinterest.ktlint:ktlint-cli-reporter-sarif:1.0.0",
                "com.pinterest.ktlint:ktlint-ruleset-standard:1.0.0",
            )
        }
    }
}

plugins {
    id("fabric-loom") version "1.5-SNAPSHOT"
    id("maven-publish")
    kotlin("jvm") version "1.9.21"
    id("org.jmailen.kotlinter") version "4.2.0"
}

base {
    archivesName.set(project.property("archives_base_name") as? String)
    version = project.property("mod_version")!!
    group = project.property("maven_group")!!
}

val serverTest = "servertest"
val clientTest = "clienttest"
sourceSets {
    val main by main
    val classPathConfig =
        closureOf<SourceSet> {
            compileClasspath += main.compileClasspath
            compileClasspath += main.output
            runtimeClasspath += main.runtimeClasspath
            runtimeClasspath += main.output
        }
    create(serverTest, classPathConfig)
    create(clientTest, classPathConfig)
}
val serverTestSourceSet = sourceSets.getByName(serverTest)
val clientTestSourceSet = sourceSets.getByName(clientTest)

configurations {
    val implementation = "Implementation"
    val testImplementation = testImplementation.get().exclude("org.slf4j", "slf4j-simple")
    getByName("$serverTest$implementation").extendsFrom(testImplementation)
    getByName("$clientTest$implementation").extendsFrom(testImplementation)
}

repositories {
    // Add repositories to retrieve artifacts from in here.
    // You should only use this when depending on other mods because
    // Loom adds the essential maven repositories to download Minecraft and libraries from automatically.
    // See https://docs.gradle.org/current/userguide/declaring_repositories.html
    // for more information about repositories.

    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

val minecraftVersion = project.property("minecraft_version")
val loaderVersion = project.property("loader_version")
val fabricVersion = project.property("fabric_version")
val flkVersion = project.property("flk_version")
dependencies {
    // To change the versions see the gradle.properties file
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings("net.fabricmc:yarn:${project.property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")

    // Fabric API. This is technically optional, but you probably want it anyway.
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricVersion")
    // Kotlin
    modImplementation("net.fabricmc:fabric-language-kotlin:$flkVersion")
    // Permissions API
    modImplementation(include("me.lucko:fabric-permissions-api:0.1-SNAPSHOT")!!)

    // Uncomment the following line to enable the deprecated Fabric API modules.
    // These are included in the Fabric API production distribution and allow you to update your mod to the latest modules at a later more convenient time.

    // modImplementation("net.fabricmc.fabric-api:fabric-api-deprecated:${project.property("fabric_version}")")

    testImplementation("io.kotest:kotest-runner-junit5:5.6.2")?.version?.also { kotestVersion ->
        testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
        testImplementation("io.kotest:kotest-property:$kotestVersion")
        testImplementation("io.kotest:kotest-framework-datatest:$kotestVersion")
    }
}

loom {
    accessWidenerPath.set(file("src/main/resources/fabpose.accesswidener"))
    runtimeOnlyLog4j.set(true)

    runs {
        create(serverTest) {
            server()
            configName = serverTest
            vmArgs(
                "-Dfabric-api.gametest",
                "-Dfabric.api.gametest.report-file=${project.layout.buildDirectory}/$name/junit.xml",
            )
            runDir = "build/$serverTest"
            setSource(serverTestSourceSet)
            isIdeConfigGenerated = true
        }
        create(clientTest) {
            client()
            configName = clientTest
            vmArgs(
                "-Dfabric-api.gametest",
                "-Dfabric.api.gametest.report-file=${project.layout.buildDirectory}/$name/junit.xml",
            )
            runDir = "build/$clientTest"
            setSource(clientTestSourceSet)
            isIdeConfigGenerated = true
        }
        create("manual$serverTest") {
            server()
            configName = "Manual $serverTest"
            runDir = "build/$serverTest"
            vmArgs("-Dfabric-api.gametest.command=true")
            setSource(serverTestSourceSet)
            isIdeConfigGenerated = true
        }
    }
}

tasks.processResources {
    inputs.properties(
        "loader_version" to loaderVersion,
        "version" to project.version,
        "fabric_version" to fabricVersion,
        "minecraft_version" to minecraftVersion,
        "flk_version" to flkVersion,
    )

    filesMatching("fabric.mod.json") {
        expand(
            "loader_version" to loaderVersion,
            "version" to project.version,
            "fabric_version" to fabricVersion,
            "minecraft_version" to minecraftVersion,
            "flk_version" to flkVersion,
        )
    }
}

tasks.withType<JavaCompile>().configureEach {
    // Minecraft 1.18 (1.18-pre2) upwards uses Java 17.
    options.release = 17
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
    // if it is present.
    // If you remove this line, sources will not be generated.
    withSourcesJar()
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${project.base.archivesName}" }
    }
}

// configure the maven publication
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components.getByName("java"))
        }
    }

    // See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
    repositories {
        // Add repositories to publish to here.
        // Notice: This block does NOT have the same function as the block in the top level.
        // The repositories here will be used for publishing your artifact, not for
        // retrieving dependencies.
    }
}
