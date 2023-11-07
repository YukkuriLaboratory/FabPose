package net.fill1890.fabsit.mixin.injector;

import net.fill1890.fabsit.entity.Pose;
import net.fill1890.fabsit.entity.PoseManagerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.EnumSet;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Redirect(
            method = "updatePotionVisibility",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;setInvisible(Z)V"
            )
    )
    private void preventVisibilityUpdateWhenPosing(LivingEntity instance, boolean b) {
        if (instance instanceof PlayerEntity player && player.getVehicle() instanceof PoseManagerEntity poseManager && EnumSet.of(Pose.LAYING, Pose.SPINNING).contains(poseManager.getCustomPose())) {
            instance.setInvisible(true);
        } else {
            instance.setInvisible(b);
        }
    }
}
