package net.yukulab.fabpose.network.packet.play

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fill1890.fabsit.entity.Pose
import net.fill1890.fabsit.error.PoseException
import net.fill1890.fabsit.util.Messages
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.network.packet.CustomPayload.Id
import net.yukulab.fabpose.extension.pose
import net.yukulab.fabpose.network.Networking

data class PoseRequestC2SPacket(val pose: Pose) : CustomPayload {
    companion object {
        val ID: Id<PoseRequestC2SPacket> = Id<PoseRequestC2SPacket>(Networking.POSE_REQUEST)
        val CODEC: PacketCodec<RegistryByteBuf, PoseRequestC2SPacket> = PacketCodec.of({ value, buf ->
            buf.writeEnumConstant(value.pose)
        }, { buf -> PoseRequestC2SPacket(buf.readEnumConstant(Pose::class.java)) })

        fun onReceive(
            payload: PoseRequestC2SPacket,
            context: ServerPlayNetworking.Context,
        ) {
            val player = context.player()
            player.pose(payload.pose).onFailure {
                if (it is PoseException.PermissionException) {
                    player.sendMessage(Messages.getStateError(player, payload.pose), false)
                }
            }
        }

        @JvmStatic
        @Environment(EnvType.CLIENT)
        fun send(pose: Pose) {
            ClientPlayNetworking.send(PoseRequestC2SPacket(pose))
        }
    }

    override fun getId(): Id<PoseRequestC2SPacket> {
        return ID
    }
}
