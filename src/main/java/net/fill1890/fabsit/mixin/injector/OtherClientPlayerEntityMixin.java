package net.fill1890.fabsit.mixin.injector;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.yukulab.fabpose.network.packet.play.SyncRequestC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OtherClientPlayerEntity.class)
abstract public class OtherClientPlayerEntityMixin {
    @Inject(
            method = "<init>",
            at = @At("TAIL")
    )
    private void requestPlayerCurrentPose(ClientWorld clientWorld, GameProfile gameProfile, CallbackInfo ci) {
        new SyncRequestC2SPacket(gameProfile.getId()).send();
    }
}
