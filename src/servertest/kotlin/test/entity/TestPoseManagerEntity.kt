package test.entity

import extension.addInstantFinalTask
import extension.runCatchingAssertion
import extension.waitAndRun
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import mock.createMockServerPlayer
import net.fabricmc.fabric.api.gametest.v1.GameTest
import net.fill1890.fabsit.entity.Pose
import net.fill1890.fabsit.error.PoseException.MidairException
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.SlabBlock
import net.minecraft.world.level.block.state.properties.SlabType
import net.yukulab.fabpose.DelegatedLogger
import net.yukulab.fabpose.extension.pose

@Suppress("UNUSED")
class TestPoseManagerEntity {
    @GameTest
    fun checkSitOnSlabBlock(context: GameTestHelper) = runCatchingAssertion(logger, context) {
        poseOnSlabBlock(context, Pose.SITTING)
    }

    @GameTest
    fun checkSitOnSlabBlockLowHeight(context: GameTestHelper) = runCatchingAssertion(logger, context) {
        poseOnSlabBlock(context, Pose.SITTING, blockHeight = 2)
    }

    @GameTest
    fun checkSitOnSlabBlockWithSneak(context: GameTestHelper) = runCatchingAssertion(logger, context) {
        poseOnSlabBlock(context, Pose.SITTING, removeWithSneak = true)
    }

    @GameTest
    fun checkLayOnSlabBlock(context: GameTestHelper) = runCatchingAssertion(logger, context) {
        poseOnSlabBlock(context, Pose.LAYING)
    }

    @GameTest
    fun checkLayOnSlabBlockLowHeight(context: GameTestHelper) = runCatchingAssertion(logger, context) {
        poseOnSlabBlock(context, Pose.SITTING, blockHeight = 2)
    }

    @GameTest
    fun checkLayOnSlabBlockWithSneak(context: GameTestHelper) = runCatchingAssertion(logger, context) {
        poseOnSlabBlock(context, Pose.SITTING, removeWithSneak = true)
    }

    @GameTest
    fun checkSpinOnSlabBlock(context: GameTestHelper) = runCatchingAssertion(logger, context) {
        poseOnSlabBlock(context, Pose.SPINNING)
    }

    @GameTest
    fun checkSpinOnSlabBlockLowHeight(context: GameTestHelper) = runCatchingAssertion(logger, context) {
        poseOnSlabBlock(context, Pose.SITTING, blockHeight = 2)
    }

    @GameTest
    fun checkSpinOnSlabBlockWithSneak(context: GameTestHelper) = runCatchingAssertion(logger, context) {
        poseOnSlabBlock(context, Pose.SITTING, removeWithSneak = true)
    }

    @GameTest
    fun checkAirBockShouldFailed(context: GameTestHelper) = runCatchingAssertion(logger, context) {
        context.addInstantFinalTask(logger) {
            val mockPlayer = context.createMockServerPlayer(BlockPos(0, 3, 0))
            mockPlayer.pose(Pose.SITTING).shouldBeFailure<MidairException>()
            checkPlayerNotPosing(mockPlayer)
        }
    }

    @GameTest
    fun checkBlockUpdated(context: GameTestHelper) = runCatchingAssertion(logger, context) {
        val blockHeight = 4
        context.setBlock(
            BlockPos(0, blockHeight, 0),
            Blocks.STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM),
        )
        val mockPlayer = context.createMockServerPlayer(BlockPos(0, blockHeight + 1, 0))
        mockPlayer.absSnapTo(mockPlayer.x, mockPlayer.y - 0.5, mockPlayer.z)
        val pose = Pose.SITTING
        mockPlayer.pose(pose).shouldBeSuccess()
        context.waitAndRun(2, logger) {
            checkPlayerPosing(mockPlayer, pose)
            context.setBlock(BlockPos(0, blockHeight, 0), Blocks.AIR)
            context.runAfterDelay(2) {
                context.addInstantFinalTask(logger) {
                    checkPlayerNotPosing(mockPlayer)
                }
            }
        }
    }

    private fun poseOnSlabBlock(context: GameTestHelper, pose: Pose, blockHeight: Int = 4, removeWithSneak: Boolean = false) {
        context.setBlock(
            BlockPos(0, blockHeight, 0),
            Blocks.STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM),
        )
        val mockPlayer = context.createMockServerPlayer(BlockPos(0, blockHeight + 1, 0))
        mockPlayer.absSnapTo(mockPlayer.x, mockPlayer.y - 0.4, mockPlayer.z)
        mockPlayer.pose(pose).getOrThrow()
        context.waitAndRun(2, logger) {
            checkPlayerPosing(mockPlayer, pose)
            if (removeWithSneak) {
                mockPlayer.setShiftKeyDown(true)
            } else {
                mockPlayer.pose(pose, checkSpam = false).getOrThrow()
            }
            context.runAfterDelay(2) {
                context.addInstantFinalTask(logger) {
                    checkPlayerNotPosing(mockPlayer)
                }
            }
        }
    }

    private fun checkPlayerPosing(player: ServerPlayer, pose: Pose) {
        player.vehicle.shouldNotBeNull().isAlive.shouldBeTrue()
        if (pose in setOf(Pose.LAYING, Pose.SPINNING)) {
            player.isInvisible.shouldBeTrue()
        } else {
            player.isInvisible.shouldBeFalse()
        }
    }

    private fun checkPlayerNotPosing(player: ServerPlayer) {
        player.vehicle.shouldBeNull()
        player.isInvisible.shouldBeFalse()
    }

    companion object {
        val logger by DelegatedLogger()
    }
}
