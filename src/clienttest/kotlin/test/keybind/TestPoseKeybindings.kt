package test.keybind

import com.mojang.blaze3d.platform.InputConstants
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import net.fill1890.fabsit.entity.Pose
import net.fill1890.fabsit.keybind.PoseKeybinds
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket
import net.yukulab.fabpose.extension.currentPose
import net.yukulab.fabpose.extension.loop
import runner.clientDispatcher

object TestPoseKeybindings {
    suspend fun testSitKey(client: Minecraft) {
        testKey(client, PoseKeybinds.sitKey, Pose.SITTING)
    }

    suspend fun testLayKey(client: Minecraft) {
        testKey(client, PoseKeybinds.layKey, Pose.LAYING)
    }

    suspend fun testSpinKey(client: Minecraft) {
        testKey(client, PoseKeybinds.spinKey, Pose.SPINNING)
    }

    suspend fun testSwimKey(client: Minecraft) {
        testKey(client, PoseKeybinds.swimKey, Pose.SWIMMING)
    }

    private suspend fun testKey(client: Minecraft, key: KeyMapping, pose: Pose) {
        val player = client.player.shouldNotBeNull()
        withContext(clientDispatcher) {
            //? if <26.3 {
            val inputKey = InputConstants.Type.KEYSYM.getOrCreate(InputConstants.KEY_H)
            //?} else {
            /*val inputKey = InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_H)
            *///?}
            key.setKey(inputKey)
            KeyMapping.resetMapping()
            try {
                withClue("Checking player is posing $pose") {
                    withTimeout(2.seconds) {
                        loop(50.milliseconds) {
                            KeyMapping.click(inputKey)
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

                if (pose != Pose.SWIMMING) {
                    withClue("Checking passenger updates preserve $pose") {
                        val vehicle = player.vehicle.shouldNotBeNull()
                        client.connection.shouldNotBeNull()
                            .handleSetEntityPassengersPacket(ClientboundSetPassengersPacket(vehicle))
                        player.currentPose shouldBe pose
                        player.vehicle shouldBe vehicle
                        player.isInvisible shouldBe (pose in setOf(Pose.LAYING, Pose.SPINNING))
                    }
                }
            } finally {
                key.setKey(key.defaultKey)
                KeyMapping.resetMapping()
            }
        }
    }
}
