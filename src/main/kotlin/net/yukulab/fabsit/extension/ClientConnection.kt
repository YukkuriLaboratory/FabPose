package net.yukulab.fabsit.extension

import net.fill1890.fabsit.extension.ModFlag
import net.fill1890.fabsit.mixin.accessor.ClientConnectionAccessor
import net.minecraft.network.ClientConnection

val ClientConnection.accessor: ClientConnectionAccessor
    get() = this as ClientConnectionAccessor

val ClientConnection.isModEnabled: Boolean
    get() = (this as ModFlag).`fabSit$isModEnabled`()
