package net.fill1890.fabsit.mixin.accessor;

import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Optional;

/**
 * Access properties of MannequinEntity.class
 */
@Mixin(Mannequin.class)
public interface MannequinAccessor {
    /**
     * Accessor for the PROFILE TrackedData field
     * <p>
     * Used to access the profile data of a mannequin entity
     *
     * @return TrackedData for ProfileComponent
     */
    @Accessor("DATA_PROFILE")
    static EntityDataAccessor<ResolvableProfile> getPROFILE() {
        throw new AssertionError();
    }

    /**
     * Accessor for the DESCRIPTION TrackedData field
     * <p>
     * Used to hide or customize the "NPC" label below the name tag
     * Set to Optional.empty() to hide the label
     *
     * @return TrackedData for Optional Text (description)
     */
    @Accessor("DATA_DESCRIPTION")
    static EntityDataAccessor<Optional<Component>> getDESCRIPTION() {
        throw new AssertionError();
    }
}
