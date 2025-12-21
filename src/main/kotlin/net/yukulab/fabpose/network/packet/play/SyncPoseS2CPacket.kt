package net.yukulab.fabpose.network.packet.play

import java.util.*
import kotlin.jvm.optionals.getOrNull
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fill1890.fabsit.entity.Pose
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.network.packet.CustomPayload.Id
import net.minecraft.server.network.ServerPlayerEntity
import net.yukulab.fabpose.extension.currentPose
import net.yukulab.fabpose.network.Networking

data class SyncPoseS2CPacket(val playerId: UUID, val pose: Pose?) : CustomPayload {
    override fun getId(): Id<out CustomPayload> = ID

    fun send(player: ServerPlayerEntity) {
        ServerPlayNetworking.send(player, this)
    }

    companion object {
        val ID: Id<SyncPoseS2CPacket> = Id(Networking.SYNC_POSE)
        val CODEC: PacketCodec<RegistryByteBuf, SyncPoseS2CPacket> = PacketCodec.of(
            { packet, buf ->
                buf.writeUuid(packet.playerId)
                buf.writeOptional(Optional.ofNullable(packet.pose)) { b, value ->
                    b.writeEnumConstant(value)
                }
            },
            { buf ->
                SyncPoseS2CPacket(buf.readUuid(), buf.readOptional { it.readEnumConstant(Pose::class.java) }.getOrNull())
            },
        )

        @Environment(EnvType.CLIENT)
        fun onReceive(payload: SyncPoseS2CPacket, context: ClientPlayNetworking.Context) {
            context.player().entityWorld?.getPlayerByUuid(payload.playerId)?.currentPose = payload.pose
        }
    }
}
