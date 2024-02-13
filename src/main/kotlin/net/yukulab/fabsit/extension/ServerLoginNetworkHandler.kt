package net.yukulab.fabsit.extension

import net.fill1890.fabsit.mixin.accessor.ServerLoginNetworkHandlerAccessor
import net.minecraft.server.network.ServerLoginNetworkHandler

val ServerLoginNetworkHandler.accessor: ServerLoginNetworkHandlerAccessor
    get() = this as ServerLoginNetworkHandlerAccessor
