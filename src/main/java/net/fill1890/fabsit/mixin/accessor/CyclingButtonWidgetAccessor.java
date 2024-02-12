package net.fill1890.fabsit.mixin.accessor;

import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CyclingButtonWidget.class)
public interface CyclingButtonWidgetAccessor {
    @Accessor("optionText")
    Text getOptionText();
}
