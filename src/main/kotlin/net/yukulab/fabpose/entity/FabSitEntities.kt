package net.yukulab.fabpose.entity

import net.fabricmc.fabric.api.event.registry.RegistryAttribute
import net.fabricmc.fabric.api.event.registry.RegistryAttributeHolder
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricDefaultAttributeRegistry
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.entity.decoration.ArmorStand
import net.yukulab.fabpose.MOD_ID
import net.yukulab.fabpose.entity.define.PoseManagerEntity

object FabSitEntities {
    @JvmField
    val POSE_MANAGER: EntityType<PoseManagerEntity> = register(
        "pose_manager",
        EntityType.Builder.of(::PoseManagerEntity, MobCategory.MISC)
            .sized(0.5f, 1.975f)
            .eyeHeight(1.975f * 0.85f),
    )

    fun register() {
        // Disable registry syncing and allows all clients to join the server
        RegistryAttributeHolder.get(Registries.ENTITY_TYPE).addAttribute(RegistryAttribute.OPTIONAL)
        FabricDefaultAttributeRegistry.register(POSE_MANAGER, ArmorStand.createLivingAttributes())
    }

    private fun <T : Entity> register(id: String, entityType: EntityType.Builder<T>): EntityType<T> {
        val key = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, id))
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, entityType.build(key))
    }
}
