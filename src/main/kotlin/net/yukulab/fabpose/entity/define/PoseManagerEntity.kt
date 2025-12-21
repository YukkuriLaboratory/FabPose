package net.yukulab.fabpose.entity.define

import com.mojang.authlib.GameProfile
import java.util.OptionalInt
import java.util.UUID
import net.fill1890.fabsit.config.ConfigManager
import net.fill1890.fabsit.entity.ChairPosition
import net.fill1890.fabsit.entity.LayingEntity
import net.fill1890.fabsit.entity.Pose
import net.fill1890.fabsit.entity.PosingEntity
import net.fill1890.fabsit.entity.SpinningEntity
import net.fill1890.fabsit.mixin.accessor.PlayerEntityAccessor
import net.fill1890.fabsit.util.Messages
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.packet.c2s.common.SyncedClientOptions
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.TypeFilter
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import net.yukulab.fabpose.extension.currentPose

/**
 * The PoseManagerEntity provides an interface to a variety of posing actions, currently:
 * <pre>entity.Pose.SITTING</pre>
 * <pre>entity.Pose.LAYING</pre>
 * <pre>entity.Pose.SPINNING</pre>
 * <br>
 * The manager spawns in invisible armour stand for the player to ride to sit
 * <br>
 * If needed, the player will then be made invisible and an NPC spawned to pose instead
 */
class PoseManagerEntity(entityType: EntityType<out PoseManagerEntity>, world: World) : ArmorStandEntity(entityType, world) {
    private var owner: PlayerEntity? = null

    // Storing pose data even if player reset pose from [ServerPlayerEntity.pose]
    private var selectedPose: Pose? = null
    private var poser: PosingEntity? = null

    var chairPosition: ChairPosition? = null
        private set

    init {
        isInvisible = true
        isInvulnerable = true
        customName = Text.of("FABSEAT")
        setNoGravity(true)
    }

    override fun addPassenger(passenger: Entity?) {
        super.addPassenger(passenger)

        if (passenger is PlayerEntity) {
            owner = passenger
            val pose = selectedPose
            val poseEntity = poser
            if (poseEntity != null && pose in setOf(Pose.LAYING, Pose.SPINNING)) {
                passenger.isInvisible = true

                val poserDataTracker = poseEntity.dataTracker
                val passengerDataTracker = passenger.dataTracker
                PlayerEntityAccessor.getLEFT_SHOULDER_PARROT_VARIANT_ID().also {
                    poserDataTracker.set(it, passengerDataTracker.get(it))
                    passengerDataTracker.set(it, OptionalInt.empty())
                }
                PlayerEntityAccessor.getRIGHT_SHOULDER_PARROT_VARIANT_ID().also {
                    poserDataTracker.set(it, passengerDataTracker.get(it))
                    passengerDataTracker.set(it, OptionalInt.empty())
                }
            }

            if (passenger is ServerPlayerEntity && pose != null && ConfigManager.getConfig().enable_messages.action_bar) {
                passenger.sendMessage(Messages.getPoseStopMessage(passenger, pose), true)
            }
        }
    }

    override fun removePassenger(passenger: Entity?) {
        super.removePassenger(passenger)

        if (passenger is PlayerEntity) {
            val pose = selectedPose
            val poseEntity = poser
            if (poseEntity != null && pose in setOf(Pose.LAYING, Pose.SPINNING)) {
                passenger.isInvisible = false

                val poserDataTracker = poseEntity.dataTracker
                val passengerDataTracker = passenger.dataTracker
                PlayerEntityAccessor.getLEFT_SHOULDER_PARROT_VARIANT_ID().also {
                    passengerDataTracker.set(it, poserDataTracker.get(it))
                    poserDataTracker.set(it, OptionalInt.empty())
                }
                PlayerEntityAccessor.getRIGHT_SHOULDER_PARROT_VARIANT_ID().also {
                    passengerDataTracker.set(it, poserDataTracker.get(it))
                    poserDataTracker.set(it, OptionalInt.empty())
                }
                passenger.updatePosition(passenger.x, passenger.y + 0.5, passenger.z)
            }
            // Reset pose state for player sneaking
            passenger.currentPose = null
        }
    }

    fun animate(id: Int) {
        val pose = selectedPose
        if (pose in setOf(Pose.LAYING, Pose.SPINNING)) {
            poser?.animate(id)
        }
    }

    override fun collidesWith(other: Entity?): Boolean = false

    override fun kill(world: ServerWorld) {
        poser?.destroy()
        super.kill(world)
    }

    override fun tick() {
        if (isRemoved) return

        // kill when the player stops posing
        val world = entityWorld
        if (passengerList.isEmpty() && owner != null && world is ServerWorld) {
            kill(world)
            return
        }

        // if pose is npc-based, update players with npc info
        val pose = selectedPose
        val poseEntity = poser
        if (poseEntity != null && pose in setOf(Pose.LAYING, Pose.SPINNING)) {
            poseEntity.sendUpdates()
        }
        super.tick()
    }

    override fun tickControlled(controllingPlayer: PlayerEntity?, movementInput: Vec3d?) {
        if (controllingPlayer == null) return

        // rotate the armour stand with the player so the player's legs line up
        setRotation(controllingPlayer.yaw, controllingPlayer.pitch)
        lastYaw = yaw
        bodyYaw = yaw
        headYaw = yaw
    }

    /**
     * Remove the entity when server stops
     */
    override fun shouldSave(): Boolean = false

    override fun getControllingPassenger(): LivingEntity? = firstPassenger as? PlayerEntity

    companion object {
        fun getInitializer(pos: Vec3d, playerEntity: ServerPlayerEntity, position: ChairPosition): (PoseManagerEntity) -> Unit = {
            it.setPosition(pos.x, pos.y - 1.88, pos.z)
            it.yaw = playerEntity.yaw
            it.chairPosition = position
            it.selectedPose = playerEntity.currentPose

            // if the pose is more complex than sitting, create a posing npc
            val pose = playerEntity.currentPose
            if (pose in setOf(Pose.LAYING, Pose.SPINNING)) {
                val gameProfile = GameProfile(UUID.randomUUID(), playerEntity.nameForScoreboard, playerEntity.gameProfile.properties)

                if (pose == Pose.LAYING) {
                    it.poser = LayingEntity(playerEntity, gameProfile, SyncedClientOptions.createDefault())
                } else if (pose == Pose.SPINNING) {
                    it.poser = SpinningEntity(playerEntity, gameProfile, SyncedClientOptions.createDefault())
                }
            }
        }

        @JvmStatic
        fun isOccupied(world: World, pos: BlockPos): Boolean {
            val box = Box.from(Vec3d.of(pos))
            return world.getEntitiesByType(TypeFilter.instanceOf(PoseManagerEntity::class.java), box) {
                it.chairPosition == ChairPosition.IN_BLOCK
            }.isNotEmpty()
        }
    }
}
