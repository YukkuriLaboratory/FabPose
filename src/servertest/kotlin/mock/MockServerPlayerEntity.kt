package mock

import com.mojang.authlib.GameProfile
import java.util.UUID
import kotlinx.atomicfu.atomic
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.server.level.ClientInformation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.CommonListenerCookie
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.world.level.GameType

private val playerId = atomic(0)

fun GameTestHelper.createMockServerPlayer(relativePos: BlockPos = BlockPos(0, 1, 0)) = ServerPlayer(
    level.server,
    level,
    GameProfile(UUID.randomUUID(), "test-mock-server-player-${playerId.getAndIncrement()}"),
    ClientInformation.createDefault(),
).also {
    it.connection = ServerGamePacketListenerImpl(
        level.server,
        MockClientConnection(PacketFlow.SERVERBOUND),
        it,
        CommonListenerCookie.createInitial(it.gameProfile, true),
    )
    it.snapTo(absolutePos(relativePos), 0f, 0f)
    it.setGameMode(GameType.CREATIVE)
    level.addNewPlayer(it)
}
