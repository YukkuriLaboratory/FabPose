package net.fill1890.fabsit.mixin.injector.swim;

import net.fill1890.fabsit.extension.ForceSwimFlag;
import net.fill1890.fabsit.mixin.accessor.EntityAccessor;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
abstract public class PlayerEntityMixin implements ForceSwimFlag {
    @Unique
    private static TrackedData<Boolean> FABSIT_TRACKER_FORCE_SWIM;

    @Inject(
            method = "updatePose",
            at = @At("HEAD"),
            cancellable = true
    )
    private void checkForceSwim(CallbackInfo ci) {
        var player = ((PlayerEntity) (Object) this);
        if (fabSit$shouldForceSwim()) {
            player.setPose(EntityPose.SWIMMING);
            ci.cancel();
        }
    }

    @Inject(
            method = "<clinit>",
            at = @At("TAIL")
    )
    private static void initDataTracker(CallbackInfo ci) {
        FABSIT_TRACKER_FORCE_SWIM = DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    }

    @Inject(
            method = "initDataTracker",
            at = @At("RETURN")
    )
    private void initCustomTracker(CallbackInfo ci) {
        var dataTracker = ((EntityAccessor) this).getDataTracker();
        dataTracker.startTracking(FABSIT_TRACKER_FORCE_SWIM, false);
    }

    @Override
    public void fabSit$setForceSwim(boolean shouldSwim) {
        var dataTracker = ((EntityAccessor) this).getDataTracker();
        dataTracker.set(FABSIT_TRACKER_FORCE_SWIM, shouldSwim);
    }

    @Override
    public boolean fabSit$shouldForceSwim() {
        var dataTracker = ((EntityAccessor) this).getDataTracker();
        return dataTracker.get(FABSIT_TRACKER_FORCE_SWIM);
    }
}
