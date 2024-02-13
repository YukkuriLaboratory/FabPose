package net.fill1890.fabsit.mixin.injector;

import net.fill1890.fabsit.entity.Pose;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.PlayerHeldItemFeatureRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.EnumSet;

@Mixin(PlayerHeldItemFeatureRenderer.class)
abstract public class PlayerHeldItemFeatureRendererMixin {
    @Inject(
            method = "renderItem",
            at = @At("HEAD"),
            cancellable = true
    )
    private void preventInvisiblePlayerItems(LivingEntity entity, ItemStack stack, ModelTransformationMode transformationMode, Arm arm, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (entity instanceof PlayerEntity player && EnumSet.of(Pose.LAYING, Pose.SPINNING).contains(player.fabSit$currentPose())) {
            ci.cancel();
        }
    }
}
