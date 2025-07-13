package net.yukulab.fabpose.network.packet.play

import java.util.UUID
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.network.packet.CustomPayload.Id
import net.yukulab.fabpose.extension.currentPose
import net.yukulab.fabpose.network.Networking

data class SyncRequestC2SPacket(val playerId: UUID) : CustomPayload {
    override fun getId(): Id<out CustomPayload> = ID

    @Environment(EnvType.CLIENT)
    fun send() {
        ClientPlayNetworking.send(this)
    }

    companion object {
        val ID: Id<SyncRequestC2SPacket> = Id(Networking.SYNC_REQUEST)
        val CODEC: PacketCodec<RegistryByteBuf, SyncRequestC2SPacket> = PacketCodec.of(
            { packet, buf ->
                buf.writeUuid(packet.playerId)
            },
            { SyncRequestC2SPacket(it.readUuid()) },
        )

        fun onReceive(payload: SyncRequestC2SPacket, context: ServerPlayNetworking.Context) {
            val targetId = payload.playerId
            val player = context.player()
            val pose = player.server?.playerManager?.getPlayer(targetId)?.currentPose
            SyncPoseS2CPacket(targetId, pose).send(player)
        }
    }
}
