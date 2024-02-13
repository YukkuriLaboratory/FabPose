package net.yukulab.fabsit

import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.yukulab.fabsit.command.Command
import net.yukulab.fabsit.entity.FabSitEntities
import net.yukulab.fabsit.network.Networking

class FabSit : ModInitializer {
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
