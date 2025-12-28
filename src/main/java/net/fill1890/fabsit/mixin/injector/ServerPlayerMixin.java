package net.fill1890.fabsit.mixin.injector;

import net.minecraft.server.level.ServerPlayer;
import net.yukulab.fabpose.entity.define.PoseManagerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hijack server players to remove from fabsit configs
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    /**
     * Remove player seats from the world when they disconnect
     *
     * @param ci mixin callback info
     */
    @Inject(at = @At("HEAD"), method = "disconnect")
    private void removeSeat(CallbackInfo ci) {
        // cast this to a usable object
        ServerPlayer self = (ServerPlayer) (Object) this;

        // if player is sitting on a fabsit chair, kick them off
        if(self.isPassenger() && self.getVehicle() instanceof PoseManagerEntity chair) {
            self.stopRiding();
            chair.kill(self.level());
        }
    }
}
