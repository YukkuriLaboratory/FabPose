package net.fill1890.fabsit.mixin.injector.swim;

import net.fill1890.fabsit.entity.Pose;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
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
    public abstract boolean isInWater();

    @Shadow
    public abstract boolean isPassenger();

    @Inject(
            method = "updateSwimming",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;setSwimming(Z)V",
                    ordinal = 0
            ),
            cancellable = true
    )
    private void shouldForceSwimmingInLand(CallbackInfo ci) {
        var entity = (Entity) (Object) this;
        if (entity instanceof ServerPlayer player) {
            var pose = player.fabSit$currentPose();
            var generallyCanSwim = isSprinting() && isInWater();
            setSwimming((pose == Pose.SWIMMING || generallyCanSwim) && !isPassenger());
            ci.cancel();
        }
    }
}
