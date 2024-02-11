package mock

import net.minecraft.network.ClientConnection
import net.minecraft.network.NetworkSide
import net.minecraft.network.listener.PacketListener
import net.turtton.fabsit.extension.acccessor

class MockClientConnection(side: NetworkSide?) : ClientConnection(side) {
    override fun setPacketListener(packetListener: PacketListener?) {
        acccessor.setPrivatePacketListener(packetListener)
    }
}
