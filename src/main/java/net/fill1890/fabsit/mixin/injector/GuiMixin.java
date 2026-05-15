package net.fill1890.fabsit.mixin.injector;

import net.fill1890.fabsit.mixin.accessor.GuiAccessor;
import net.minecraft.client.gui.Gui;
import net.minecraft.world.entity.LivingEntity;
import net.yukulab.fabpose.entity.define.PoseManagerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Gui.class)
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
     * <p>Targeting {@code getPlayerVehicleWithHealth} works on both 1.21.11
     * ({@code renderVehicleHealth(GuiGraphics)}) and 26.1
     * ({@code extractVehicleHealth(GuiGraphicsExtractor)}) because both call
     * it at the very top and bail out on {@code null}.
     */
    @Redirect(
            method = {"renderVehicleHealth", "extractVehicleHealth"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Gui;getPlayerVehicleWithHealth()Lnet/minecraft/world/entity/LivingEntity;"
            ),
            require = 1
    )
    private LivingEntity ignorePoseManagerEntityHealthRendering(Gui instance) {
        LivingEntity entity = ((GuiAccessor) instance).fabSit$invokeGetPlayerVehicleWithHealth();
        return entity instanceof PoseManagerEntity ? null : entity;
    }
}
