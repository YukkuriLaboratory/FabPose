package net.fill1890.fabsit.mixin.accessor;

import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
//? if <26.2 {
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
//?} else {
/*import net.minecraft.client.gui.components.tabs.MenuTabBar;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CreateWorldScreen.class)
public interface CreateWorldScreenAccessor {
    //? if <26.2 {
    @Accessor("tabNavigationBar")
    TabNavigationBar getTabNavigationBar();
    //?} else {
    /*@Accessor("tabNavigationBar")
    MenuTabBar getTabNavigationBar();
    *///?}
}
