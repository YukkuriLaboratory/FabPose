package net.yukulab.fabsit

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import net.minecraft.server.MinecraftServer

const val MOD_ID = "fabsit"

lateinit var server: MinecraftServer
    internal set

val serverScope: CoroutineScope by lazy { CoroutineScope(server.asCoroutineDispatcher()) }
