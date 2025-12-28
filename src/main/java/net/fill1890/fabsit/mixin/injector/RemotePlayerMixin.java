package net.fill1890.fabsit.mixin.injector;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.yukulab.fabpose.network.packet.play.SyncRequestC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RemotePlayer.class)
abstract public class RemotePlayerMixin {
    @Inject(
            method = "<init>",
            at = @At("TAIL")
    )
    private void requestPlayerCurrentPose(ClientLevel clientWorld, GameProfile gameProfile, CallbackInfo ci) {
        new SyncRequestC2SPacket(gameProfile.id()).send();
    }
}
