package net.fill1890.fabsit.mixin.injector;

import net.fill1890.fabsit.entity.PoseManagerEntity;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Redirect(
            method = "getHeartCount",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;isLiving()Z"
            )
    )
    private boolean ignorePoseManagerEntity(LivingEntity instance) {
        return instance.isLiving() && !(instance instanceof PoseManagerEntity);
    }
}
