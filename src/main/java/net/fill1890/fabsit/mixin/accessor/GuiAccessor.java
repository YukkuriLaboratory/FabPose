package net.fill1890.fabsit.mixin.accessor;

//? if <26.2 {
import net.minecraft.client.gui.Gui;
//?} else {
/*import net.minecraft.client.gui.Hud;
*///?}
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

//? if <26.2 {
@Mixin(Gui.class)
//?} else {
/*@Mixin(Hud.class)
*///?}
public interface GuiAccessor {
    @Invoker("getPlayerVehicleWithHealth")
    LivingEntity fabSit$invokeGetPlayerVehicleWithHealth();
}
