package net.fill1890.fabsit.mixin.accessor;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientConnection.class)
public interface ClientConnectionAccessor {
    @Accessor("packetListener")
    void setPrivatePacketListener(PacketListener packetListener);
}
