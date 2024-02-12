package test.keybind

import io.kotest.assertions.withClue
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
        testKey(client, PoseKeybinds.sitKey)
    }

    suspend fun testLayKey(client: MinecraftClient) {
        testKey(client, PoseKeybinds.layKey)
    }

    suspend fun testSpinKey(client: MinecraftClient) {
        testKey(client, PoseKeybinds.spinKey)
    }

    suspend fun testSwimKey(client: MinecraftClient) {
        testKey(client, PoseKeybinds.swimKey)
    }

    private suspend fun testKey(client: MinecraftClient, key: KeyBinding) {
        val player = client.player.shouldNotBeNull()
        withContext(clientDispatcher) {
            KeyBinding.setKeyPressed(key.defaultKey, true)
            withClue("Checking player is sitting") {
                withTimeout(1.seconds) {
                    loop(50.milliseconds) {
                        val vehicle = player.vehicle ?: return@loop true
                        !vehicle.isAlive
                    }
                }
            }
            KeyBinding.setKeyPressed(key.defaultKey, false)
        }
    }
}
