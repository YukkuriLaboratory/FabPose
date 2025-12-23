package net.yukulab.fabpose.entity

import net.fill1890.fabsit.config.ConfigManager
import net.fill1890.fabsit.mixin.accessor.LivingEntityAccessor
import net.fill1890.fabsit.mixin.accessor.MannequinEntityAccessor
import net.minecraft.block.BedBlock
import net.minecraft.block.Blocks
import net.minecraft.block.enums.BedPart
import net.minecraft.component.type.ProfileComponent
import net.minecraft.entity.EntityPose
import net.minecraft.entity.EntityType
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.decoration.MannequinEntity
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.EntityS2CPacket
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

/**
 * Helper class for managing a MannequinEntity that visually represents a posing player.
 *
 * Unlike the old PosingEntity (which extended ServerPlayerEntity and required complex
 * tab list management), MannequinEntity is a standard entity type that:
 * - Spawns normally in the world (no tab list packets needed)
 * - Uses ProfileComponent for automatic skin loading
 * - Supports poses natively (SLEEPING, SWIMMING, CROUCHING, GLIDING, SPIN_ATTACK)
 */
class PosingMannequin private constructor(
    val mannequin: MannequinEntity,
    val player: ServerPlayerEntity,
    val pose: EntityPose,
    private val world: ServerWorld,
    private val bedPos: BlockPos?,
    private val originalBedState: net.minecraft.block.BlockState?,
    private val bedState: net.minecraft.block.BlockState?,
) {

    /**
     * Sync equipment from the player to the mannequin.
     */
    fun syncEquipment() {
        EquipmentSlot.entries.forEach { slot ->
            mannequin.equipStack(slot, player.getEquippedStack(slot).copy())
        }
    }

    /**
     * Sync head rotation (yaw/pitch) from the player to the mannequin.
     * For SPIN_ATTACK pose, only sync yaw since pitch is fixed at -90.
     */
    fun syncHeadRotation() {
        if (pose == EntityPose.SPIN_ATTACK) {
            // For spin attack, keep pitch at -90 but sync yaw
            mannequin.headYaw = player.headYaw
        } else {
            mannequin.headYaw = player.headYaw
            mannequin.pitch = player.pitch
        }
    }

    /**
     * Send bed block update packet to a player (for SLEEPING pose).
     */
    fun sendBedPacket(target: ServerPlayerEntity) {
        if (bedPos != null && bedState != null) {
            target.networkHandler.sendPacket(BlockUpdateS2CPacket(bedPos, bedState))
        }
    }

    /**
     * Send bed removal packet to a player (for SLEEPING pose cleanup).
     */
    fun sendBedRemovalPacket(target: ServerPlayerEntity) {
        if (bedPos != null && originalBedState != null) {
            target.networkHandler.sendPacket(BlockUpdateS2CPacket(bedPos, originalBedState))
        }
    }

    /**
     * Send pivot packet to a player (for SPIN_ATTACK pose).
     */
    fun sendPivotPacket(target: ServerPlayerEntity) {
        if (pose == EntityPose.SPIN_ATTACK) {
            val pivotPacket = EntityS2CPacket.RotateAndMoveRelative(
                mannequin.id,
                0.toShort(),
                0.toShort(),
                0.toShort(),
                0.toByte(),
                (-90.0f * 256.0f / 360.0f).toInt().toByte(),
                true,
            )
            target.networkHandler.sendPacket(pivotPacket)
        }
    }

    /**
     * Destroy the mannequin and clean up.
     */
    fun destroy() {
        // Send bed removal to all nearby players
        if (bedPos != null && originalBedState != null) {
            world.players.filter { it.canSee(mannequin) }.forEach { p ->
                sendBedRemovalPacket(p)
            }
        }

        // Silently remove the mannequin (no death sound)
        mannequin.discard()
    }

    companion object {
        /**
         * Create a PosingMannequin for the given player and pose.
         *
         * @param player The player to create a mannequin for
         * @param pose The pose the mannequin should take
         * @return The created PosingMannequin, or null if creation failed
         */
        fun create(player: ServerPlayerEntity, pose: EntityPose): PosingMannequin? {
            val world = player.entityWorld as? ServerWorld ?: return null
            val mannequin = MannequinEntity.create(EntityType.MANNEQUIN, world) ?: return null

            // Set position (optionally centered on block)
            val pos = if (ConfigManager.getConfig().centre_on_blocks) {
                val blockPos = player.blockPos
                Triple(blockPos.x + 0.5, blockPos.y.toDouble(), blockPos.z + 0.5)
            } else {
                Triple(player.x, player.y, player.z)
            }

            mannequin.refreshPositionAndAngles(pos.first, pos.second, pos.third, player.yaw, 0f)

            // Set player's profile for skin
            val profileComponent = ProfileComponent.ofStatic(player.gameProfile)
            mannequin.dataTracker.set(MannequinEntityAccessor.getPROFILE(), profileComponent)

            // Set pose
            mannequin.pose = pose

            // Hide the "NPC" label below the name tag by setting description to empty
            mannequin.dataTracker.set(MannequinEntityAccessor.getDESCRIPTION(), java.util.Optional.empty())

            // Handle SPIN_ATTACK pose - need to set LIVING_FLAGS for riptide spinning
            if (pose == EntityPose.SPIN_ATTACK) {
                // 0x04 = using riptide flag, makes the entity spin
                mannequin.dataTracker.set(LivingEntityAccessor.getLIVING_FLAGS(), 0x04.toByte())
                // Set pitch to -90 degrees to make entity spin vertically (upward)
                mannequin.pitch = -90f
                // Don't show name for SPIN_ATTACK (position would be wrong due to rotation)
            } else {
                // Set player's name as custom name (visible like player name tag)
                mannequin.customName = player.name
                mannequin.isCustomNameVisible = true
            }

            // Sync equipment
            EquipmentSlot.entries.forEach { slot ->
                mannequin.equipStack(slot, player.getEquippedStack(slot).copy())
            }

            // Handle SLEEPING pose specific logic
            var bedPos: BlockPos? = null
            var originalBedState: net.minecraft.block.BlockState? = null
            var bedState: net.minecraft.block.BlockState? = null

            if (pose == EntityPose.SLEEPING) {
                // Place bed at world bottom + 1 to hide it completely
                bedPos = BlockPos(player.blockX, world.bottomY + 1, player.blockZ)
                originalBedState = world.getBlockState(bedPos)

                // Create bed state facing player's direction
                val direction = getCardinal(player.headYaw)
                bedState = Blocks.WHITE_BED.defaultState
                    .with(BedBlock.PART, BedPart.HEAD)
                    .with(BedBlock.FACING, direction.opposite)
            }

            // Spawn the mannequin
            world.spawnEntity(mannequin)

            return PosingMannequin(
                mannequin = mannequin,
                player = player,
                pose = pose,
                world = world,
                bedPos = bedPos,
                originalBedState = originalBedState,
                bedState = bedState,
            )
        }

        /**
         * Get the cardinal direction for a given head yaw.
         */
        private fun getCardinal(yaw: Float): Direction = when {
            yaw >= -45 && yaw <= 45 -> Direction.SOUTH
            yaw > 45 && yaw <= 135 -> Direction.WEST
            yaw >= -135 && yaw < -45 -> Direction.EAST
            else -> Direction.NORTH
        }
    }
}
