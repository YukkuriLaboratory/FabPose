# AGENTS.md

This file provides guidance to AI coding agents working with code in the legacy
branch group of FabPose.

## Project Overview

This is the **legacy branch group** of FabPose, managing Minecraft versions
1.21 / 1.21.1 / 1.21.4 / 1.21.7 in a single Stonecutter setup.

The mod logic still uses the old **PosingEntity** system (armor stand based).
The modern PosingMannequin / MannequinEntity architecture starting from
Minecraft 1.21.10 lives on `main`, not here.

- `main` branch: 1.21.11+ (and onwards). Modern PosingMannequin.
- `legacy/1.21` branch (this branch): 1.21 - 1.21.7. Legacy PosingEntity.

## Legacy Stonecutter layout

Stonecutter manages each MC version as a subproject under `versions/<mc>/`.
All four versions share a single buildscript: `build.fabric.gradle.kts`.

- Java 21 (uniform across the legacy group)
- Yarn mappings
- Loom 1.11-SNAPSHOT (lowest common multiple)
- Gradle 9.4.0

## Build and Run

```bash
./gradlew chiseledBuild              # Build every legacy MC version
./gradlew :1.21.7:build              # Build a specific version
./gradlew :1.21.7:runServer          # Start a dev server
./gradlew :1.21.7:runClient          # Start a dev client
./gradlew :1.21.7:runServertest      # Run server-side tests
./gradlew :1.21.7:runClienttest      # Run client-side tests
./gradlew lintKotlin                 # Lint Kotlin code (Kotlinter)
./gradlew formatKotlin               # Auto-format Kotlin code
```

Build outputs land in `versions/<mc>/build/libs/`.

## Adding a new legacy MC version

1. Add the version to `versions(...)` in `settings.gradle.kts`.
2. Create `versions/<new-mc>/gradle.properties` mirroring `versions/1.21.7/gradle.properties`,
   adjusting `minecraft_version` / `loader_version` / `fabric_version` /
   `flk_version` / `yarn_mappings`.
3. Add the version to `matrix.include` in `.github/workflows/build.yml`.
4. If the new MC needs a different `fabric-permissions-api` coordinate (e.g.
   the `0.1-SNAPSHOT` published only on the Sonatype snapshots repo for 1.21
   and 1.21.1), gate the dependency declaration in `build.fabric.gradle.kts`
   with Stonecutter `?if` comments.
5. Run `./gradlew :<new-mc>:build` and `./gradlew :<new-mc>:runServertest`
   to validate locally.

## Release Tags

Legacy releases are triggered by pushing an **annotated** tag matching
`legacy-v<modver>` (e.g. `git tag -a legacy-v1.0.0 -m "Release notes..."`).
The `publish.yml` workflow on this branch requires the tag to be annotated and
uses its annotation message as the changelog for Modrinth, CurseForge, and the
GitHub Release.

The modern `main` branch publishes via `v<modver>` tags. The two namespaces
are independent.

A single legacy tag publishes every MC version registered in
`settings.gradle.kts`:

- `./gradlew chiseledBuild` builds all versions into
  `versions/<mcver>/build/libs/fabpose-<modver>+<mcver>.jar`.
- `./gradlew chiseledPublishMods` uploads each artifact to Modrinth and
  CurseForge. Each MC version becomes a separate platform release.
- The GitHub Release attaches all built jars (sources excluded) and is titled
  `Legacy v<modver>`.

Required repository secrets (shared with modern `main`):
`MODRINTH_ID`, `MODRINTH_TOKEN`, `CURSEFORGE_ID`, `CURSEFORGE_TOKEN`.

## See also

- `.sisyphus/plans/stonecutter-migration-legacy.md` (local-only plan
  document; not tracked in git via global ignore)
