package net.yukulab.fabsit

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import net.minecraft.server.MinecraftServer

lateinit var server: MinecraftServer
    internal set

val serverScope: CoroutineScope by lazy { CoroutineScope(server.asCoroutineDispatcher()) }
