package test.keybind

import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import net.fill1890.fabsit.entity.Pose
import net.fill1890.fabsit.keybind.PoseKeybinds
import net.minecraft.client.MinecraftClient
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.yukulab.fabsit.extension.currentPose
import net.yukulab.fabsit.extension.loop
import runner.clientDispatcher

object TestPoseKeybindings {
    suspend fun testSitKey(client: MinecraftClient) {
        testKey(client, PoseKeybinds.sitKey, Pose.SITTING)
    }

    suspend fun testLayKey(client: MinecraftClient) {
        testKey(client, PoseKeybinds.layKey, Pose.LAYING)
    }

    suspend fun testSpinKey(client: MinecraftClient) {
        testKey(client, PoseKeybinds.spinKey, Pose.SPINNING)
    }

    suspend fun testSwimKey(client: MinecraftClient) {
        testKey(client, PoseKeybinds.swimKey, Pose.SWIMMING)
    }

    private suspend fun testKey(client: MinecraftClient, key: KeyBinding, pose: Pose) {
        val player = client.player.shouldNotBeNull()
        withContext(clientDispatcher) {
            val inputKey = InputUtil.Type.KEYSYM.createFromCode(InputUtil.GLFW_KEY_H)
            key.setBoundKey(inputKey)
            KeyBinding.updateKeysByCode()
            withClue("Checking player is posing $pose") {
                withTimeout(2.seconds) {
                    loop(50.milliseconds) {
                        KeyBinding.onKeyPressed(inputKey)
                        val validPlayerState =
                            player.currentPose == pose && (pose == Pose.SWIMMING || player.vehicle != null)
                        val validPlayerVisible = if (pose in setOf(Pose.LAYING, Pose.SPINNING)) {
                            player.isInvisible
                        } else {
                            !player.isInvisible
                        }
                        !validPlayerState || !validPlayerVisible
                    }
                }
            }
            key.setBoundKey(key.defaultKey)
            KeyBinding.updateKeysByCode()
        }
    }
}
