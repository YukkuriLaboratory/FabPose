package net.yukulab.fabpose

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import net.minecraft.server.MinecraftServer

const val MOD_ID = "fabpose"

lateinit var server: MinecraftServer
    internal set

val serverDispatcher: CoroutineDispatcher by lazy { server.asCoroutineDispatcher() }

val serverScope: CoroutineScope by lazy { CoroutineScope(serverDispatcher) }

lateinit var coroutineScope: CoroutineScope
    internal set
