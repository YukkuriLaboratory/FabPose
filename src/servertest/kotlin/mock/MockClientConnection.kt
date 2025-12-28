package mock

import net.minecraft.network.Connection
import net.minecraft.network.protocol.PacketFlow

class MockClientConnection(side: PacketFlow) : Connection(side)
