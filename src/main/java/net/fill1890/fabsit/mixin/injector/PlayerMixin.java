package net.fill1890.fabsit.mixin.injector;

import net.fill1890.fabsit.entity.Pose;
import net.fill1890.fabsit.extension.PosingFlag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.yukulab.fabpose.network.packet.play.SyncPoseS2CPacket;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.time.Instant;

@Mixin(Player.class)
abstract public class PlayerMixin implements PosingFlag {
    @Unique
    Pose fabsit$pose;

    @Unique
    Instant fabSit$lastPoseTime;

    @SuppressWarnings("UnreachableCode")
    @Inject(
            method = "updatePlayerPose",
            at = @At("HEAD"),
            cancellable = true
    )
    private void checkForceSwim(CallbackInfo ci) {
        var player = ((Player) (Object) this);
        if (fabSit$currentPose() == Pose.SWIMMING) {
            player.setPose(net.minecraft.world.entity.Pose.SWIMMING);
            ci.cancel();
        }
    }

    @SuppressWarnings("UnreachableCode")
    @Override
    public void fabSit$setPosing(@Nullable Pose posing) {
        fabsit$pose = posing;
        var player = (Player) (Object) this;
        if (posing != null) {
            fabSit$lastPoseTime = Instant.now();
        } else {
            player.stopRiding();
        }
        var server = player.level().getServer();
        if (server != null) {
            var currentWorldKey = player.level().dimension();
            var packet = new SyncPoseS2CPacket(player.getUUID(), posing);
            for (ServerPlayer targetPlayer : server.getPlayerList().getPlayers()) {
                if (targetPlayer.level().dimension() == currentWorldKey) {
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
