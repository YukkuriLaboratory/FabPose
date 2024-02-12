package test.keybind

import io.kotest.assertions.withClue
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import net.fill1890.fabsit.keybind.PoseKeybinds
import net.minecraft.client.MinecraftClient
import net.minecraft.client.option.KeyBinding
import net.yukulab.fabsit.extension.loop
import runner.clientDispatcher

object TestPoseKeybindings {
    suspend fun testSitKey(client: MinecraftClient) {
        val player = client.player.shouldNotBeNull()
        KeyBinding.setKeyPressed(PoseKeybinds.sitKey.defaultKey, true)
        withContext(clientDispatcher) {
            withTimeout(1.seconds) {
                loop(50.milliseconds) {
                    withClue("Checking player is sitting") {
                        player.vehicle.shouldNotBeNull().isAlive.shouldBeTrue()
                    }
                }
            }
        }
    }
}
