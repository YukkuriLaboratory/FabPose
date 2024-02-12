package net.fill1890.fabsit.mixin.injector.swim;

import net.fill1890.fabsit.entity.Pose;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(Entity.class)
abstract public class EntityMixin {
    @Shadow
    public abstract void setSwimming(boolean swimming);

    @Shadow
    public abstract boolean isSprinting();

    @Shadow
    public abstract boolean isTouchingWater();

    @Shadow
    public abstract boolean hasVehicle();

    @Inject(
            method = "updateSwimming",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;setSwimming(Z)V",
                    ordinal = 0
            ),
            cancellable = true
    )
    private void shouldForceSwimmingInLand(CallbackInfo ci) {
        var entity = (Entity) (Object) this;
        if (entity instanceof ServerPlayerEntity player) {
            var pose = player.fabSit$currentPose();
            var generallyCanSwim = isSprinting() && isTouchingWater();
            setSwimming((pose == Pose.SWIMMING || generallyCanSwim) && !hasVehicle());
            ci.cancel();
        }
    }
}
