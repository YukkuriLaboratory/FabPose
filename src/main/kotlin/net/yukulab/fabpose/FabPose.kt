package net.yukulab.fabpose

import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.yukulab.fabpose.command.Command
import net.yukulab.fabpose.entity.FabSitEntities
import net.yukulab.fabpose.network.Networking

class FabPose : ModInitializer {
    private val job = Job()
    override fun onInitialize() {
        coroutineScope = CoroutineScope(Dispatchers.Default + job)
        ServerLifecycleEvents.SERVER_STARTED.register {
            server = it
        }
        ServerLifecycleEvents.SERVER_STOPPING.register {
            runBlocking {
                withTimeoutOrNull(1.minutes) {
                    job.cancelAndJoin()
                }
            }
        }
        Command.register()
        FabSitEntities.register()
        Networking.registerServerHandlers()
    }
}
