package net.fill1890.fabsit.mixin.injector;

import net.fill1890.fabsit.entity.Pose;
import net.fill1890.fabsit.extension.PosingFlag;
import net.fill1890.fabsit.mixin.accessor.EntityAccessor;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.player.PlayerEntity;
import net.yukulab.fabsit.entity.data.TrackedDataHandlers;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.time.Instant;
import java.util.Optional;

@Mixin(PlayerEntity.class)
abstract public class PlayerEntityMixin implements PosingFlag {
    @Unique
    private static TrackedData<Optional<Pose>> FABSIT_TRACKER_POSE;

    @Unique
    Instant fabSit$lastPoseTime;

    @Inject(
            method = "updatePose",
            at = @At("HEAD"),
            cancellable = true
    )
    private void checkForceSwim(CallbackInfo ci) {
        var player = ((PlayerEntity) (Object) this);
        if (fabSit$currentPose() == Pose.SWIMMING) {
            player.setPose(EntityPose.SWIMMING);
            ci.cancel();
        }
    }

    @Inject(
            method = "<clinit>",
            at = @At("TAIL")
    )
    private static void initDataTracker(CallbackInfo ci) {
        FABSIT_TRACKER_POSE = DataTracker.registerData(
                PlayerEntity.class,
                TrackedDataHandlers.POSE_HANDLER
        );
    }

    @Inject(
            method = "initDataTracker",
            at = @At("RETURN")
    )
    private void initCustomTracker(CallbackInfo ci) {
        var dataTracker = ((EntityAccessor) this).getDataTracker();
        dataTracker.startTracking(FABSIT_TRACKER_POSE, Optional.empty());
    }


    @Override
    public void fabSit$setPosing(@Nullable Pose posing) {
        var dataTracker = ((EntityAccessor) this).getDataTracker();
        dataTracker.set(FABSIT_TRACKER_POSE, Optional.ofNullable(posing));
        if (posing != null) {
            fabSit$lastPoseTime = Instant.now();
        } else {
            var player = (PlayerEntity) (Object) this;
            player.stopRiding();
        }
    }

    @Override
    public @Nullable Pose fabSit$currentPose() {
        var dataTracker = ((EntityAccessor) this).getDataTracker();
        return dataTracker.get(FABSIT_TRACKER_POSE).orElse(null);
    }

    @Override
    public @Nullable Instant fabSit$lastPoseTime() {
        return fabSit$lastPoseTime;
    }
}
