# Migration Plan: PosingEntity → MannequinEntity

## Overview

Migrate the current `PosingEntity` (extends `ServerPlayerEntity`) to use Minecraft 1.21.x's new `MannequinEntity` (extends `PlayerLikeEntity`) to fix the player invisibility issue with `/lay` command and simplify the codebase.

## Problem Statement

The `/lay` command causes the player to become invisible because:
1. `PosingEntity` extends `ServerPlayerEntity`, requiring complex tab list management
2. Client-side `setPositionInBed()` teleports the entity to world bottom when `SLEEPING_POSITION` is set
3. Manual skin fetching (`SkinUtil`) is unreliable and adds complexity

## Solution

Use `MannequinEntity` (added in Minecraft 1.21.x) which:
- Uses `EntityType.MANNEQUIN` (standard entity, no tab list required)
- Supports `ProfileComponent` for automatic skin loading
- Supports `SLEEPING`, `SWIMMING`, `GLIDING`, `CROUCHING` poses natively
- Renders using `PlayerEntityRenderer` like players

## Current Architecture

```
Player
    ↓ rides (startRiding)
PoseManagerEntity (extends ArmorStandEntity, registered)
    ↓ creates & manages
PosingEntity (extends ServerPlayerEntity, NOT registered as EntityType)
    ├── LayingEntity (sleeping pose)
    └── SpinningEntity (spin attack pose)
```

## Target Architecture

```
Player
    ↓ rides (startRiding)
PoseManagerEntity (existing, minimal changes)
    ↓ creates & manages
MannequinEntity (Minecraft's EntityType.MANNEQUIN)
    - ProfileComponent for skin
    - setPose() for pose
    - No tab list packets needed
```

---

## File Impact Analysis

### Files to DELETE
| File | Reason |
|------|--------|
| `src/main/java/net/fill1890/fabsit/util/SkinUtil.java` | Replaced by ProfileComponent |
| `src/main/java/net/fill1890/fabsit/error/LoadSkinException.java` | No longer needed |
| `src/main/java/net/fill1890/fabsit/mixin/accessor/PlayerListS2CPacketAccessor.java` | Tab list no longer used |

### Files to REWRITE
| File | Changes |
|------|---------|
| `src/main/java/net/fill1890/fabsit/entity/PosingEntity.java` | Complete rewrite to use MannequinEntity |
| `src/main/java/net/fill1890/fabsit/entity/LayingEntity.java` | Simplify, keep bed placement logic |
| `src/main/java/net/fill1890/fabsit/entity/SpinningEntity.java` | Simplify |

### Files to MODIFY
| File | Changes |
|------|---------|
| `src/main/kotlin/net/yukulab/fabpose/entity/define/PoseManagerEntity.kt` | Change poser creation to MannequinEntity |

### Files to VERIFY (may need changes)
| File | Check |
|------|-------|
| `src/main/java/net/fill1890/fabsit/mixin/injector/ServerPlayerEntityMixin.java` | PosingEntity references |
| `src/main/java/net/fill1890/fabsit/mixin/injector/ServerPlayNetworkHandlerMixin.java` | PosingEntity references |
| `src/main/java/net/fill1890/fabsit/mixin/injector/OtherClientPlayerEntityMixin.java` | May not be needed |
| Rendering Mixins | MannequinEntity compatibility |

### Files UNCHANGED
| File | Reason |
|------|--------|
| `src/main/java/net/fill1890/fabsit/mixin/accessor/EntityAccessor.java` | Still needed for POSE access |
| `src/main/java/net/fill1890/fabsit/mixin/accessor/LivingEntityAccessor.java` | Still needed for SLEEPING_POSITION |
| `src/main/java/net/fill1890/fabsit/mixin/accessor/PlayerLikeEntityAccessor.java` | Works with MannequinEntity too |

---

## Implementation Tasks

### Phase 1: Prototype & Validation ✅ COMPLETE
- [x] **Task 1.1**: Create minimal MannequinEntity spawn test ✅
  - Created `TestMannequinEntityPrototype.kt` with 12 tests
  - All poses (STANDING, SLEEPING, SWIMMING, CROUCHING, GLIDING) work
- [x] **Task 1.2**: Create MannequinEntityAccessor for ProfileComponent ✅
  - Created `MannequinEntityAccessor.java` to access protected PROFILE field
  - ProfileComponent setting verified via tests
- [x] **Task 1.3**: Verify SLEEPING pose renders correctly on MannequinEntity ✅
  - Verified via `/fabpose debug mannequin SLEEPING` command
  - Screenshot confirmation: renders correctly
- [x] **Task 1.4**: Verify equipment sync works with MannequinEntity ✅
  - Equipment (armor, held items) syncs correctly to MannequinEntity
  - Screenshot confirmation: all equipment visible

**Additional Phase 1 work:**
- [x] Fixed `MixinPlayerEntityRenderer.java` - added instanceof check for MannequinEntity compatibility
- [x] Added debug command `/fabpose debug mannequin <pose>` (dev environment only)
- [x] Fixed `LayingEntity.java` bed position (use `getBlockPos().down()` instead of `minY`)

### Phase 2: Core Implementation
- [ ] **Task 2.1**: Create new `PosingMannequin` helper class for MannequinEntity management
- [ ] **Task 2.2**: Implement laying pose using MannequinEntity
- [ ] **Task 2.3**: Implement spinning pose using MannequinEntity
- [ ] **Task 2.4**: Update `PoseManagerEntity.kt` to use new implementation

### Phase 3: Cleanup
- [ ] **Task 3.1**: Remove old PosingEntity, LayingEntity, SpinningEntity
- [ ] **Task 3.2**: Remove SkinUtil and LoadSkinException
- [ ] **Task 3.3**: Remove PlayerListS2CPacketAccessor
- [ ] **Task 3.4**: Clean up unused Mixins

### Phase 4: Testing & Verification
- [ ] **Task 4.1**: Test /sit command
- [ ] **Task 4.2**: Test /lay command (the main issue)
- [ ] **Task 4.3**: Test /spin command
- [ ] **Task 4.4**: Test /swim command
- [ ] **Task 4.5**: Multiplayer visibility test
- [ ] **Task 4.6**: Vanilla client compatibility test

---

## Technical Notes

### MannequinEntity Key APIs

```java
// Create MannequinEntity
MannequinEntity mannequin = EntityType.MANNEQUIN.create(world, SpawnReason.COMMAND);

// Set skin via ProfileComponent
ProfileComponent profile = ProfileComponent.of(player.getGameProfile());
mannequin.getDataTracker().set(MannequinEntity.PROFILE, profile);

// Set pose
mannequin.setPose(EntityPose.SLEEPING);

// Spawn in world
world.spawnEntity(mannequin);
```

### Bed Placement for SLEEPING Pose

The SLEEPING pose requires a bed block for `getSleepingDirection()` to return non-null. Options:
1. Place invisible bed client-side (current approach, may still be needed)
2. Override `getSleepingDirection()` via Mixin to return player's facing direction

### Equipment Sync

MannequinEntity needs equipment synced from player:
```java
for (EquipmentSlot slot : EquipmentSlot.values()) {
    mannequin.equipStack(slot, player.getEquippedStack(slot).copy());
}
```

---

## Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| MannequinEntity behavior differs from expectation | Prototype in Phase 1 before full implementation |
| Bed placement still needed for SLEEPING direction | Keep simplified bed logic if needed |
| Equipment not syncing properly | Test equipment sync in prototype |
| Client-side rendering issues | Manual testing with user assistance |

---

## Success Criteria

1. `/lay` command shows the player lying down (not invisible)
2. `/spin` command continues to work
3. `/sit` command continues to work
4. No tab list entries created for pose NPCs
5. Skin displays correctly on pose NPCs
6. Equipment displays correctly on pose NPCs
