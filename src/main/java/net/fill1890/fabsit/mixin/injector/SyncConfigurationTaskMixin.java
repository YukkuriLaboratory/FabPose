package net.fill1890.fabsit.mixin.injector;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.fabric.impl.registry.sync.RegistrySyncManager;
import net.fill1890.fabsit.mixin.accessor.ServerCommonPacketListenerImplAccessor;
import net.minecraft.network.protocol.Packet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.resources.Identifier;
import net.yukulab.fabpose.entity.FabSitEntities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Hijack registry sync manager to remove entites
 */
@Mixin(RegistrySyncManager.SyncConfigurationTask.class)
public abstract class SyncConfigurationTaskMixin {

    @Shadow(remap = false)
    public abstract ServerConfigurationPacketListenerImpl handler();

    @Shadow(remap = false)
    public abstract Map<Identifier, Object2IntMap<Identifier>> map();

    /**
     * Scrub registry if needed to remove custom fabsit entities for compatibility
     * <br>
     * Vanilla clients don't like having unknown entities, and clients with the Fabric API without fabsit will crash if
     * they receive unknown registry IDs
     * <br>
     * Injects just before sending the registry sync packet, checks if client has fabsit, and scrubs if not
     */
    @Inject(
            method = "start",
            at = @At("HEAD")
    )
    private void removeFromSync(Consumer<Packet<?>> sender, CallbackInfo ci) {
        // if client does not have fabsit
        var connection = ((ServerCommonPacketListenerImplAccessor) handler()).getConnection();
        if (!connection.fabSit$isModEnabled()) {

            var id = Registries.ENTITY_TYPE.identifier();
            // scrub entities from the syncing registry
            var entityTypeMap = map().get(id);
            entityTypeMap.removeInt(BuiltInRegistries.ENTITY_TYPE.getKey(FabSitEntities.POSE_MANAGER));
        }
    }

}
