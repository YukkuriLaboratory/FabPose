package net.fill1890.fabsit.mixin.injector.swim;

import net.fill1890.fabsit.extension.ForceSwimFlag;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(Entity.class)
abstract public class EntityMixin implements ForceSwimFlag {
    @Shadow
    public abstract void setSwimming(boolean swimming);

    @Shadow
    public abstract boolean isSprinting();

    @Shadow
    public abstract boolean isTouchingWater();

    @Shadow
    public abstract boolean hasVehicle();

    @Unique
    boolean fabsit$forceSwim;


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
        var generallyCanSwim = isSprinting() && isTouchingWater();
        setSwimming((fabsit$forceSwim || generallyCanSwim) && !hasVehicle());
        ci.cancel();
    }

    @Override
    public boolean fabSit$shouldForceSwim() {
        return fabsit$forceSwim;
    }

    @Override
    public void fabSit$setForceSwim(boolean forceSwim) {
        fabsit$forceSwim = forceSwim;
    }
}
