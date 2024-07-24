package net.fill1890.fabsit.mixin.injector;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.fabric.impl.registry.sync.RegistrySyncManager;
import net.fabricmc.fabric.impl.registry.sync.packet.RegistryPacketHandler;
import net.fill1890.fabsit.FabSit;
import net.fill1890.fabsit.entity.ChairEntity;
import net.fill1890.fabsit.mixin.accessor.ServerPlayNetworkHandlerAccessor;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.yukulab.fabpose.entity.FabSitEntities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Map;

/**
 * Hijack registry sync manager to remove entites
 */
@SuppressWarnings("UnstableApiUsage")
@Mixin(RegistrySyncManager.class)
public abstract class RegistrySyncManagerMixin {

    /**
     * Scrub registry if needed to remove custom fabsit entities for compatibility
     * <br>
     * Vanilla clients don't like having unknown entities, and clients with the Fabric API without fabsit will crash if
     * they receive unknown registry IDs
     * <br>
     * Injects just before sending the registry sync packet, checks if client has fabsit, and scrubs if not
     */
    @Inject(
            method = "sendPacket(Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/fabricmc/fabric/impl/registry/sync/packet/RegistryPacketHandler;)V",
            at = @At(value = "INVOKE", target = "Lnet/fabricmc/fabric/impl/registry/sync/packet/RegistryPacketHandler;sendPacket(Lnet/minecraft/server/network/ServerPlayerEntity;Ljava/util/Map;)V"),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private static void removeFromSync(ServerPlayerEntity player, RegistryPacketHandler handler, CallbackInfo ci, Map<Identifier, Object2IntMap<Identifier>> map) {
        // if client does not have fabsit
        var connection = ((ServerPlayNetworkHandlerAccessor) player.networkHandler).getConnection();
        if (!connection.fabSit$isModEnabled()) {
            var id = RegistryKeys.ENTITY_TYPE.getValue();
            // scrub entities from the syncing registry
            map.get(id).removeInt(new Identifier(FabSit.MOD_ID, ChairEntity.ENTITY_ID));
            map.get(id).removeInt(Registries.ENTITY_TYPE.getId(FabSitEntities.POSE_MANAGER));
        }
    }

}
