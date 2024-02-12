package runner

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import net.minecraft.client.MinecraftClient

val clientDispatcher = MinecraftClient.getInstance().asCoroutineDispatcher()

val clientScope: CoroutineScope by lazy { CoroutineScope(clientDispatcher) }
