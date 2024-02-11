package net.yukulab.fabsit.extension

import net.fill1890.fabsit.mixin.accessor.ClientConnectionAccessor
import net.minecraft.network.ClientConnection

val ClientConnection.acccessor: ClientConnectionAccessor
    get() = this as ClientConnectionAccessor
