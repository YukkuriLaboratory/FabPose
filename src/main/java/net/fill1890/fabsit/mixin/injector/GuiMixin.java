package net.fill1890.fabsit.mixin.injector;

import net.minecraft.client.gui.Gui;
import net.minecraft.world.entity.LivingEntity;
import net.yukulab.fabpose.entity.define.PoseManagerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
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

    @ModifyVariable(
            method = "renderVehicleHealth",
            at = @At("STORE")
    )
    private LivingEntity ignorePoseManagerEntityHealthRendering(LivingEntity entity) {
        return entity instanceof PoseManagerEntity ? null : entity;
    }
}
