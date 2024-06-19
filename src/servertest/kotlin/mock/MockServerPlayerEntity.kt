package mock

import com.mojang.authlib.GameProfile
import java.util.UUID
import kotlinx.atomicfu.atomic
import net.minecraft.network.NetworkSide
import net.minecraft.network.packet.c2s.common.SyncedClientOptions
import net.minecraft.server.network.ConnectedClientData
import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.test.TestContext
import net.minecraft.util.math.BlockPos
import net.minecraft.world.GameMode

private val playerId = atomic(0)

fun TestContext.createMockServerPlayer(relativePos: BlockPos = BlockPos(0, 1, 0)) =
    ServerPlayerEntity(
        world.server,
        world,
        GameProfile(UUID.randomUUID(), "test-mock-server-player-${playerId.getAndIncrement()}"),
        SyncedClientOptions.createDefault(),
    ).also {
        it.networkHandler = ServerPlayNetworkHandler(
            world.server,
            MockClientConnection(NetworkSide.SERVERBOUND),
            it,
            ConnectedClientData.createDefault(it.gameProfile, true),
        )
        it.refreshPositionAndAngles(getAbsolutePos(relativePos), 0f, 0f)
        it.changeGameMode(GameMode.CREATIVE)
        world.onPlayerConnected(it)
    }
