package net.yukulab.fabsit.entity

import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricDefaultAttributeRegistry
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricEntityTypeBuilder
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityDimensions
import net.minecraft.entity.EntityType
import net.minecraft.entity.SpawnGroup
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier
import net.yukulab.fabsit.MOD_ID
import net.yukulab.fabsit.entity.define.PoseManagerEntity

object FabSitEntities {
    @JvmField
    val POSE_MANAGER: EntityType<PoseManagerEntity> = register(
        "pose_manager",
        FabricEntityTypeBuilder.create(SpawnGroup.MISC, ::PoseManagerEntity)
            .dimensions(EntityDimensions(0.5f, 1.975f, true))
            .build(),
    )

    fun register() {
        FabricDefaultAttributeRegistry.register(POSE_MANAGER, ArmorStandEntity.createLivingAttributes())
    }

    private fun <T : Entity> register(id: String, entityType: EntityType<T>): EntityType<T> =
        Registry.register(Registries.ENTITY_TYPE, Identifier.of(MOD_ID, id), entityType)
}
