package net.yukulab.fabpose.extension

import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toKotlinInstant
import kotlinx.coroutines.launch
import me.lucko.fabric.api.permissions.v0.Permissions
import net.fill1890.fabsit.config.ConfigManager
import net.fill1890.fabsit.entity.ChairPosition
import net.fill1890.fabsit.entity.Pose
import net.fill1890.fabsit.error.PoseException
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.phys.Vec3
import net.yukulab.fabpose.entity.FabSitEntities
import net.yukulab.fabpose.entity.define.PoseManagerEntity
import net.yukulab.fabpose.serverScope

@JvmOverloads
fun ServerPlayer.pose(pose: Pose, targetSitPos: Vec3? = null, chairPosition: ChairPosition = ChairPosition.ON_BLOCK, checkSpam: Boolean = true): Result<Unit> = runCatching {
    pose.confirmEnabled()

    if (!Permissions.check(this, pose.getPermissionName(), true)) {
        throw PoseException.PermissionException()
    }

    val now = Clock.System.now()
    val lastUse = lastPoseTime?.toKotlinInstant()
    if (checkSpam && lastUse != null && (now - lastUse) < 500.milliseconds) {
        throw PoseException.TooQuickly()
    }

    if (currentPose != null) {
        currentPose = null
        return@runCatching
    }

    val sitPos = targetSitPos ?: run {
        if (ConfigManager.getConfig().centre_on_blocks) {
            Vec3.atLowerCornerWithOffset(blockPosition(), 0.5, 0.0, 0.5)
        } else {
            position()
        }
    }

    canPose().getOrThrow()

    currentPose = pose
    if (pose == Pose.SWIMMING) {
        isSwimming = true
    } else {
        val chair = FabSitEntities.POSE_MANAGER.spawn(
            level(),
            PoseManagerEntity.getInitializer(sitPos, this, chairPosition),
            blockPosition(),
            EntitySpawnReason.COMMAND,
            false,
            false,
        )
        if (chair == null) {
            throw PoseException.StateException("Failed to spawn chair")
        }

        // Adding delay to sync entity with client
        serverScope.launch {
            startRiding(chair, true, false)
        }
    }
}

/**
 * Check if a player can currently perform a given pose
 *
 * On a successful return, pose is valid
 * If pose is invalid, will send the relevant message to the player and throw an exception
 *
 * @return Result.failed<[PoseException]> is pose is not valid
 */
fun ServerPlayer.canPose(): Result<Unit> = runCatching {
    // check if spectating
    if (isSpectator) throw PoseException.SpectatorException()

    val config = ConfigManager.getConfig()
    // check if underwater
    if (isInLiquid && !config.allow_posing_underwater) {
        throw PoseException.StateException("Cannot pose underwater")
    }

    // check if flying, jumping, swimming, sleeping, or underwater
    if (isFallFlying || deltaMovement.y > 0 || isSwimming || isSleeping) {
        throw PoseException.StateException("Cannot pose while flying, jumping, swimming, or sleeping")
    }

    val standingBlock = blockStateOn
    // check if in midair
    if (standingBlock.isAir && !config.allow_posing_midair) {
        throw PoseException.MidairException()
    }

    if (config.centre_on_blocks || config.right_click_sit && PoseManagerEntity.isOccupied(level(), onPos)) {
        throw PoseException.BlockOccupied()
    }
}
