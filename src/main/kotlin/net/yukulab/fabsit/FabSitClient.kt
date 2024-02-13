package net.yukulab.fabsit

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry
import net.minecraft.client.render.entity.EmptyEntityRenderer
import net.yukulab.fabsit.entity.FabSitEntities

class FabSitClient : ClientModInitializer {
    override fun onInitializeClient() {
        EntityRendererRegistry.register(FabSitEntities.POSE_MANAGER, ::EmptyEntityRenderer)
    }
}
