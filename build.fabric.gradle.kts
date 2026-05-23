import java.util.concurrent.TimeUnit
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Legacy buildscript for Minecraft 1.21.7 (and, after PR2-4: 1.21 / 1.21.1 /
// 1.21.4). All four versions in the legacy group share Yarn mappings, Java 21,
// and Loom 1.11-SNAPSHOT. Mod logic still uses the old PosingEntity system
// (modern moved to MannequinEntity from 1.21.10 onward; see legacy plan §1.4).

plugins {
    id("fabric-loom") version "1.11-SNAPSHOT"
    kotlin("jvm") version "2.1.0"
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
    maven("https://api.modrinth.com/maven")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

val loaderVersion = project.property("loader_version").toString()
val fabricVersion = project.property("fabric_version").toString()
val flkVersion = project.property("flk_version").toString()
val yarnMappings = project.property("yarn_mappings").toString()
val javaVersion = project.property("java_version").toString()
dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings("net.fabricmc:yarn:$yarnMappings:v2")
    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")

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
        modImplementation(fabricApi.module(it, fabricVersion))
    }
    modLocalRuntime("net.fabricmc.fabric-api:fabric-api:$fabricVersion")
    modImplementation("net.fabricmc:fabric-language-kotlin:$flkVersion")
    // PR1 (1.21.7): permissions-api 0.4.1 (Modrinth maven).
    // PR2/PR3 (1.21 / 1.21.1) may need a different coordinate; gate with Stonecutter `?if` if A-3 found a divergence.
    modImplementation(include("me.lucko:fabric-permissions-api:0.4.1")!!)

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
        "version" to project.version.toString(),
        "fabric_version" to fabricVersion,
        "minecraft_version" to minecraftVersion,
        "flk_version" to flkVersion,
        "java_version" to javaVersion,
    )

    filesMatching("fabric.mod.json") {
        expand(
            "loader_version" to loaderVersion,
            "version" to project.version.toString(),
            "fabric_version" to fabricVersion,
            "minecraft_version" to minecraftVersion,
            "flk_version" to flkVersion,
            "java_version" to javaVersion,
        )
    }
}

tasks.withType<JavaCompile>().configureEach {
    // All legacy MC versions (1.21 / 1.21.1 / 1.21.4 / 1.21.7) target Java 21.
    options.release = 21
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
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

publishMods {
    file.set(tasks.named("remapJar", org.gradle.api.tasks.bundling.AbstractArchiveTask::class).flatMap { it.archiveFile })
    additionalFiles.from(tasks.named("remapSourcesJar", org.gradle.api.tasks.bundling.AbstractArchiveTask::class).flatMap { it.archiveFile })
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

// --- Headless Xvfb auto-management (copied from modern build.fabric.gradle.kts) ---

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

tasks.withType<net.fabricmc.loom.task.AbstractRunTask>().configureEach {
    // Loom 1.14+ added AbstractRunTask#useXvfb; Loom 1.11 (used by the legacy
    // group) does not have it, so silently skip when the getter is missing.
    @Suppress("UNCHECKED_CAST")
    val getter = javaClass.methods.firstOrNull { it.name == "getUseXvfb" } ?: return@configureEach
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
        val shutdownHook = Thread { if (process.isAlive) process.destroyForcibly() }
        Runtime.getRuntime().addShutdownHook(shutdownHook)

        logger.lifecycle("Started Xvfb on display $display (pid: ${process.pid()})")
        environment("DISPLAY", display)
        environment("ALSOFT_DRIVERS", "null")
        environment("SDL_AUDIODRIVER", "dummy")
    }
}
