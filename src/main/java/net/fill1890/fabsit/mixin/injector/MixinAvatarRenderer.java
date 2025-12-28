package net.fill1890.fabsit.mixin.injector;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class MixinAvatarRenderer<AvatarlikeEntity extends Avatar> {
    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("RETURN")
    )
    private void updatePoseState(AvatarlikeEntity playerLikeEntity, AvatarRenderState playerEntityRenderState, float f, CallbackInfo ci) {
        // Only apply pose state for actual PlayerEntity instances (not MannequinEntity)
        if (playerLikeEntity instanceof Player player) {
            playerEntityRenderState.fabSit$setPosing(player.fabSit$currentPose());
        }
    }
}
