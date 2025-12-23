package net.fill1890.fabsit.mixin.accessor;

import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.decoration.MannequinEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Optional;

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

    /**
     * Accessor for the DESCRIPTION TrackedData field
     * <p>
     * Used to hide or customize the "NPC" label below the name tag
     * Set to Optional.empty() to hide the label
     *
     * @return TrackedData for Optional Text (description)
     */
    @Accessor("DESCRIPTION")
    static TrackedData<Optional<Text>> getDESCRIPTION() {
        throw new AssertionError();
    }
}
