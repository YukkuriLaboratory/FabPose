package net.fill1890.fabsit.mixin.injector;

import net.fill1890.fabsit.entity.Pose;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.EnumSet;

@Mixin(PlayerItemInHandLayer.class)
abstract public class PlayerItemInHandLayerMixin<S extends AvatarRenderState> {
    @Inject(
            method = "submitArmWithItem(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void preventInvisiblePlayerItems(S playerEntityRenderState, ItemStackRenderState itemRenderState, ItemStack itemStack, HumanoidArm arm, PoseStack matrixStack, SubmitNodeCollector orderedRenderCommandQueue, int i, CallbackInfo ci) {
        if (EnumSet.of(Pose.LAYING, Pose.SPINNING).contains(playerEntityRenderState.fabSit$currentPose())) {
            ci.cancel();
        }
    }
}
