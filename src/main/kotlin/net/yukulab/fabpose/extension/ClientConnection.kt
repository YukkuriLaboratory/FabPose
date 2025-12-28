package net.yukulab.fabpose.extension

import net.fill1890.fabsit.extension.ModFlag
import net.fill1890.fabsit.mixin.accessor.ConnectionAccessor
import net.minecraft.network.Connection

val Connection.accessor: ConnectionAccessor
    get() = this as ConnectionAccessor

val Connection.isModEnabled: Boolean
    get() = (this as ModFlag).`fabSit$isModEnabled`()
