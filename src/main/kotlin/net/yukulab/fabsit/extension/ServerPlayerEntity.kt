package net.yukulab.fabsit.extension

import kotlin.time.Duration.Companion.milliseconds
import kotlinx.datetime.Clock
import kotlinx.datetime.toKotlinInstant
import net.fill1890.fabsit.FabSit
import net.fill1890.fabsit.config.ConfigManager
import net.fill1890.fabsit.entity.ChairPosition
import net.fill1890.fabsit.entity.Pose
import net.fill1890.fabsit.entity.PoseManagerEntity
import net.fill1890.fabsit.error.PoseException
import net.minecraft.entity.SpawnReason
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.Vec3d

@JvmOverloads
fun ServerPlayerEntity.pose(pose: Pose, targetSitPos: Vec3d? = null, chairPosition: ChairPosition = ChairPosition.ON_BLOCK): Result<Unit> =
    runCatching {
        pose.confirmEnabled()

        val now = Clock.System.now()
        val lastUse = lastPoseTime?.toKotlinInstant()
        if (lastUse != null && (now - lastUse) < 500.milliseconds) {
            throw PoseException.TooQuickly()
        }

        if (currentPose != null) {
            currentPose = null
            // isSwimming = false
            return@runCatching
        }

        val sitPos = targetSitPos ?: run {
            if (ConfigManager.getConfig().centre_on_blocks) {
                Vec3d.add(blockPos, 0.5, 0.0, 0.5)
            } else {
                pos
            }
        }

        canPose().getOrThrow()

        currentPose = pose
        if (pose == Pose.SWIMMING) {
            isSwimming = true
        } else {
            val chair = FabSit.RAW_CHAIR_ENTITY_TYPE.spawn(
                serverWorld,
                null,
                PoseManagerEntity.getInitializer(sitPos, pose, this, chairPosition),
                blockPos,
                SpawnReason.COMMAND,
                false,
                false,
            )
            startRiding(chair, true)
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
fun ServerPlayerEntity.canPose(): Result<Unit> = runCatching {
    // check if spectating
    if (isSpectator) throw PoseException.SpectatorException()

    val config = ConfigManager.getConfig()
    // check if underwater
    if (isInsideWaterOrBubbleColumn && !config.allow_posing_underwater) {
        throw PoseException.StateException()
    }

    // check if flying, jumping, swimming, sleeping, or underwater
    if (isFallFlying || velocity.y > 0 || isSwimming || isSleeping) {
        throw PoseException.StateException()
    }

    val standingBlock = steppingBlockState
    // check if in midair
    if (standingBlock.isAir && !config.allow_posing_midair) {
        throw PoseException.MidairException()
    }

    if (config.centre_on_blocks || config.right_click_sit && ConfigManager.occupiedBlocks.contains(steppingPos)) {
        throw PoseException.BlockOccupied()
    }
}
