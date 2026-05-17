# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview
FabPose is a Minecraft mod for Fabric servers that allows players to take various poses (/sit, /lay, /spin, /swim). It's a fork of FabSit aiming for feature parity with the Spigot GSit mod.

The `main` branch manages modern Minecraft versions (1.21.11+) via [Stonecutter](https://stonecutter.kikugie.dev/). Legacy versions (1.21.10 and earlier) remain on their dedicated branches and are not Stonecutter-managed.

Each Stonecutter-managed version uses one of two buildscripts, picked per version in `settings.gradle.kts`:

- `build.fabric.gradle.kts` — obfuscated Fabric Loom pipeline (mappings + remap). Used for 1.21.x.
- `build.fabric.unobfuscated.gradle.kts` — un-obfuscated Loom pipeline (no `mappings()`, plugin id `net.fabricmc.fabric-loom`). Used for 26.1+.

The two scripts also pin different JDKs (1.21.x = JDK 21, 26.1+ = JDK 25); they are provisioned automatically by Gradle's `foojay-resolver-convention` toolchain plugin. The Nix dev shell (`flake.nix`) provides `Xvfb` (via `xorg-server`) for headless `runClienttest`, plus `gcc` for the 26.1 PulseAudio stub used to keep flite from aborting in headless environments. Both buildscripts disable Loom's built-in `xvfb-run` wrapping and start `Xvfb` themselves only for `runClienttest`. Enter the shell with `nix develop` before invoking Gradle.

## Key Development Commands

### Build and Run
Stonecutter exposes a subproject per Minecraft version under `versions/<mc>`. Prefix Gradle tasks with `:<mc>:` to target a specific version, or use `chiseledBuild` to build every active version at once.

```bash
./gradlew chiseledBuild              # Build every active version
./gradlew :1.21.11:build             # Build a specific version
./gradlew :26.1:build                # Build the 26.1 (un-obfuscated) version
./gradlew :1.21.11:runServer         # Start development server
./gradlew :1.21.11:runClient         # Start development client
./gradlew :1.21.11:runServertest     # Run server-side tests
./gradlew :1.21.11:runClienttest     # Run client-side tests
```

Build outputs land in `versions/<mc>/build/libs/`.

### Adding a new Minecraft version
1. Add the version string to `versions(...)` in `settings.gradle.kts`. Pick the appropriate buildscript (`build.fabric.gradle.kts` for 1.21.x, `build.fabric.unobfuscated.gradle.kts` for 26.1+) via `versions("<mc>").buildscript("...")`.
2. Create `versions/<new-mc>/gradle.properties` mirroring an existing one with `minecraft_version` / `loader_version` / `fabric_version` / `flk_version` / `java_version` adjusted. `java_version` is expanded into `fabric.mod.json` and must match the buildscript's JDK (21 for 1.21.x, 25 for 26.1+).
3. Add the version to `matrix.include` in `.github/workflows/build.yml`, pinning the right JDK (21 for 1.21.x, 25 for 26.1+). Update the `case` in `publish.yml` (`Determine Java version for MC`) the same way.
4. (Optional) Update the active version pointer in `stonecutter.gradle.kts` (`stonecutter active "<mc>"`) if you want IDE imports and bare `./gradlew build` to target the new version by default.
5. Run `./gradlew :<new-mc>:build` to validate locally.

### Code Quality
```bash
./gradlew lintKotlin     # Run Kotlin linter (Kotlinter)
./gradlew formatKotlin   # Auto-format Kotlin code
```

### Release Tags
Releases are triggered by pushing tags matching `v<modver>+<mcver>` (e.g. `v1.4.2+1.21.11`).
The `publish.yml` workflow validates the tag with the regex
`^v[0-9A-Za-z._-]+\+[0-9]+\.[0-9]+(\.[0-9]+)?$` and uploads
`versions/<mcver>/build/libs/fabpose-<modver>+<mcver>.jar` (and `-sources.jar`).
The `<mcver>` segment must match an existing `versions/<mcver>/gradle.properties`.
The JDK is selected automatically from the MC version (`26.*` → JDK 25, otherwise → JDK 21).

## Architecture Overview

### Language Structure
- **Kotlin code** (`net.yukulab.fabpose`): New FabPose implementation
- **Java code** (`net.fill1890.fabsit`): Legacy FabSit code being migrated
- **Mixins**: Minecraft behavior modifications in `net.yukulab.fabpose.mixin`

### Core Systems
1. **Pose Management**: Uses invisible armor stand entities to handle player poses
2. **Command System**: Brigadier-based commands with permission checks
3. **Networking**: Custom packets for client-server synchronization
4. **Configuration**: JSON config at `config/fabsit.json` (Gson-based), reloadable via `/fabpose reload`

### Key Components
- `PoseManager`: Central pose state management
- `PoseManagerEntity`: Armor stand entity handling player poses
- `EntityPosing`: Extension methods for entity pose operations
- `FabPoseNetworking`: Packet handling for client-server communication

### Testing Approach
- Server tests use Fabric GameTest API in `src/servertest`
- Client tests for keybinds and UI in `src/clienttest`
- Tests are automatically run in CI via GitHub Actions

### Development Notes
- Uses Kotlin coroutines for async operations
- Access Widener for internal Minecraft class access
- Fabric Permissions API integration for permission management
- Vanilla client compatibility maintained through optional client-side features