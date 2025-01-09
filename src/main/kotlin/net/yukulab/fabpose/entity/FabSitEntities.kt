package net.yukulab.fabpose.entity

import net.fabricmc.fabric.api.event.registry.RegistryAttribute
import net.fabricmc.fabric.api.event.registry.RegistryAttributeHolder
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricDefaultAttributeRegistry
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.SpawnGroup
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.util.Identifier
import net.yukulab.fabpose.MOD_ID
import net.yukulab.fabpose.entity.define.PoseManagerEntity

object FabSitEntities {
    @JvmField
    val POSE_MANAGER: EntityType<PoseManagerEntity> = register(
        "pose_manager",
        EntityType.Builder.create(::PoseManagerEntity, SpawnGroup.MISC)
            .dimensions(0.5f, 1.975f)
            .eyeHeight(1.975f * 0.85f),
    )

    fun register() {
        // Disable registry syncing and allows all clients to join the server
        RegistryAttributeHolder.get(RegistryKeys.ENTITY_TYPE).addAttribute(RegistryAttribute.OPTIONAL)
        FabricDefaultAttributeRegistry.register(POSE_MANAGER, ArmorStandEntity.createLivingAttributes())
    }

    private fun <T : Entity> register(id: String, entityType: EntityType.Builder<T>): EntityType<T> {
        val key = RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(MOD_ID, id))
        return Registry.register(Registries.ENTITY_TYPE, key, entityType.build(key))
    }
}
