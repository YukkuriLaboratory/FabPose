package net.fill1890.fabsit.mixin.injector;

import net.fill1890.fabsit.entity.Pose;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.PlayerHeldItemFeatureRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.EnumSet;

@Mixin(PlayerHeldItemFeatureRenderer.class)
abstract public class PlayerHeldItemFeatureRendererMixin<S extends PlayerEntityRenderState> {
    @Inject(
            method = "renderItem(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/Arm;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void preventInvisiblePlayerItems(S playerEntityRenderState, ItemRenderState itemRenderState, ItemStack itemStack, Arm arm, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, int i, CallbackInfo ci) {
        if (EnumSet.of(Pose.LAYING, Pose.SPINNING).contains(playerEntityRenderState.fabSit$currentPose())) {
            ci.cancel();
        }
    }
}
