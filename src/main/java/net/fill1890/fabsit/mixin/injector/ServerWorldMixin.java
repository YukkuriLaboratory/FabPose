package net.fill1890.fabsit.mixin.injector;

import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.yukulab.fabpose.entity.define.PoseManagerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin {
    @Inject(
            method = "onBlockChanged",
            at = @At("RETURN")
    )
    private void checkPoseManagerEntity(BlockPos pos, BlockState oldBlock, BlockState newBlock, CallbackInfo ci) {
        if (!newBlock.isAir()) return;
        var world = (ServerWorld) (Object) this;
        var center = pos.toCenterPos();
        world.getEntitiesByClass(PoseManagerEntity.class, Box.of(center, 0.5, 0.5, 0.5), (e) -> true)
                .forEach(PoseManagerEntity::kill);
    }
}
