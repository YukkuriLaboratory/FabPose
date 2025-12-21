package net.fill1890.fabsit.mixin.accessor;

import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.entity.data.TrackedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerLikeEntity.class)
public interface PlayerLikeEntityAccessor {
    /**
     * Get NBT location of skin layer data to match skin layers for posers
     *
     * @return NBT location of skin layer data
     */
    @Accessor("PLAYER_MODE_CUSTOMIZATION_ID")
    static TrackedData<Byte> getPLAYER_MODE_CUSTOMIZATION_ID() {
        throw new AssertionError();
    }

    /**
     * @return NBT location of main arm selector
     */
    @Accessor("MAIN_ARM_ID")
    static TrackedData<Byte> getMAIN_ARM_ID() {
        throw new AssertionError();
    }
}
