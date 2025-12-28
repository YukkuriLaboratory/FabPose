package net.yukulab.fabpose

import net.fabricmc.api.ClientModInitializer
import net.minecraft.client.renderer.entity.EntityRenderers
import net.minecraft.client.renderer.entity.NoopRenderer
import net.yukulab.fabpose.entity.FabSitEntities
import net.yukulab.fabpose.network.Networking

class FabPoseClient : ClientModInitializer {
    override fun onInitializeClient() {
        EntityRenderers.register(FabSitEntities.POSE_MANAGER, ::NoopRenderer)
        Networking.registerClientHandlers()
    }
}
