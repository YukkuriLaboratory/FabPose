package net.yukulab.fabpose.entity.define

import net.fill1890.fabsit.config.ConfigManager
import net.fill1890.fabsit.entity.ChairPosition
import net.fill1890.fabsit.entity.Pose
import net.fill1890.fabsit.util.Messages
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityPose
import net.minecraft.entity.EntityType
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.TypeFilter
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import net.yukulab.fabpose.entity.PosingMannequin
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
    private var posingMannequin: PosingMannequin? = null

    // Track players who have received bed packets (for SLEEPING pose)
    private val playersWithBedPacket = mutableSetOf<ServerPlayerEntity>()

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
            val mannequin = posingMannequin
            if (mannequin != null && pose in setOf(Pose.LAYING, Pose.SPINNING)) {
                passenger.isInvisible = true
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
            val mannequin = posingMannequin
            if (mannequin != null && pose in setOf(Pose.LAYING, Pose.SPINNING)) {
                passenger.isInvisible = false
                passenger.updatePosition(passenger.x, passenger.y + 0.5, passenger.z)
            }
            // Reset pose state for player sneaking
            passenger.currentPose = null
        }
    }

    fun animate(id: Int) {
        // MannequinEntity handles animations automatically as a real entity
        // No manual packet sending needed
    }

    override fun collidesWith(other: Entity?): Boolean = false

    override fun kill(world: ServerWorld) {
        posingMannequin?.destroy()
        playersWithBedPacket.clear()
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

        // Handle MannequinEntity updates
        val pose = selectedPose
        val mannequin = posingMannequin
        if (mannequin != null && pose in setOf(Pose.LAYING, Pose.SPINNING)) {
            // Sync equipment and head rotation periodically
            mannequin.syncEquipment()
            mannequin.syncHeadRotation()

            // For SLEEPING pose, send bed packets to nearby players
            if (pose == Pose.LAYING && world is ServerWorld) {
                val nearbyPlayers = world.players.filter {
                    it.canSee(mannequin.mannequin) && it !in playersWithBedPacket
                }
                nearbyPlayers.forEach { player ->
                    mannequin.sendBedPacket(player)
                    playersWithBedPacket.add(player)
                }

                // Clean up players who left range
                playersWithBedPacket.removeIf { !it.canSee(mannequin.mannequin) }
            }

            // For SPINNING pose, send pivot packets
            if (pose == Pose.SPINNING && world is ServerWorld) {
                val nearbyPlayers = world.players.filter {
                    it.canSee(mannequin.mannequin) && it !in playersWithBedPacket
                }
                nearbyPlayers.forEach { player ->
                    mannequin.sendPivotPacket(player)
                    playersWithBedPacket.add(player)
                }
                playersWithBedPacket.removeIf { !it.canSee(mannequin.mannequin) }
            }
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

            // if the pose is more complex than sitting, create a posing mannequin
            val pose = playerEntity.currentPose
            if (pose in setOf(Pose.LAYING, Pose.SPINNING)) {
                val entityPose = when (pose) {
                    Pose.LAYING -> EntityPose.SLEEPING
                    Pose.SPINNING -> EntityPose.SPIN_ATTACK
                    else -> null
                }
                if (entityPose != null) {
                    it.posingMannequin = PosingMannequin.create(playerEntity, entityPose)
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
