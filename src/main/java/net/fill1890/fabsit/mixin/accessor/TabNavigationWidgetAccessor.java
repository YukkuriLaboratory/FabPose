package net.fill1890.fabsit.mixin.accessor;

import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.TabButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TabButton.class)
public interface TabNavigationWidgetAccessor {
    @Accessor("tabManager")
    TabManager getTabManager();
}
