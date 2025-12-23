# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview
FabPose is a Minecraft mod for Fabric servers that allows players to take various poses (/sit, /lay, /spin, /swim). It's a fork of FabSit aiming for feature parity with the Spigot GSit mod.

## Key Development Commands

### Build and Run
```bash
./gradlew build          # Build the project
./gradlew runServer      # Start development server
./gradlew runClient      # Start development client
./gradlew runServertest  # Run server-side tests
./gradlew runClienttest  # Run client-side tests
```

### Code Quality
```bash
./gradlew lintKotlin     # Run Kotlin linter (Kotlinter)
./gradlew formatKotlin   # Auto-format Kotlin code
```

## Architecture Overview

### Language Structure
- **Kotlin code** (`net.yukulab.fabpose`): New FabPose implementation
- **Java code** (`net.fill1890.fabsit`): Legacy FabSit code being migrated
- **Mixins**: Minecraft behavior modifications in `net.yukulab.fabpose.mixin`

### Core Systems
1. **Pose Management**: Uses invisible armor stand entities to handle player poses
2. **Command System**: Brigadier-based commands with permission checks
3. **Networking**: Custom packets for client-server synchronization
4. **Configuration**: JSON5 config at `config/fabsit.json`, reloadable via `/fabpose reload`

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