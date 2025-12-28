package net.yukulab.fabpose.network.packet.play

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fill1890.fabsit.entity.Pose
import net.fill1890.fabsit.error.PoseException
import net.fill1890.fabsit.util.Messages
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type
import net.yukulab.fabpose.extension.pose
import net.yukulab.fabpose.network.Networking

data class PoseRequestC2SPacket(val pose: Pose) : CustomPacketPayload {
    companion object {
        val ID: Type<PoseRequestC2SPacket> = Type<PoseRequestC2SPacket>(Networking.POSE_REQUEST)
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, PoseRequestC2SPacket> = StreamCodec.ofMember({ value, buf ->
            buf.writeEnum(value.pose)
        }, { buf -> PoseRequestC2SPacket(buf.readEnum(Pose::class.java)) })

        fun onReceive(
            payload: PoseRequestC2SPacket,
            context: ServerPlayNetworking.Context,
        ) {
            val player = context.player()
            player.pose(payload.pose).onFailure {
                if (it is PoseException.PermissionException) {
                    player.displayClientMessage(Messages.getStateError(player, payload.pose), false)
                }
            }
        }

        @JvmStatic
        @Environment(EnvType.CLIENT)
        fun send(pose: Pose) {
            ClientPlayNetworking.send(PoseRequestC2SPacket(pose))
        }
    }

    override fun type(): Type<PoseRequestC2SPacket> = ID
}
