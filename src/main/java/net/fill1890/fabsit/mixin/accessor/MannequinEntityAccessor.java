package net.fill1890.fabsit.mixin.accessor;

import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.decoration.MannequinEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Access properties of MannequinEntity.class
 */
@Mixin(MannequinEntity.class)
public interface MannequinEntityAccessor {
    /**
     * Accessor for the PROFILE TrackedData field
     * <p>
     * Used to access the profile data of a mannequin entity
     *
     * @return TrackedData for ProfileComponent
     */
    @Accessor("PROFILE")
    static TrackedData<ProfileComponent> getPROFILE() {
        throw new AssertionError();
    }
}
