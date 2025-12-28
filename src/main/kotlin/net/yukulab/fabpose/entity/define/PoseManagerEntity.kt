package net.yukulab.fabpose.entity.define

import net.fill1890.fabsit.config.ConfigManager
import net.fill1890.fabsit.entity.ChairPosition
import net.fill1890.fabsit.entity.Pose
import net.fill1890.fabsit.util.Messages
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Pose as EntityPose
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.entity.EntityTypeTest
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
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
class PoseManagerEntity(entityType: EntityType<out PoseManagerEntity>, world: Level) : ArmorStand(entityType, world) {
    private var owner: Player? = null

    // Storing pose data even if player reset pose from [ServerPlayerEntity.pose]
    private var selectedPose: Pose? = null
    private var posingMannequin: PosingMannequin? = null

    // Track players who have received bed packets (for SLEEPING pose)
    private val playersWithBedPacket = mutableSetOf<ServerPlayer>()

    var chairPosition: ChairPosition? = null
        private set

    init {
        isInvisible = true
        isInvulnerable = true
        customName = Component.nullToEmpty("FABSEAT")
        setNoGravity(true)
    }

    override fun addPassenger(passenger: Entity) {
        super.addPassenger(passenger)

        if (passenger is Player) {
            owner = passenger
            val pose = selectedPose
            val mannequin = posingMannequin
            if (mannequin != null && pose in setOf(Pose.LAYING, Pose.SPINNING)) {
                passenger.isInvisible = true
            }

            if (passenger is ServerPlayer && pose != null && ConfigManager.getConfig().enable_messages.action_bar) {
                passenger.displayClientMessage(Messages.getPoseStopMessage(passenger, pose), true)
            }
        }
    }

    override fun removePassenger(passenger: Entity) {
        super.removePassenger(passenger)

        if (passenger is Player) {
            val pose = selectedPose
            val mannequin = posingMannequin
            if (mannequin != null && pose in setOf(Pose.LAYING, Pose.SPINNING)) {
                passenger.isInvisible = false
                passenger.absSnapTo(passenger.x, passenger.y + 0.5, passenger.z)
            }
            // Reset pose state for player sneaking
            passenger.currentPose = null
        }
    }

    fun animate(id: Int) {
        // MannequinEntity handles animations automatically as a real entity
        // No manual packet sending needed
    }

    override fun canCollideWith(other: Entity): Boolean = false

    override fun kill(world: ServerLevel) {
        posingMannequin?.destroy()
        playersWithBedPacket.clear()
        super.kill(world)
    }

    override fun tick() {
        if (isRemoved) return

        // kill when the player stops posing
        val world = level()
        if (passengers.isEmpty() && owner != null && world is ServerLevel) {
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
            if (pose == Pose.LAYING && world is ServerLevel) {
                val nearbyPlayers = world.players().filter {
                    it.hasLineOfSight(mannequin.mannequin) && it !in playersWithBedPacket
                }
                nearbyPlayers.forEach { player ->
                    mannequin.sendBedPacket(player)
                    playersWithBedPacket.add(player)
                }

                // Clean up players who left range
                playersWithBedPacket.removeIf { !it.hasLineOfSight(mannequin.mannequin) }
            }

            // For SPINNING pose, send pivot packets
            if (pose == Pose.SPINNING && world is ServerLevel) {
                val nearbyPlayers = world.players().filter {
                    it.hasLineOfSight(mannequin.mannequin) && it !in playersWithBedPacket
                }
                nearbyPlayers.forEach { player ->
                    mannequin.sendPivotPacket(player)
                    playersWithBedPacket.add(player)
                }
                playersWithBedPacket.removeIf { !it.hasLineOfSight(mannequin.mannequin) }
            }
        }
        super.tick()
    }

    override fun tickRidden(controllingPlayer: Player, movementInput: Vec3) {
        if (controllingPlayer == null) return

        // rotate the armour stand with the player so the player's legs line up
        setRot(controllingPlayer.yRot, controllingPlayer.xRot)
        yRotO = yRot
        yBodyRot = yRot
        yHeadRot = yRot
    }

    /**
     * Remove the entity when server stops
     */
    override fun shouldBeSaved(): Boolean = false

    override fun getControllingPassenger(): LivingEntity? = firstPassenger as? Player

    companion object {
        fun getInitializer(pos: Vec3, playerEntity: ServerPlayer, position: ChairPosition): (PoseManagerEntity) -> Unit = {
            it.setPos(pos.x, pos.y - 1.88, pos.z)
            it.setYRot(playerEntity.yRot)
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
        fun isOccupied(world: Level, pos: BlockPos): Boolean {
            val box = AABB.unitCubeFromLowerCorner(Vec3.atLowerCornerOf(pos))
            return world.getEntities(EntityTypeTest.forClass(PoseManagerEntity::class.java), box) {
                it.chairPosition == ChairPosition.IN_BLOCK
            }.isNotEmpty()
        }
    }
}
