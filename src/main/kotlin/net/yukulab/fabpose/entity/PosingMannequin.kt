package net.yukulab.fabpose.entity

import net.fill1890.fabsit.config.ConfigManager
import net.fill1890.fabsit.mixin.accessor.LivingEntityAccessor
import net.fill1890.fabsit.mixin.accessor.MannequinAccessor
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.Pose
import net.minecraft.world.entity.decoration.Mannequin
import net.minecraft.world.item.component.ResolvableProfile
import net.minecraft.world.level.block.BedBlock
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.properties.BedPart

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
    val mannequin: Mannequin,
    val player: ServerPlayer,
    val pose: Pose,
    private val world: ServerLevel,
    private val bedPos: BlockPos?,
    private val originalBedState: net.minecraft.world.level.block.state.BlockState?,
    private val bedState: net.minecraft.world.level.block.state.BlockState?,
) {

    /**
     * Sync equipment from the player to the mannequin.
     */
    fun syncEquipment() {
        EquipmentSlot.entries.forEach { slot ->
            mannequin.setItemSlot(slot, player.getItemBySlot(slot).copy())
        }
    }

    /**
     * Sync head rotation (yaw/pitch) from the player to the mannequin.
     * For SPIN_ATTACK pose, only sync yaw since pitch is fixed at -90.
     */
    fun syncHeadRotation() {
        if (pose == Pose.SPIN_ATTACK) {
            // For spin attack, keep pitch at -90 but sync yaw
            mannequin.yHeadRot = player.yHeadRot
        } else {
            mannequin.yHeadRot = player.yHeadRot
            mannequin.setXRot(player.xRot)
        }
    }

    /**
     * Send bed block update packet to a player (for SLEEPING pose).
     */
    fun sendBedPacket(target: ServerPlayer) {
        if (bedPos != null && bedState != null) {
            target.connection.send(ClientboundBlockUpdatePacket(bedPos, bedState))
        }
    }

    /**
     * Send bed removal packet to a player (for SLEEPING pose cleanup).
     */
    fun sendBedRemovalPacket(target: ServerPlayer) {
        if (bedPos != null && originalBedState != null) {
            target.connection.send(ClientboundBlockUpdatePacket(bedPos, originalBedState))
        }
    }

    /**
     * Send pivot packet to a player (for SPIN_ATTACK pose).
     */
    fun sendPivotPacket(target: ServerPlayer) {
        if (pose == Pose.SPIN_ATTACK) {
            val pivotPacket = ClientboundMoveEntityPacket.PosRot(
                mannequin.id,
                0.toShort(),
                0.toShort(),
                0.toShort(),
                0.toByte(),
                (-90.0f * 256.0f / 360.0f).toInt().toByte(),
                true,
            )
            target.connection.send(pivotPacket)
        }
    }

    /**
     * Destroy the mannequin and clean up.
     */
    fun destroy() {
        // Send bed removal to all nearby players
        if (bedPos != null && originalBedState != null) {
            world.players().filter { it.hasLineOfSight(mannequin) }.forEach { p ->
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
        fun create(player: ServerPlayer, pose: Pose): PosingMannequin? {
            val world = player.level() as? ServerLevel ?: return null
            val mannequin = Mannequin.create(EntityType.MANNEQUIN, world) ?: return null

            // Set position (optionally centered on block)
            val pos = if (ConfigManager.getConfig().centre_on_blocks) {
                val blockPos = player.blockPosition()
                Triple(blockPos.x + 0.5, blockPos.y.toDouble(), blockPos.z + 0.5)
            } else {
                Triple(player.x, player.y, player.z)
            }

            mannequin.snapTo(pos.first, pos.second, pos.third, player.yRot, 0f)

            // Set player's profile for skin
            val profileComponent = ResolvableProfile.createResolved(player.gameProfile)
            mannequin.entityData.set(MannequinAccessor.getPROFILE(), profileComponent)

            // Set pose
            mannequin.pose = pose

            // Hide the "NPC" label below the name tag by setting description to empty
            mannequin.entityData.set(MannequinAccessor.getDESCRIPTION(), java.util.Optional.empty())

            // Handle SPIN_ATTACK pose - need to set LIVING_FLAGS for riptide spinning
            if (pose == Pose.SPIN_ATTACK) {
                // 0x04 = using riptide flag, makes the entity spin
                mannequin.entityData.set(LivingEntityAccessor.getLIVING_FLAGS(), 0x04.toByte())
                // Set pitch to -90 degrees to make entity spin vertically (upward)
                mannequin.setXRot(-90f)
                // Don't show name for SPIN_ATTACK (position would be wrong due to rotation)
            } else {
                // Set player's name as custom name (visible like player name tag)
                mannequin.customName = player.name
                mannequin.isCustomNameVisible = true
            }

            // Sync equipment
            EquipmentSlot.entries.forEach { slot ->
                mannequin.setItemSlot(slot, player.getItemBySlot(slot).copy())
            }

            // Handle SLEEPING pose specific logic
            var bedPos: BlockPos? = null
            var originalBedState: net.minecraft.world.level.block.state.BlockState? = null
            var bedState: net.minecraft.world.level.block.state.BlockState? = null

            if (pose == Pose.SLEEPING) {
                // Place bed at world bottom + 1 to hide it completely
                bedPos = BlockPos(player.blockX, world.minY + 1, player.blockZ)
                originalBedState = world.getBlockState(bedPos)

                // Create bed state facing player's direction
                val direction = getCardinal(player.yHeadRot)
                bedState = Blocks.WHITE_BED.defaultBlockState()
                    .setValue(BedBlock.PART, BedPart.HEAD)
                    .setValue(BedBlock.FACING, direction.opposite)
            }

            // Spawn the mannequin
            world.addFreshEntity(mannequin)

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
