package net.fill1890.fabsit.entity;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fill1890.fabsit.FabSit;
import net.fill1890.fabsit.config.ConfigManager;
import net.fill1890.fabsit.util.Messages;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Consumer;

import static net.fill1890.fabsit.mixin.accessor.PlayerEntityAccessor.getLEFT_SHOULDER_ENTITY;
import static net.fill1890.fabsit.mixin.accessor.PlayerEntityAccessor.getRIGHT_SHOULDER_ENTITY;

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
public class PoseManagerEntity extends ArmorStandEntity {
    public static final String ENTITY_ID = "pose_manager";
    public static final EntityDimensions DIMENSIONS = new EntityDimensions(0.5F, 1.975F, true);
    public static final String ENTITY_NAME = "FABSEAT";

    // has the seat been used - checked for removing later
    private boolean used = false;
    // has the action bar status been sent? (needs to be delayed until after addPassenger has executed)
    private boolean statusSent = false;
    private Pose pose = null;
    // visible npc for posing (if needed)
    private PosingEntity poser;

    // block ticking during removal
    // TODO: figure out how to remove this, was here to fix a bug
    protected boolean killing;

    protected ChairPosition position;

    public static Consumer<PoseManagerEntity> getInitializer(Vec3d pos, Pose pose, ServerPlayerEntity player, ChairPosition position) {
        return (entity) -> {
            entity.setPosition(pos.x, pos.y - 1.6, pos.z);
            entity.setYaw(player.getYaw()); // TODO: test this properly
            entity.position = position;
            // if the pose is more complex than sitting, create a posing npc
            if (pose == Pose.LAYING || pose == Pose.SPINNING) {
                // copy player game profile with a random uuid
                GameProfile gameProfile = new GameProfile(UUID.randomUUID(), player.getEntityName());
                gameProfile.getProperties().putAll(player.getGameProfile().getProperties());

                if (pose == Pose.LAYING)
                    entity.poser = new LayingEntity(player, gameProfile, SyncedClientOptions.createDefault());
                if (pose == Pose.SPINNING)
                    entity.poser = new SpinningEntity(player, gameProfile, SyncedClientOptions.createDefault());
            }
            entity.pose = pose;
        };
    }

    public PoseManagerEntity(EntityType<? extends PoseManagerEntity> entityType, World world) {
        super(entityType, world);
        setInvisible(true);
        setInvulnerable(true);
        setCustomName(Text.of(ENTITY_NAME));
        setNoGravity(true);
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);

        // if the pose is npc-based, hide the player when initiated
        if(this.pose == Pose.LAYING || this.pose == Pose.SPINNING) {
            passenger.setInvisible(true);

            // update shoulder entities
            // parrots and such
            // TODO: this should probably be updated in PosingEntity
            this.poser.getDataTracker().set(getLEFT_SHOULDER_ENTITY(), passenger.getDataTracker().get(getLEFT_SHOULDER_ENTITY()));
            this.poser.getDataTracker().set(getRIGHT_SHOULDER_ENTITY(), passenger.getDataTracker().get(getRIGHT_SHOULDER_ENTITY()));
            passenger.getDataTracker().set(getLEFT_SHOULDER_ENTITY(), new NbtCompound());
            passenger.getDataTracker().set(getRIGHT_SHOULDER_ENTITY(), new NbtCompound());
        }

        if(ConfigManager.getConfig().centre_on_blocks || position == ChairPosition.IN_BLOCK)
            ConfigManager.occupiedBlocks.add(this.getBlockPos());

        used = true;
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);

        if(ConfigManager.getConfig().centre_on_blocks || position == ChairPosition.IN_BLOCK)
            ConfigManager.occupiedBlocks.remove(this.getBlockPos());

        // if the pose was npc-based, show the player again when exited
        if(this.pose == Pose.LAYING || this.pose == Pose.SPINNING) {
            passenger.setInvisible(false);

            // replace shoulder entities
            passenger.getDataTracker().set(getLEFT_SHOULDER_ENTITY(), this.poser.getDataTracker().get(getLEFT_SHOULDER_ENTITY()));
            passenger.getDataTracker().set(getRIGHT_SHOULDER_ENTITY(), this.poser.getDataTracker().get(getRIGHT_SHOULDER_ENTITY()));
            this.poser.getDataTracker().set(getLEFT_SHOULDER_ENTITY(), new NbtCompound());
            this.poser.getDataTracker().set(getRIGHT_SHOULDER_ENTITY(), new NbtCompound());
        }
    }

    public void animate(int id) {
        if(this.pose == Pose.LAYING || this.pose == Pose.SPINNING) {
            poser.animate(id);
        }
    }

    @Override
    public boolean collidesWith(Entity entity) {
        return false;
    }

    @Override
    public void kill() {
        this.killing = true;

        // if the pose was npc-based, remove the npc
        if(poser != null) {
            poser.destroy();
        }

        super.kill();
    }

    @Override
    public void tick() {
        if(this.killing) return;

        // kill when the player stops posing
        if(used && getPassengerList().size() < 1) { this.kill(); return; }

        // get the block the player's sitting on
        // if they're sitting on a slab or stair, get that, otherwise block below
        if (position != null) {
            BlockState sittingBlock = getEntityWorld().getBlockState(switch (this.position) {
                case IN_BLOCK -> this.getBlockPos();
                case ON_BLOCK -> this.getBlockPos().down();
            });
            // force player to stand up if the block's been removed
            if (sittingBlock.isAir()) {
                this.kill();
                return;
            }
        }

        // if pose is npc-based, update players with npc info
        if(this.pose == Pose.LAYING || this.pose == Pose.SPINNING) {
            poser.sendUpdates();
        }

        super.tick();
    }

    @Override
    protected void tickControlled(PlayerEntity controllingPlayer, Vec3d movementInput) {
        // rotate the armour stand with the player so the player's legs line up
        setRotation(controllingPlayer.getYaw(), controllingPlayer.getPitch());
        prevYaw = bodyYaw = headYaw = getYaw();
        // send the action bar status if it hasn't been sent yet
        // needs to be delayed or it's overwritten
        if (controllingPlayer instanceof ServerPlayerEntity serverPlayer && !this.statusSent && this.pose != null) {
            if (ConfigManager.getConfig().enable_messages.action_bar)
                serverPlayer.sendMessage(Messages.getPoseStopMessage(serverPlayer, this.pose), true);

            this.statusSent = true;
        }
    }

    /**
     * Remove the entity when server stops
     */
    @Override
    public boolean shouldSave() {
        return false;
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof PlayerEntity player ? player : null;
    }

    public static EntityType<PoseManagerEntity> register() {
        return Registry.register(
                Registries.ENTITY_TYPE,
                new Identifier(FabSit.MOD_ID, ENTITY_ID),
                FabricEntityTypeBuilder.<PoseManagerEntity>create(SpawnGroup.MISC, PoseManagerEntity::new).dimensions(DIMENSIONS).build()
        );
    }
}
