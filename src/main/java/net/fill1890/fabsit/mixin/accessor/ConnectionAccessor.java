package net.fill1890.fabsit.mixin.accessor;

import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Connection.class)
public interface ConnectionAccessor {
    @Accessor("packetListener")
    void setPrivatePacketListener(PacketListener packetListener);
}
