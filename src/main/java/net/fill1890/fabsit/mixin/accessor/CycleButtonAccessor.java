package net.fill1890.fabsit.mixin.accessor;

import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CycleButton.class)
public interface CycleButtonAccessor {
    @Accessor("name")
    Component getName();
}
