package net.fill1890.fabsit.mixin.accessor;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Access properties of Entity.class
 */
@Mixin(Entity.class)
public interface EntityAccessor {
    /**
     * Accessor for the NBT data for an entity pose
     * <p>
     * Used to get the NBT location for setting a specific pose
     *
     * @return NBT pose data
     */
    @Accessor("DATA_POSE")
    static EntityDataAccessor<Pose> getPOSE() {
        throw new AssertionError();
    }

    @Accessor("entityData")
    SynchedEntityData getEntityData();
}
