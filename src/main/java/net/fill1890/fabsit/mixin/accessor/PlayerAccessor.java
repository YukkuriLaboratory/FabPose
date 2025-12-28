package net.fill1890.fabsit.mixin.accessor;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.OptionalInt;

/**
 * Accessor for fields of PlayerEntity
 */
@Mixin(Player.class)
public interface PlayerAccessor {
    /**
     * Get NBT location of the left shoulder entity, for syncing parrots with posers
     *
     * @return NBT location of shoulder entity
     */
    @Accessor("DATA_SHOULDER_PARROT_LEFT")
    static EntityDataAccessor<OptionalInt> getLEFT_SHOULDER_PARROT_VARIANT_ID() { throw new AssertionError(); }

    /**
     * Get NBT location of the right shoulder entity, for syncing parrots with posers
     *
     * @return NBT location of shoulder entity
     */
    @Accessor("DATA_SHOULDER_PARROT_RIGHT")
    static EntityDataAccessor<OptionalInt> getRIGHT_SHOULDER_PARROT_VARIANT_ID() { throw new AssertionError(); }
}
