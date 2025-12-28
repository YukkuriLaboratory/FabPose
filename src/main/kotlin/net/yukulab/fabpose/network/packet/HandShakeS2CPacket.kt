package net.yukulab.fabpose.network.packet

import io.netty.channel.ChannelFutureListener
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.networking.v1.LoginPacketSender
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking
import net.fill1890.fabsit.extension.ModFlag
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerLoginPacketListenerImpl
import net.yukulab.fabpose.extension.accessor
import net.yukulab.fabpose.network.Networking

object HandShakeS2CPacket {
    fun sendQuery(
        handler: ServerLoginPacketListenerImpl,
        server: MinecraftServer,
        sender: LoginPacketSender,
        synchronizer: ServerLoginNetworking.LoginSynchronizer,
    ) {
        sender.sendPacket(Networking.HANDSHAKE, PacketByteBufs.empty())
    }

    fun onHandShakeServer(
        server: MinecraftServer,
        handler: ServerLoginPacketListenerImpl,
        understood: Boolean,
        buf: FriendlyByteBuf,
        synchronizer: ServerLoginNetworking.LoginSynchronizer,
        responseSender: PacketSender,
    ) {
        if (understood) {
            val connection = handler.accessor.connection
            (connection as ModFlag).`fabSit$onModEnabled`()
        }
    }

    @Environment(EnvType.CLIENT)
    fun onHandShakeClient(
        client: Minecraft,
        clientLoginNetworkHandler: ClientHandshakePacketListenerImpl,
        buf: FriendlyByteBuf,
        callbacksConsumer: Consumer<ChannelFutureListener>,
    ): CompletableFuture<FriendlyByteBuf?> = CompletableFuture.completedFuture(PacketByteBufs.empty())
}
