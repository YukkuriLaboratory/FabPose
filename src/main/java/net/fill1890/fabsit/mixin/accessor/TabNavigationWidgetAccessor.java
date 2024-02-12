package net.fill1890.fabsit.mixin.accessor;

import net.minecraft.client.gui.tab.TabManager;
import net.minecraft.client.gui.widget.TabButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TabButtonWidget.class)
public interface TabNavigationWidgetAccessor {
    @Accessor("tabManager")
    TabManager getTabManager();
}
