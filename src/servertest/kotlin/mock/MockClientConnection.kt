package mock

import net.minecraft.network.ClientConnection
import net.minecraft.network.NetworkSide

class MockClientConnection(side: NetworkSide?) : ClientConnection(side)
