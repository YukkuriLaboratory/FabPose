package net.yukulab.fabpose.network

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.resources.Identifier
import net.yukulab.fabpose.MOD_ID
import net.yukulab.fabpose.network.packet.HandShakeS2CPacket
import net.yukulab.fabpose.network.packet.play.PoseRequestC2SPacket
import net.yukulab.fabpose.network.packet.play.SyncPoseS2CPacket
import net.yukulab.fabpose.network.packet.play.SyncRequestC2SPacket

object Networking {
    val HANDSHAKE: Identifier = id("handshake")
    val POSE_REQUEST: Identifier = id("poserequest")
    val SYNC_REQUEST: Identifier = id("syncrequest")
    val SYNC_POSE: Identifier = id("syncpose")

    fun registerServerHandlers() {
        PayloadTypeRegistry.playC2S().register(PoseRequestC2SPacket.ID, PoseRequestC2SPacket.CODEC)
        PayloadTypeRegistry.playC2S().register(SyncRequestC2SPacket.ID, SyncRequestC2SPacket.CODEC)
        PayloadTypeRegistry.playS2C().register(SyncPoseS2CPacket.ID, SyncPoseS2CPacket.CODEC)

        ServerLoginConnectionEvents.QUERY_START.register(HandShakeS2CPacket::sendQuery)
        ServerLoginNetworking.registerGlobalReceiver(HANDSHAKE, HandShakeS2CPacket::onHandShakeServer)
        ServerPlayNetworking.registerGlobalReceiver(SyncRequestC2SPacket.ID, SyncRequestC2SPacket::onReceive)
        ServerPlayNetworking.registerGlobalReceiver(PoseRequestC2SPacket.ID, PoseRequestC2SPacket::onReceive)
    }

    @Environment(EnvType.CLIENT)
    fun registerClientHandlers() {
        ClientLoginNetworking.registerGlobalReceiver(HANDSHAKE, HandShakeS2CPacket::onHandShakeClient)
        ClientPlayNetworking.registerGlobalReceiver(SyncPoseS2CPacket.ID, SyncPoseS2CPacket::onReceive)
    }

    private fun id(name: String): Identifier = Identifier.fromNamespaceAndPath(MOD_ID, name)
}
