package net.fill1890.fabsit.mixin.injector;

import net.fill1890.fabsit.mixin.accessor.GuiAccessor;
//? if <26.2 {
import net.minecraft.client.gui.Gui;
//?} else {
/*import net.minecraft.client.gui.Hud;
*///?}
import net.minecraft.world.entity.LivingEntity;
import net.yukulab.fabpose.entity.define.PoseManagerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

//? if <26.2 {
@Mixin(Gui.class)
//?} else {
/*@Mixin(Hud.class)
*///?}
public abstract class GuiMixin {
    @Redirect(
            method = "getVehicleMaxHearts",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;showVehicleHealth()Z"
            )
    )
    private boolean ignorePoseManagerEntity(LivingEntity instance) {
        return instance.showVehicleHealth() && !(instance instanceof PoseManagerEntity);
    }

    /**
     * Redirect the lookup of the player's mounted vehicle so PoseManagerEntity
     * (the invisible armor stand used to seat the player) is never reported
     * as the vehicle whose hearts should be drawn.
     *
     * <p>Targeting {@code getPlayerVehicleWithHealth} works on 1.21.11
     * ({@code renderVehicleHealth(GuiGraphics)}), 26.1
     * ({@code extractVehicleHealth(GuiGraphicsExtractor)}) and 26.2
     * (same methods, moved from {@code Gui} to {@code Hud}) because all call
     * it at the very top and bail out on {@code null}.
     */
    @Redirect(
            method = {"renderVehicleHealth", "extractVehicleHealth"},
            at = @At(
                    value = "INVOKE",
                    //? if <26.2 {
                    target = "Lnet/minecraft/client/gui/Gui;getPlayerVehicleWithHealth()Lnet/minecraft/world/entity/LivingEntity;"
                    //?} else {
                    /*target = "Lnet/minecraft/client/gui/Hud;getPlayerVehicleWithHealth()Lnet/minecraft/world/entity/LivingEntity;"
                    *///?}
            ),
            require = 1
    )
    //? if <26.2 {
    private LivingEntity ignorePoseManagerEntityHealthRendering(Gui instance) {
    //?} else {
    /*private LivingEntity ignorePoseManagerEntityHealthRendering(Hud instance) {
    *///?}
        LivingEntity entity = ((GuiAccessor) instance).fabSit$invokeGetPlayerVehicleWithHealth();
        return entity instanceof PoseManagerEntity ? null : entity;
    }
}
