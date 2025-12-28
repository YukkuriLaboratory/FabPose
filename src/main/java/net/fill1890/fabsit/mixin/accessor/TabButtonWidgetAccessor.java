package net.fill1890.fabsit.mixin.accessor;

import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TabNavigationBar.class)
public interface TabButtonWidgetAccessor {
    @Accessor("tabManager")
    TabManager getTabManager();
}
