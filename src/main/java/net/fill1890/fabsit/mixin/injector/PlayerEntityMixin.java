package net.fill1890.fabsit.mixin.injector;

import net.fill1890.fabsit.entity.Pose;
import net.fill1890.fabsit.extension.PosingFlag;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.yukulab.fabpose.network.packet.play.SyncPoseS2CPacket;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.time.Instant;

@Mixin(PlayerEntity.class)
abstract public class PlayerEntityMixin implements PosingFlag {
    @Unique
    Pose fabsit$pose;

    @Unique
    Instant fabSit$lastPoseTime;

    @SuppressWarnings("UnreachableCode")
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

    @SuppressWarnings("UnreachableCode")
    @Override
    public void fabSit$setPosing(@Nullable Pose posing) {
        fabsit$pose = posing;
        var player = (PlayerEntity) (Object) this;
        if (posing != null) {
            fabSit$lastPoseTime = Instant.now();
        } else {
            player.stopRiding();
        }
        var server = player.getServer();
        if (server != null) {
            var currentWorldKey = player.getWorld().getRegistryKey();
            var packet = new SyncPoseS2CPacket(player.getUuid(), posing);
            for (ServerPlayerEntity targetPlayer : server.getPlayerManager().getPlayerList()) {
                if (targetPlayer.getWorld().getRegistryKey() == currentWorldKey) {
                    packet.send(targetPlayer);
                }
            }
        }
    }

    @Override
    public @Nullable Pose fabSit$currentPose() {
        return fabsit$pose;
    }

    @Override
    public @Nullable Instant fabSit$lastPoseTime() {
        return fabSit$lastPoseTime;
    }
}
