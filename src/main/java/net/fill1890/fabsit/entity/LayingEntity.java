package net.fill1890.fabsit.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityPosition;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.Set;

import static net.fill1890.fabsit.mixin.accessor.EntityAccessor.getPOSE;
import static net.fill1890.fabsit.mixin.accessor.LivingEntityAccessor.getSLEEPING_POSITION;

/**
 * Laying entity
 * <br>
 * Subclasses posing entity and implements a sleeping pose
 * <br>
 * Note that to do this, a bed is placed one block below the player's feet client-side
 * and set as the poser's bed. This is packet-based only and does not affect the world
 * server-side, and is reset when the pose is left.
 * <br>
 * The bed position is set close to the player because Minecraft's client-side
 * {@code LivingEntity.onTrackedDataSet()} calls {@code setPositionInBed()} when
 * SLEEPING_POSITION is updated, which teleports the entity to the bed position.
 * By placing the bed near the player, this teleportation has minimal effect.
 */
public class LayingEntity extends PosingEntity {
    // replace a block with a bed
    private final BlockUpdateS2CPacket addBedPacket;
    // revert bed replacement
    private final BlockUpdateS2CPacket removeBedPacket;
    // teleport the poser to the correct location
    private final EntityPositionS2CPacket teleportPoserPacket;

    public LayingEntity(ServerPlayerEntity player, GameProfile profile, SyncedClientOptions clientOptions) {
        super(player, profile, clientOptions);

        // set sleeping pose; mixin is again used to access entity data
        this.getDataTracker().set(getPOSE(), EntityPose.SLEEPING);

        // Place bed one block below the player's feet.
        // This is important because Minecraft's client calls setPositionInBed() when
        // SLEEPING_POSITION is updated via data tracker, which teleports the entity to:
        // (bedPos.x + 0.5, bedPos.y + 0.6875, bedPos.z + 0.5)
        // By placing the bed near the player, the resulting position stays close to
        // the player's actual position, preventing them from being teleported to
        // the world bottom (minY) and becoming invisible.
        BlockPos bedPos = getBlockPos().down();
        // set the sleeping position of the poser to the bed
        this.getDataTracker().set(getSLEEPING_POSITION(), Optional.of(bedPos));

        // get the top half of a bed to replace the old block with
        BlockState bed = Blocks.WHITE_BED.getDefaultState().with(BedBlock.PART, BedPart.HEAD);
        // save the old block to restore it later
        BlockState old = this.getEntityWorld().getBlockState(bedPos);

        // update bed facing direction to match player
        bed = bed.with(BedBlock.FACING, this.initialDirection.getOpposite());

        // Set position to match where setPositionInBed() will place the entity.
        // setPositionInBed() sets Y to bedPos.y + 0.6875 (bed height offset).
        // This ensures the entity doesn't visibly jump when the client processes
        // the SLEEPING_POSITION data tracker update.
        double targetY = bedPos.getY() + 0.6875;
        this.setPosition(this.getX(), targetY, this.getZ());

        this.addBedPacket = new BlockUpdateS2CPacket(bedPos, bed);
        this.removeBedPacket = new BlockUpdateS2CPacket(bedPos, old);
        // teleport the poser to maintain the correct position after client-side
        // setPositionInBed() is called
        this.teleportPoserPacket = EntityPositionS2CPacket.create(getId(), EntityPosition.fromEntity(this), Set.of(), isOnGround());
        // refresh metadata so the bed is assigned correctly
        this.trackerPoserPacket = new EntityTrackerUpdateS2CPacket(this.getId(), this.getDataTracker().getChangedEntries());

    }

    @Override
    public void sendUpdates() {
        super.sendUpdates();

        this.addingPlayers.forEach(p -> {
            // add the bed to the world
            p.networkHandler.sendPacket(addBedPacket);
            // refresh metadata now that the bed exists
            p.networkHandler.sendPacket(trackerPoserPacket);
            // teleport the poser to the surface
            p.networkHandler.sendPacket(teleportPoserPacket);
        });

        // reset bed blocks after posing
        this.removingPlayers.forEach(p -> p.networkHandler.sendPacket(removeBedPacket));
    }

    @Override
    public void destroy() {
        this.updatingPlayers.forEach(p -> p.networkHandler.sendPacket(removeBedPacket));

        super.destroy();
    }
}
