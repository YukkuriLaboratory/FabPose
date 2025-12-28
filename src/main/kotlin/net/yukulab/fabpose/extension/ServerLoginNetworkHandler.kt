package net.yukulab.fabpose.extension

import net.fill1890.fabsit.mixin.accessor.ServerLoginPacketListenerImplAccessor
import net.minecraft.server.network.ServerLoginPacketListenerImpl

val ServerLoginPacketListenerImpl.accessor: ServerLoginPacketListenerImplAccessor
    get() = this as ServerLoginPacketListenerImplAccessor
