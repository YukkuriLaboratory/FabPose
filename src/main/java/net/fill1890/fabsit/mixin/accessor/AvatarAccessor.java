package net.fill1890.fabsit.mixin.accessor;

import net.minecraft.world.entity.Avatar;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Avatar.class)
public interface AvatarAccessor {
    /**
     * Get NBT location of skin layer data to match skin layers for posers
     *
     * @return NBT location of skin layer data
     */
    @Accessor("DATA_PLAYER_MODE_CUSTOMISATION")
    static EntityDataAccessor<Byte> getPLAYER_MODE_CUSTOMIZATION_ID() {
        throw new AssertionError();
    }

    /**
     * @return NBT location of main arm selector
     */
    @Accessor("DATA_PLAYER_MAIN_HAND")
    static EntityDataAccessor<HumanoidArm> getMAIN_ARM_ID() {
        throw new AssertionError();
    }
}
