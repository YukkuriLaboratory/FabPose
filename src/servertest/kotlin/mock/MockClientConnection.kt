package mock

import net.minecraft.network.ClientConnection
import net.minecraft.network.NetworkSide
import net.minecraft.network.listener.PacketListener
import net.yukulab.fabpose.extension.accessor

class MockClientConnection(side: NetworkSide?) : ClientConnection(side) {
    override fun setPacketListener(packetListener: PacketListener?) {
        accessor.setPrivatePacketListener(packetListener)
    }
}
