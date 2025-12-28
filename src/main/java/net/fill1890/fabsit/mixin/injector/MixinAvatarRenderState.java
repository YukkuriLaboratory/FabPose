package net.fill1890.fabsit.mixin.injector;

import net.fill1890.fabsit.entity.Pose;
import net.fill1890.fabsit.extension.PosingFlag;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
public abstract class MixinAvatarRenderState implements PosingFlag {
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
