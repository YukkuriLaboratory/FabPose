package net.fill1890.fabsit.mixin.injector;

import net.fill1890.fabsit.entity.Pose;
import net.fill1890.fabsit.extension.PosingFlag;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PlayerEntityRenderState.class)
public abstract class MixinPlayerEntityRenderState implements PosingFlag {
    @Unique
    private Pose fabSit$currentPose;

    @Override
    public Pose fabSit$currentPose() {
        return this.fabSit$currentPose;
    }

    @Override
    public void fabSit$setPosing(Pose pose) {
        this.fabSit$currentPose = pose;
    }
}
