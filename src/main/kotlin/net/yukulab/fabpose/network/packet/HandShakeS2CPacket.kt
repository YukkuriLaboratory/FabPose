package net.yukulab.fabpose.network.packet

import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.networking.v1.LoginPacketSender
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking
import net.fill1890.fabsit.extension.ModFlag
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientLoginNetworkHandler
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.PacketCallbacks
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerLoginNetworkHandler
import net.yukulab.fabpose.extension.accessor
import net.yukulab.fabpose.network.Networking

object HandShakeS2CPacket {
    fun sendQuery(
        handler: ServerLoginNetworkHandler,
        server: MinecraftServer,
        sender: LoginPacketSender,
        synchronizer: ServerLoginNetworking.LoginSynchronizer,
    ) {
        sender.sendPacket(Networking.HANDSHAKE, PacketByteBufs.empty())
    }

    fun onHandShakeServer(
        server: MinecraftServer,
        handler: ServerLoginNetworkHandler,
        understood: Boolean,
        buf: PacketByteBuf,
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
        client: MinecraftClient,
        clientLoginNetworkHandler: ClientLoginNetworkHandler,
        bufs: PacketByteBuf,
        callbacksConsumer: Consumer<PacketCallbacks>,
    ): CompletableFuture<PacketByteBuf?> {
        return CompletableFuture.completedFuture(PacketByteBufs.empty())
    }
}
