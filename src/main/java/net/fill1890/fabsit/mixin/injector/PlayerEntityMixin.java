package net.fill1890.fabsit.mixin.injector;

import net.fill1890.fabsit.FabSit;
import net.fill1890.fabsit.entity.Pose;
import net.fill1890.fabsit.extension.PosingFlag;
import net.fill1890.fabsit.mixin.accessor.EntityAccessor;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.time.Instant;
import java.util.OptionalInt;

@Mixin(PlayerEntity.class)
abstract public class PlayerEntityMixin implements PosingFlag {
    @Unique
    private static TrackedData<OptionalInt> FABSIT_TRACKER_POSE;

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
                TrackedDataHandlerRegistry.OPTIONAL_INT
        );
    }

    @Inject(
            method = "initDataTracker",
            at = @At("RETURN")
    )
    private void initCustomTracker(DataTracker.Builder builder, CallbackInfo ci) {
        builder.add(FABSIT_TRACKER_POSE, OptionalInt.empty());
    }


    @Override
    public void fabSit$setPosing(@Nullable Pose posing) {
        var dataTracker = ((EntityAccessor) this).getDataTracker();
        var ordinal = posing != null ? OptionalInt.of(posing.ordinal()) : OptionalInt.empty();
        dataTracker.set(FABSIT_TRACKER_POSE, ordinal);
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
        var ordinal = dataTracker.get(FABSIT_TRACKER_POSE);
        if (ordinal.isPresent()) {
            return Pose.values()[ordinal.getAsInt()];
        } else {
            return null;
        }
    }

    @Override
    public @Nullable Instant fabSit$lastPoseTime() {
        return fabSit$lastPoseTime;
    }
}
