package net.fill1890.fabsit.mixin.accessor;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Optional;

/**
 * Accessor for LivingEntity
 */
@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    /**
     * Get the NBT data location for the sleeping position to manually set it, for sleeping entity
     *
     * @return NBT location of sleeping position
     */
    @Accessor("SLEEPING_POS_ID")
    static EntityDataAccessor<Optional<BlockPos>> getSLEEPING_POSITION() {
        throw new AssertionError();
    }

    /**
     * Get the NBT data location for living flags, to make posing entity spin
     *
     * @return NBT location of living flags
     */
    @Accessor("DATA_LIVING_ENTITY_FLAGS")
    static EntityDataAccessor<Byte> getLIVING_FLAGS() { throw new AssertionError(); }
}
