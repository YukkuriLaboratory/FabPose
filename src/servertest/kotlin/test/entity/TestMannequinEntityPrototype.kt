package test.entity

import com.mojang.authlib.GameProfile
import extension.addInstantFinalTask
import extension.runCatchingAssertion
import extension.waitAndRun
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.util.UUID
import mock.createMockServerPlayer
import net.fabricmc.fabric.api.gametest.v1.GameTest
import net.fill1890.fabsit.mixin.accessor.MannequinAccessor
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Pose
import net.minecraft.world.entity.decoration.Mannequin
import net.minecraft.world.item.component.ResolvableProfile
import net.yukulab.fabpose.DelegatedLogger

/**
 * Prototype test for MannequinEntity to validate the migration approach from PosingEntity.
 * Tests basic MannequinEntity spawning, profile setting, pose configuration, and position/rotation.
 */
@Suppress("UNUSED")
class TestMannequinEntityPrototype {

    @GameTest
    fun testSpawnMannequinEntity(context: GameTestHelper) = runCatchingAssertion(logger, context) {
        context.addInstantFinalTask(logger) {
            val spawnPos = context.absolutePos(BlockPos(0, 2, 0))
            val mannequin = Mannequin.create(EntityType.MANNEQUIN, context.level)

            mannequin.shouldNotBeNull()
            mannequin.snapTo(spawnPos, 0f, 0f)

            // Verify basic spawning
            context.level.addFreshEntity(mannequin).shouldBeTrue()
            mannequin.isAlive.shouldBeTrue()
        }
    }

    @GameTest
    fun testSetProfileComponent(context: GameTestHelper) = runCatchingAssertion(logger, context) {
        context.addInstantFinalTask(logger) {
            val spawnPos = context.absolutePos(BlockPos(0, 2, 0))
            val mannequin = Mannequin.create(EntityType.MANNEQUIN, context.level)!!

            mannequin.snapTo(spawnPos, 0f, 0f)

            // Create a GameProfile and set it via ProfileComponent using our accessor
            val gameProfile = GameProfile(UUID.randomUUID(), "test-mannequin-player")
            val profileComponent = ResolvableProfile.createResolved(gameProfile)

            // Set the profile using our MannequinEntityAccessor
            mannequin.entityData.set(MannequinAccessor.getPROFILE(), profileComponent)

            context.level.addFreshEntity(mannequin).shouldBeTrue()

            // Verify the profile was set
            val retrievedProfile = mannequin.entityData.get(MannequinAccessor.getPROFILE())
            retrievedProfile.shouldNotBeNull()
            retrievedProfile.partialProfile().name.shouldBe("test-mannequin-player")
        }
    }

    @GameTest
    fun testMannequinWithPlayerProfile(context: GameTestHelper) = runCatchingAssertion(logger, context) {
        context.addInstantFinalTask(logger) {
            val mockPlayer = context.createMockServerPlayer(BlockPos(0, 2, 0))
            val spawnPos = context.absolutePos(BlockPos(3, 2, 3))

            val mannequin = Mannequin.create(EntityType.MANNEQUIN, context.level)!!
            mannequin.snapTo(spawnPos, 0f, 0f)

            // Set the mannequin to use the player's profile
            val profileComponent = ResolvableProfile.createResolved(mockPlayer.gameProfile)
            mannequin.entityData.set(MannequinAccessor.getPROFILE(), profileComponent)

            // Set a pose
            mannequin.setPose(Pose.CROUCHING)

            context.level.addFreshEntity(mannequin).shouldBeTrue()

            // Verify the mannequin has the player's profile and correct pose
            mannequin.isAlive.shouldBeTrue()
            mannequin.pose.shouldBe(Pose.CROUCHING)
            val profile = mannequin.entityData.get(MannequinAccessor.getPROFILE())
            profile.partialProfile().id.shouldBe(mockPlayer.gameProfile.id)
        }
    }

    @GameTest
    fun testSetStandingPose(context: GameTestHelper) = runCatchingAssertion(logger, context) {
        testSetPose(context, Pose.STANDING)
    }

    @GameTest
    fun testSetSleepingPose(context: GameTestHelper) = runCatchingAssertion(logger, context) {
        testSetPose(context, Pose.SLEEPING)
    }

    @GameTest
    fun testSetSwimmingPose(context: GameTestHelper) = runCatchingAssertion(logger, context) {
        testSetPose(context, Pose.SWIMMING)
    }

    @GameTest
    fun testSetCrouchingPose(context: GameTestHelper) = runCatchingAssertion(logger, context) {
        testSetPose(context, Pose.CROUCHING)
    }

    @GameTest
    fun testSetGlidingPose(context: GameTestHelper) = runCatchingAssertion(logger, context) {
        testSetPose(context, Pose.FALL_FLYING)
    }

    @GameTest
    fun testSetPositionAndRotation(context: GameTestHelper) = runCatchingAssertion(logger, context) {
        context.addInstantFinalTask(logger) {
            val spawnPos = context.absolutePos(BlockPos(1, 3, 2))
            val yaw = 45.0f
            val pitch = 30.0f

            val mannequin = Mannequin.create(EntityType.MANNEQUIN, context.level)!!
            mannequin.snapTo(spawnPos, yaw, pitch)

            context.level.addFreshEntity(mannequin).shouldBeTrue()

            // Verify position (refreshPositionAndAngles sets to block center)
            mannequin.x.shouldBe(spawnPos.x.toDouble() + 0.5)
            mannequin.y.shouldBe(spawnPos.y.toDouble())
            mannequin.z.shouldBe(spawnPos.z.toDouble() + 0.5)

            // Verify rotation
            mannequin.yRot.shouldBe(yaw)
            mannequin.xRot.shouldBe(pitch)
        }
    }

    @GameTest
    fun testMannequinPersistence(context: GameTestHelper) = runCatchingAssertion(logger, context) {
        val spawnPos = context.absolutePos(BlockPos(0, 2, 0))

        val mannequin = Mannequin.create(EntityType.MANNEQUIN, context.level)!!
        mannequin.snapTo(spawnPos, 90f, 0f)
        mannequin.setPose(Pose.SWIMMING)

        context.level.addFreshEntity(mannequin).shouldBeTrue()

        // Wait a few ticks and verify the mannequin is still there with correct state
        context.waitAndRun(5, logger) {
            context.addInstantFinalTask(logger) {
                mannequin.isAlive.shouldBeTrue()
                mannequin.pose.shouldBe(Pose.SWIMMING)
                mannequin.yRot.shouldBe(90f)
            }
        }
    }

    @GameTest
    fun testMannequinKill(context: GameTestHelper) = runCatchingAssertion(logger, context) {
        val spawnPos = context.absolutePos(BlockPos(0, 2, 0))

        val mannequin = Mannequin.create(EntityType.MANNEQUIN, context.level)!!
        mannequin.snapTo(spawnPos, 0f, 0f)

        context.level.addFreshEntity(mannequin).shouldBeTrue()
        mannequin.isAlive.shouldBeTrue()

        // Kill the mannequin
        mannequin.kill(context.level)

        // Verify it's removed
        context.waitAndRun(2, logger) {
            context.addInstantFinalTask(logger) {
                mannequin.isAlive.shouldBe(false)
            }
        }
    }

    private fun testSetPose(context: GameTestHelper, pose: Pose) {
        context.addInstantFinalTask(logger) {
            val spawnPos = context.absolutePos(BlockPos(0, 2, 0))
            val mannequin = Mannequin.create(EntityType.MANNEQUIN, context.level)!!

            mannequin.snapTo(spawnPos, 0f, 0f)
            mannequin.setPose(pose)

            context.level.addFreshEntity(mannequin).shouldBeTrue()

            // Verify the pose was set correctly
            mannequin.pose.shouldBe(pose)
            mannequin.isAlive.shouldBeTrue()
        }
    }

    companion object {
        val logger by DelegatedLogger()
    }
}
