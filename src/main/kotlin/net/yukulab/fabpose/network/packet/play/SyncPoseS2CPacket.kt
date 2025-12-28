package net.yukulab.fabpose.network.packet.play

import java.util.*
import kotlin.jvm.optionals.getOrNull
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fill1890.fabsit.entity.Pose
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type
import net.minecraft.server.level.ServerPlayer
import net.yukulab.fabpose.extension.currentPose
import net.yukulab.fabpose.network.Networking

data class SyncPoseS2CPacket(val playerId: UUID, val pose: Pose?) : CustomPacketPayload {
    override fun type(): Type<out CustomPacketPayload> = ID

    fun send(player: ServerPlayer) {
        ServerPlayNetworking.send(player, this)
    }

    companion object {
        val ID: Type<SyncPoseS2CPacket> = Type(Networking.SYNC_POSE)
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, SyncPoseS2CPacket> = StreamCodec.ofMember(
            { packet, buf ->
                buf.writeUUID(packet.playerId)
                buf.writeOptional(Optional.ofNullable(packet.pose)) { b, value ->
                    b.writeEnum(value)
                }
            },
            { buf ->
                SyncPoseS2CPacket(buf.readUUID(), buf.readOptional { it.readEnum(Pose::class.java) }.getOrNull())
            },
        )

        @Environment(EnvType.CLIENT)
        fun onReceive(payload: SyncPoseS2CPacket, context: ClientPlayNetworking.Context) {
            context.player().level().getPlayerByUUID(payload.playerId)?.currentPose = payload.pose
        }
    }
}
