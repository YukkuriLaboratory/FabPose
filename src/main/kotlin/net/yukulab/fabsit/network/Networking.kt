package net.yukulab.fabsit.network

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.util.Identifier
import net.yukulab.fabsit.MOD_ID
import net.yukulab.fabsit.network.packet.HandShakeS2CPacket
import net.yukulab.fabsit.network.packet.play.PoseRequestC2SPacket

object Networking {
    val HANDSHAKE: Identifier = id("handshake")
    val POSE_REQUEST: Identifier = id("poserequest")

    fun registerServerHandlers() {
        ServerLoginConnectionEvents.QUERY_START.register(HandShakeS2CPacket::sendQuery)
        ServerLoginNetworking.registerGlobalReceiver(HANDSHAKE, HandShakeS2CPacket::onHandShakeServer)
        ServerPlayNetworking.registerGlobalReceiver(POSE_REQUEST, PoseRequestC2SPacket::onReceive)
    }

    @Environment(EnvType.CLIENT)
    fun registerClientHandlers() {
        ClientLoginNetworking.registerGlobalReceiver(HANDSHAKE, HandShakeS2CPacket::onHandShakeClient)
    }

    private fun id(name: String): Identifier = Identifier(MOD_ID, name)
}
