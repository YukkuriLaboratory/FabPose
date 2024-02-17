package net.yukulab.fabpose

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry
import net.minecraft.client.render.entity.EmptyEntityRenderer
import net.yukulab.fabpose.entity.FabSitEntities
import net.yukulab.fabpose.network.Networking

class FabPoseClient : ClientModInitializer {
    override fun onInitializeClient() {
        EntityRendererRegistry.register(FabSitEntities.POSE_MANAGER, ::EmptyEntityRenderer)
        Networking.registerClientHandlers()
    }
}
