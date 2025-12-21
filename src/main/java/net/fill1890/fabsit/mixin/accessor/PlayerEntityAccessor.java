package net.fill1890.fabsit.mixin.accessor;

import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.OptionalInt;

/**
 * Accessor for fields of PlayerEntity
 */
@Mixin(PlayerEntity.class)
public interface PlayerEntityAccessor {
    /**
     * Get NBT location of the left shoulder entity, for syncing parrots with posers
     *
     * @return NBT location of shoulder entity
     */
    @Accessor("LEFT_SHOULDER_PARROT_VARIANT_ID")
    static TrackedData<OptionalInt> getLEFT_SHOULDER_PARROT_VARIANT_ID() { throw new AssertionError(); }

    /**
     * Get NBT location of the right shoulder entity, for syncing parrots with posers
     *
     * @return NBT location of shoulder entity
     */
    @Accessor("RIGHT_SHOULDER_PARROT_VARIANT_ID")
    static TrackedData<OptionalInt> getRIGHT_SHOULDER_PARROT_VARIANT_ID() { throw new AssertionError(); }
}
