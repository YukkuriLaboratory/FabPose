import java.util.concurrent.TimeUnit
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("fabric-loom") version "1.14-SNAPSHOT"
    id("maven-publish")
    kotlin("jvm") version "2.3.0"
    id("org.jmailen.kotlinter") version "5.2.0"
}

val minecraftVersion = project.property("minecraft_version").toString()
val modVersion = System.getenv("MOD_VERSION") ?: "0.0.0"
base {
    archivesName.set(project.property("archives_base_name") as? String)
    version = "$modVersion+$minecraftVersion"
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
    maven("https://api.modrinth.com/maven")
}

val loaderVersion = project.property("loader_version").toString()
val fabricVersion = project.property("fabric_version").toString()
val flkVersion = project.property("flk_version").toString()
val javaVersion = project.property("java_version").toString()
dependencies {
    // To change the versions see versions/<mc>/gradle.properties
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")
    compileOnly("com.mojang:authlib:3.13.56")

    setOf(
        "fabric-api-base",
        "fabric-command-api-v2",
        "fabric-events-interaction-v0",
        "fabric-key-binding-api-v1",
        "fabric-lifecycle-events-v1",
        "fabric-networking-api-v1",
        "fabric-registry-sync-v0",
        "fabric-rendering-v1",
        "fabric-object-builder-api-v1",
        "fabric-gametest-api-v1",
    ).forEach {
        // Fabric API. This is technically optional, but you probably want it anyway.
        modImplementation(fabricApi.module(it, fabricVersion))
    }
    // For Gametests
    modLocalRuntime("net.fabricmc.fabric-api:fabric-api:$fabricVersion")
    // Kotlin
    modImplementation("net.fabricmc:fabric-language-kotlin:$flkVersion")
    // Permissions API
    modImplementation(include("me.lucko:fabric-permissions-api:0.6.1")!!)

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
    accessWidenerPath.set(rootProject.file("src/main/resources/fabpose.accesswidener"))
    runtimeOnlyLog4j.set(true)

    runs {
        create(serverTest) {
            server()
            configName = serverTest
            vmArgs(
                "-Dfabric-api.gametest",
                "-Dfabric.api.gametest.report-file=${project.layout.buildDirectory.get()}/$name/junit.xml",
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
                "-Dfabric.api.gametest.report-file=${project.layout.buildDirectory.get()}/$name/junit.xml",
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

tasks.withType<AbstractCopyTask>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.processResources {
    inputs.properties(
        "loader_version" to loaderVersion,
        "version" to project.version,
        "fabric_version" to fabricVersion,
        "minecraft_version" to minecraftVersion,
        "flk_version" to flkVersion,
        "java_version" to javaVersion,
    )

    filesMatching("fabric.mod.json") {
        expand(
            "loader_version" to loaderVersion,
            "version" to project.version,
            "fabric_version" to fabricVersion,
            "minecraft_version" to minecraftVersion,
            "flk_version" to flkVersion,
            "java_version" to javaVersion,
        )
    }

    // 26.1+ ships an additional AccessWidener with namespace `official`. It is unused
    // (and rejected) by Loom on intermediary-mapped versions, so drop it from the jar.
    exclude("fabpose.official.accesswidener")
}

tasks.withType<JavaCompile>().configureEach {
    // Minecraft 1.20.6 upwards uses Java 21
    options.release = 21
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
    // if it is present.
    // If you remove this line, sources will not be generated.
    withSourcesJar()
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

tasks.jar {
    from(rootProject.file("LICENSE")) {
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

// Auto-start Xvfb for headless client test execution (Wayland / headless environments)
// Shared state for Xvfb process between run task and cleanup task
val xvfbState = objects.property<Process>()

fun needsXvfb(): Boolean {
    val display = System.getenv("DISPLAY")
    if (display.isNullOrBlank()) return true
    // For remote displays (e.g., SSH X11 forwarding like localhost:10.0), trust the env
    if (display.contains(":") && !display.startsWith(":")) return false
    // For local displays, verify the X11 socket is alive (not stale)
    val displayNum = display.removePrefix(":").takeWhile { it.isDigit() }
    val socket = File("/tmp/.X11-unix/X$displayNum")
    return !socket.exists()
}

fun findXvfb(): String? {
    val candidates = listOf("Xvfb", "/usr/bin/Xvfb")
    return candidates.firstOrNull { name ->
        runCatching {
            ProcessBuilder("which", name)
                .redirectErrorStream(true)
                .start()
                .waitFor() == 0
        }.getOrDefault(false)
    }
}

fun startXvfb(xvfb: String): Pair<Process, String> {
    // Try multiple display numbers to handle concurrent usage
    for (displayNum in 99..199) {
        val display = ":$displayNum"
        if (File("/tmp/.X11-unix/X$displayNum").exists()) continue

        val process = ProcessBuilder(xvfb, display, "-screen", "0", "1280x1024x24", "-nolisten", "tcp")
            .redirectErrorStream(true)
            .start()

        // Poll for X11 socket to appear (readiness check)
        val socketFile = File("/tmp/.X11-unix/X$displayNum")
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            if (!process.isAlive) break
            if (socketFile.exists()) return process to display
            Thread.sleep(100)
        }

        // This display didn't work, clean up and try next
        if (process.isAlive) process.destroyForcibly()
    }
    error("Failed to start Xvfb: no available display number in :99..:199")
}

val cleanupXvfbTask = tasks.register("cleanupXvfb") {
    group = "verification"
    description = "Stops the Xvfb process started for runClienttest, if any."
    doLast {
        xvfbState.orNull?.let { process ->
            if (process.isAlive) {
                logger.lifecycle("Stopping Xvfb (pid: ${process.pid()})")
                process.destroy()
                process.waitFor(5, TimeUnit.SECONDS)
                if (process.isAlive) process.destroyForcibly()
            }
        }
    }
}

// Disable Loom's built-in `xvfb-run` wrapping for every run task. We manage Xvfb
// ourselves (only for runClienttest, see below). Without this, Loom auto-wraps
// even runServertest with `xvfb-run` on Linux + CI environments, which crashes
// when xvfb-run is not on PATH. `useXvfb` is protected in Loom 1.14, so go
// through reflection (it became `public` in Loom 1.16+ via PR #1508 but we
// keep one form that works on both).
tasks.withType<net.fabricmc.loom.task.AbstractRunTask>().configureEach {
    @Suppress("UNCHECKED_CAST")
    val getter = javaClass.methods.firstOrNull { it.name == "getUseXvfb" }
        ?: error("AbstractRunTask#getUseXvfb not found on ${javaClass.name}")
    (getter.invoke(this) as org.gradle.api.provider.Property<Boolean>).set(false)
}

val clientTestTaskName = "run${clientTest.replaceFirstChar(Char::uppercaseChar)}"
tasks.named<JavaExec>(clientTestTaskName) {
    finalizedBy(cleanupXvfbTask)

    doFirst {
        if (!needsXvfb()) return@doFirst

        val xvfb = findXvfb() ?: error(
            "No usable DISPLAY found and Xvfb is not installed. " +
                "Install Xvfb or run with a display server (e.g., xvfb-run ./gradlew $clientTestTaskName)",
        )

        val (process, display) = startXvfb(xvfb)
        xvfbState.set(process)
        // Last-resort cleanup for JVM crash (daemon shutdown)
        val shutdownHook = Thread { if (process.isAlive) process.destroyForcibly() }
        Runtime.getRuntime().addShutdownHook(shutdownHook)

        logger.lifecycle("Started Xvfb on display $display (pid: ${process.pid()})")
        environment("DISPLAY", display)
    }
}
