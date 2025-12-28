package runner

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import net.minecraft.client.Minecraft

val clientDispatcher = Minecraft.getInstance().asCoroutineDispatcher()

val clientScope: CoroutineScope by lazy { CoroutineScope(clientDispatcher) }
