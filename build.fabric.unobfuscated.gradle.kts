import java.util.concurrent.TimeUnit
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// NOTE: This buildscript and build.fabric.gradle.kts share large structural
// overlap. See the header of build.fabric.gradle.kts for the list of
// intentional differences (Loom plugin id/version, mappings, configuration
// names, Java/Kotlin target, AccessWidener variant, permissions-api version,
// and the PulseAudio stub specific to this 26.1+ buildscript). Keep
// behavioural fixes (e.g., headless hardening) in sync across both files.

plugins {
    // 26.1+ ships un-obfuscated → use the new fabric-loom plugin id (no remap pipeline).
    id("net.fabricmc.fabric-loom") version "1.16-SNAPSHOT"
    kotlin("jvm") version "2.3.0"
    id("org.jmailen.kotlinter") version "5.2.0"
    id("me.modmuss50.mod-publish-plugin") version "0.8.4"
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
    // 26.1+ is shipped un-obfuscated; no `mappings(...)` declaration needed.
    implementation("net.fabricmc:fabric-loader:$loaderVersion")
    compileOnly("com.mojang:authlib:3.13.56")

    setOf(
        "fabric-api-base",
        "fabric-command-api-v2",
        "fabric-events-interaction-v0",
        // 1.21.11 still uses fabric-key-binding-api-v1; 26.1 renamed it to fabric-key-mapping-api-v1.
        "fabric-key-mapping-api-v1",
        "fabric-lifecycle-events-v1",
        "fabric-networking-api-v1",
        "fabric-registry-sync-v0",
        "fabric-rendering-v1",
        "fabric-object-builder-api-v1",
        "fabric-gametest-api-v1",
    ).forEach {
        implementation(fabricApi.module(it, fabricVersion))
    }
    // For Gametests
    runtimeOnly("net.fabricmc.fabric-api:fabric-api:$fabricVersion")
    // Kotlin
    implementation("net.fabricmc:fabric-language-kotlin:$flkVersion")
    // Permissions API (un-obfuscated build that targets 26.1+)
    implementation(include("me.lucko:fabric-permissions-api:0.7.0")!!)

    testImplementation("io.kotest:kotest-runner-junit5:5.6.2")?.version?.also { kotestVersion ->
        testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
        testImplementation("io.kotest:kotest-property:$kotestVersion")
        testImplementation("io.kotest:kotest-framework-datatest:$kotestVersion")
    }
}

loom {
    accessWidenerPath.set(rootProject.file("src/main/resources/fabpose.official.accesswidener"))
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

    // Use the un-obfuscated AccessWidener (namespace `official`) for 26.1+, exposing it
    // under the canonical `fabpose.accesswidener` name referenced by fabric.mod.json.
    exclude("fabpose.accesswidener")
    rename("fabpose\\.official\\.accesswidener", "fabpose.accesswidener")
}

tasks.withType<JavaCompile>().configureEach {
    // Minecraft 26.1+ requires Java 25
    options.release = 25
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
    withSourcesJar()
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
    }
}

tasks.jar {
    from(rootProject.file("LICENSE")) {
        rename { "${it}_${project.base.archivesName}" }
    }
}

publishMods {
    file.set(tasks.jar.flatMap { it.archiveFile })
    additionalFiles.from(tasks.named("sourcesJar").map { (it as Jar).archiveFile })
    changelog.set(providers.environmentVariable("CHANGELOG").orElse(""))
    type.set(me.modmuss50.mpp.ReleaseType.STABLE)
    modLoaders.add("fabric")

    modrinth {
        projectId.set(providers.environmentVariable("MODRINTH_ID"))
        accessToken.set(providers.environmentVariable("MODRINTH_TOKEN"))
        minecraftVersions.add(minecraftVersion)
        requires("fabric-api")
        requires("fabric-language-kotlin")
    }
    curseforge {
        projectId.set(providers.environmentVariable("CURSEFORGE_ID"))
        accessToken.set(providers.environmentVariable("CURSEFORGE_TOKEN"))
        minecraftVersions.add(minecraftVersion)
        requires("fabric-api")
        requires("fabric-language-kotlin")
    }
}

// Auto-start Xvfb for headless client test execution (Wayland / headless environments)
val xvfbState = objects.property<Process>()

fun needsXvfb(): Boolean {
    val display = System.getenv("DISPLAY")
    if (display.isNullOrBlank()) return true
    if (display.contains(":") && !display.startsWith(":")) return false
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
    for (displayNum in 99..199) {
        val display = ":$displayNum"
        if (File("/tmp/.X11-unix/X$displayNum").exists()) continue

        val process = ProcessBuilder(xvfb, display, "-screen", "0", "1280x1024x24", "-nolisten", "tcp")
            .redirectErrorStream(true)
            .start()

        val socketFile = File("/tmp/.X11-unix/X$displayNum")
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            if (!process.isAlive) break
            if (socketFile.exists()) return process to display
            Thread.sleep(100)
        }

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
// when xvfb-run is not on PATH. Use reflection to keep one form that works
// across Loom versions (`useXvfb` is `public` in 1.16+, `protected` in 1.14).
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
        if (needsXvfb()) {
            val xvfb = findXvfb() ?: error(
                "No usable DISPLAY found and Xvfb is not installed. " +
                    "Install Xvfb or run with a display server (e.g., xvfb-run ./gradlew $clientTestTaskName)",
            )

            val (process, display) = startXvfb(xvfb)
            xvfbState.set(process)
            val shutdownHook = Thread { if (process.isAlive) process.destroyForcibly() }
            Runtime.getRuntime().addShutdownHook(shutdownHook)

            logger.lifecycle("Started Xvfb on display $display (pid: ${process.pid()})")
            environment("DISPLAY", display)
        }

        environment("ALSOFT_DRIVERS", "null")
        environment("SDL_AUDIODRIVER", "dummy")
        environment("PULSE_SERVER", "/dev/null")
        environment("LIBGL_ALWAYS_SOFTWARE", "1")
        ensurePulseStub()?.let { stub ->
            val existing = System.getenv("LD_PRELOAD")
            val ldPreload = if (existing.isNullOrBlank()) stub.absolutePath else "${stub.absolutePath}:$existing"
            environment("LD_PRELOAD", ldPreload)
            logger.lifecycle("Using PulseAudio stub: ${stub.absolutePath}")
        }
    }
}

/**
 * Compile a stub libpulse-simple.so that returns dummy handles to prevent
 * flite (TTS) from crashing with SIGABRT when PulseAudio daemon is unavailable.
 * Returns null if gcc is not available (e.g., on minimal CI images), in which
 * case the run task proceeds without LD_PRELOAD and may hit the assertion.
 */
fun ensurePulseStub(): File? {
    val stubDir = layout.buildDirectory.dir("pulse-stub").get().asFile
    val stubLib = File(stubDir, "libpulse-simple-stub.so")
    if (stubLib.exists()) return stubLib

    val hasGcc = runCatching {
        ProcessBuilder("which", "gcc").redirectErrorStream(true).start().waitFor() == 0
    }.getOrDefault(false)
    if (!hasGcc) {
        logger.warn(
            "gcc not found on PATH; cannot build PulseAudio stub. " +
                "runClienttest may abort with SIGABRT if PulseAudio daemon is reachable but unwritable.",
        )
        return null
    }

    stubDir.mkdirs()
    val stubSrc = File(stubDir, "pulse_stub.c")
    stubSrc.writeText(
        """
        #include <stddef.h>
        void *pa_simple_new(const void *s, const char *n, int d,
                            const char *dev, const char *sn,
                            const void *ss, const void *map, int *e) {
            static char dummy; return &dummy;
        }
        int pa_simple_write(void *p, const void *data, size_t bytes, int *e) { return 0; }
        int pa_simple_drain(void *p, int *e) { return 0; }
        void pa_simple_free(void *p) {}
        int pa_simple_read(void *p, void *data, size_t bytes, int *e) { return 0; }
        size_t pa_simple_get_latency(void *p, int *e) { return 0; }
        int pa_simple_flush(void *p, int *e) { return 0; }
        """.trimIndent(),
    )
    val tmpLib = File(stubDir, "libpulse-simple-stub.so.tmp")
    if (tmpLib.exists()) tmpLib.delete()
    val result = ProcessBuilder("gcc", "-shared", "-fPIC", "-o", tmpLib.absolutePath, stubSrc.absolutePath)
        .redirectErrorStream(true)
        .start()
    val output = result.inputStream.bufferedReader().readText()
    val exit = result.waitFor()
    if (exit != 0 || !tmpLib.exists()) {
        logger.warn("Failed to compile PulseAudio stub (exit $exit): $output")
        tmpLib.delete()
        return null
    }
    if (!tmpLib.renameTo(stubLib)) {
        logger.warn("Failed to move compiled PulseAudio stub into place")
        tmpLib.delete()
        return null
    }
    return stubLib
}
