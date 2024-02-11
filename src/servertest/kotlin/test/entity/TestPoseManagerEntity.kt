package test.entity

import extension.addInstantFinalTask
import extension.runCatchingAssertion
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import mock.createMockServerPlayer
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.fill1890.fabsit.command.GenericSitBasedCommand
import net.fill1890.fabsit.entity.Pose
import net.minecraft.block.Blocks
import net.minecraft.block.SlabBlock
import net.minecraft.block.enums.SlabType
import net.minecraft.test.GameTest
import net.minecraft.test.TestContext
import net.minecraft.util.math.BlockPos
import net.yukulab.fabsit.DelegatedLogger

class TestPoseManagerEntity : FabricGameTest {
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    fun checkSitOnSlabBlock(context: TestContext) = runCatchingAssertion(logger) {
        poseOnSlabBlock(context, Pose.SITTING)
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    fun checkLayOnSlabBlock(context: TestContext) = runCatchingAssertion(logger) {
        poseOnSlabBlock(context, Pose.LAYING)
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    fun checkSpinOnSlabBlock(context: TestContext) = runCatchingAssertion(logger) {
        poseOnSlabBlock(context, Pose.SPINNING)
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    fun checkAirBockShouldFailed(context: TestContext) = runCatchingAssertion(logger) {
        context.addInstantFinalTask(logger) {
            val mockPlayer = context.createMockServerPlayer(BlockPos(0, 3, 0))
            GenericSitBasedCommand.run(mockPlayer, Pose.SITTING) shouldBe -1
            mockPlayer.vehicle.shouldBeNull()
        }
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    fun checkBlockUpdated(context: TestContext) = runCatchingAssertion(logger) {
        val blockHeight = 2
        context.setBlockState(
            BlockPos(0, blockHeight, 0),
            Blocks.STONE_SLAB.defaultState.with(SlabBlock.TYPE, SlabType.BOTTOM),
        )
        val mockPlayer = context.createMockServerPlayer(BlockPos(0, blockHeight + 1, 0))
        GenericSitBasedCommand.run(mockPlayer, Pose.SITTING) shouldBe 1
        mockPlayer.vehicle.shouldNotBeNull().isAlive.shouldBeTrue()
        context.setBlockState(BlockPos(0, blockHeight, 0), Blocks.AIR)
        context.waitAndRun(5) {
            context.addInstantFinalTask(logger) {
                mockPlayer.vehicle.shouldBeNull()
            }
        }
    }

    private fun poseOnSlabBlock(context: TestContext, pose: Pose) {
        val blockHeight = 2
        context.setBlockState(
            BlockPos(0, blockHeight, 0),
            Blocks.STONE_SLAB.defaultState.with(SlabBlock.TYPE, SlabType.BOTTOM),
        )
        val mockPlayer = context.createMockServerPlayer(BlockPos(0, blockHeight + 1, 0))
        mockPlayer.updatePosition(mockPlayer.x, mockPlayer.y - 0.4, mockPlayer.z)
        GenericSitBasedCommand.run(mockPlayer, pose) shouldBe 1
        context.waitAndRun(5) {
            context.addInstantFinalTask(logger) {
                mockPlayer.vehicle.shouldNotBeNull().isAlive.shouldBeTrue()
            }
        }
    }

    companion object {
        val logger by DelegatedLogger()
    }
}
