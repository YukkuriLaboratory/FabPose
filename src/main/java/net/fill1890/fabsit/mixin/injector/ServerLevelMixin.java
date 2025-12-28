package net.fill1890.fabsit.mixin.injector;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.yukulab.fabpose.entity.define.PoseManagerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(
            method = "updatePOIOnBlockStateChange",
            at = @At("RETURN")
    )
    private void checkPoseManagerEntity(BlockPos pos, BlockState oldBlock, BlockState newBlock, CallbackInfo ci) {
        if (!newBlock.isAir()) return;
        var world = (ServerLevel) (Object) this;
        var center = pos.getCenter();
        world.getEntitiesOfClass(PoseManagerEntity.class, AABB.ofSize(center, 0.5, 0.5, 0.5), (e) -> true)
                .forEach(e -> e.kill(world));
    }
}
