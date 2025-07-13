package net.fill1890.fabsit.mixin.injector;

import net.minecraft.server.network.ServerPlayerEntity;
import net.yukulab.fabpose.entity.define.PoseManagerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hijack server players to remove from fabsit configs
 */
@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {

    /**
     * Remove player seats from the world when they disconnect
     *
     * @param ci mixin callback info
     */
    @Inject(at = @At("HEAD"), method = "onDisconnect")
    private void removeSeat(CallbackInfo ci) {
        // cast this to a usable object
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;

        // if player is sitting on a fabsit chair, kick them off
        if(self.hasVehicle() && self.getVehicle() instanceof PoseManagerEntity chair) {
            self.stopRiding();
            chair.kill(self.getWorld());
        }
    }
}
