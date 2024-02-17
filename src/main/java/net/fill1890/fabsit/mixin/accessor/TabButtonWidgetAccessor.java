package net.fill1890.fabsit.mixin.accessor;

import net.minecraft.client.gui.tab.TabManager;
import net.minecraft.client.gui.widget.TabNavigationWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TabNavigationWidget.class)
public interface TabButtonWidgetAccessor {
    @Accessor("tabManager")
    TabManager getTabManager();
}
