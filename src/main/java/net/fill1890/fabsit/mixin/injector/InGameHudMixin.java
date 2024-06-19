package net.fill1890.fabsit.mixin.injector;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.LivingEntity;
import net.yukulab.fabpose.entity.define.PoseManagerEntity;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Debug(export = true)
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

    @ModifyVariable(
            method = "renderMountHealth",
            at = @At("STORE")
    )
    private LivingEntity ignorePoseManagerEntityHealthRendering(LivingEntity entity) {
        return entity instanceof PoseManagerEntity ? null : entity;
    }
}
