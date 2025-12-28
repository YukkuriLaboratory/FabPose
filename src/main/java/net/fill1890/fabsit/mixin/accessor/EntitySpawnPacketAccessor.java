package net.fill1890.fabsit.mixin.accessor;

import net.minecraft.world.entity.EntityType;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Updater for fields of EntitySpawnS2CPacket
 */
@Mixin(ClientboundAddEntityPacket.class)
public interface EntitySpawnPacketAccessor {
    /**
     * Modify type ID of entity
     * <p>
     * Used to fake out pose manager packets for cross-compatibility between vanilla and fabsit clients
     *
     * @param type new entity type
     */
    @Mutable
    @Accessor("type")
    void setEntityTypeId(EntityType<?> type);

    /**
     * Modify y-position of entity
     * <p>
     * Used to adjust y-positions of entities for consistency between vanilla and fabsit clients
     *
     * @param y new position
     */
    @Mutable
    @Accessor("y")
    void setY(double y);
}
