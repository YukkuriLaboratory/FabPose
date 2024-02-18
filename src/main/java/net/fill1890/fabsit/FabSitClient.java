package net.fill1890.fabsit;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fill1890.fabsit.keybind.PoseKeybinds;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;

public class FabSitClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // renderer is required for all registered entities, but we don't want to render them
        // so pass in an empty renderer
        EntityRendererRegistry.register(FabSit.CHAIR_ENTITY_TYPE, EmptyRenderer::new);

        // keybinds for posing
        PoseKeybinds.register();
    }

    // empty renderer - will never render as shouldRender is always false
    private static class EmptyRenderer extends EntityRenderer<Entity> {
        protected EmptyRenderer(EntityRendererFactory.Context ctx) { super(ctx); }

        @Override
        public boolean shouldRender(Entity entity, Frustum frustum, double x, double y, double z) { return false; }

        @Override
        public Identifier getTexture(Entity entity) { return null; }
    }
}
