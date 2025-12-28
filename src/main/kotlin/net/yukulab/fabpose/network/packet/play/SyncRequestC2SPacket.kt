package net.yukulab.fabpose.network.packet.play

import java.util.UUID
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type
import net.yukulab.fabpose.extension.currentPose
import net.yukulab.fabpose.network.Networking

data class SyncRequestC2SPacket(val playerId: UUID) : CustomPacketPayload {
    override fun type(): Type<out CustomPacketPayload> = ID

    @Environment(EnvType.CLIENT)
    fun send() {
        ClientPlayNetworking.send(this)
    }

    companion object {
        val ID: Type<SyncRequestC2SPacket> = Type(Networking.SYNC_REQUEST)
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, SyncRequestC2SPacket> = StreamCodec.ofMember(
            { packet, buf ->
                buf.writeUUID(packet.playerId)
            },
            { SyncRequestC2SPacket(it.readUUID()) },
        )

        fun onReceive(payload: SyncRequestC2SPacket, context: ServerPlayNetworking.Context) {
            val targetId = payload.playerId
            val player = context.player()
            val pose = player.level().server?.playerList?.getPlayer(targetId)?.currentPose
            SyncPoseS2CPacket(targetId, pose).send(player)
        }
    }
}
