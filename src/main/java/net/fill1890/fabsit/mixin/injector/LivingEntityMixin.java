package net.fill1890.fabsit.mixin.injector;

import net.fill1890.fabsit.entity.Pose;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.EnumSet;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Redirect(
            method = "updateInvisibilityStatus",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;setInvisible(Z)V"
            )
    )
    private void preventVisibilityUpdateWhenPosing(LivingEntity instance, boolean b) {
        if (instance instanceof Player player && EnumSet.of(Pose.LAYING, Pose.SPINNING).contains(player.fabSit$currentPose())) {
            instance.setInvisible(true);
        } else {
            instance.setInvisible(b);
        }
    }
}
