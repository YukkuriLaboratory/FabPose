package net.yukulab.fabsit.network.packet.play

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.fill1890.fabsit.entity.Pose
import net.minecraft.network.PacketByteBuf
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.yukulab.fabsit.extension.pose
import net.yukulab.fabsit.network.Networking

object PoseRequestC2SPacket {
    fun onReceive(
        server: MinecraftServer,
        player: ServerPlayerEntity,
        networkHandler: ServerPlayNetworkHandler,
        buf: PacketByteBuf,
        sender: PacketSender,
    ) {
        player.pose(buf.readEnumConstant(Pose::class.java))
    }

    @JvmStatic
    @Environment(EnvType.CLIENT)
    fun send(pose: Pose) {
        ClientPlayNetworking.send(Networking.POSE_REQUEST, PacketByteBufs.create().writeEnumConstant(pose))
    }
}
