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
import net.fill1890.fabsit.mixin.accessor.MannequinEntityAccessor
import net.minecraft.component.type.ProfileComponent
import net.minecraft.entity.EntityPose
import net.minecraft.entity.EntityType
import net.minecraft.entity.decoration.MannequinEntity
import net.minecraft.test.TestContext
import net.minecraft.util.math.BlockPos
import net.yukulab.fabpose.DelegatedLogger

/**
 * Prototype test for MannequinEntity to validate the migration approach from PosingEntity.
 * Tests basic MannequinEntity spawning, profile setting, pose configuration, and position/rotation.
 */
@Suppress("UNUSED")
class TestMannequinEntityPrototype {

    @GameTest
    fun testSpawnMannequinEntity(context: TestContext) = runCatchingAssertion(logger, context) {
        context.addInstantFinalTask(logger) {
            val spawnPos = context.getAbsolutePos(BlockPos(0, 2, 0))
            val mannequin = MannequinEntity.create(EntityType.MANNEQUIN, context.world)

            mannequin.shouldNotBeNull()
            mannequin.refreshPositionAndAngles(spawnPos, 0f, 0f)

            // Verify basic spawning
            context.world.spawnEntity(mannequin).shouldBeTrue()
            mannequin.isAlive.shouldBeTrue()
        }
    }

    @GameTest
    fun testSetProfileComponent(context: TestContext) = runCatchingAssertion(logger, context) {
        context.addInstantFinalTask(logger) {
            val spawnPos = context.getAbsolutePos(BlockPos(0, 2, 0))
            val mannequin = MannequinEntity.create(EntityType.MANNEQUIN, context.world)!!

            mannequin.refreshPositionAndAngles(spawnPos, 0f, 0f)

            // Create a GameProfile and set it via ProfileComponent using our accessor
            val gameProfile = GameProfile(UUID.randomUUID(), "test-mannequin-player")
            val profileComponent = ProfileComponent.ofStatic(gameProfile)

            // Set the profile using our MannequinEntityAccessor
            mannequin.dataTracker.set(MannequinEntityAccessor.getPROFILE(), profileComponent)

            context.world.spawnEntity(mannequin).shouldBeTrue()

            // Verify the profile was set
            val retrievedProfile = mannequin.dataTracker.get(MannequinEntityAccessor.getPROFILE())
            retrievedProfile.shouldNotBeNull()
            retrievedProfile.gameProfile.name.shouldBe("test-mannequin-player")
        }
    }

    @GameTest
    fun testMannequinWithPlayerProfile(context: TestContext) = runCatchingAssertion(logger, context) {
        context.addInstantFinalTask(logger) {
            val mockPlayer = context.createMockServerPlayer(BlockPos(0, 2, 0))
            val spawnPos = context.getAbsolutePos(BlockPos(3, 2, 3))

            val mannequin = MannequinEntity.create(EntityType.MANNEQUIN, context.world)!!
            mannequin.refreshPositionAndAngles(spawnPos, 0f, 0f)

            // Set the mannequin to use the player's profile
            val profileComponent = ProfileComponent.ofStatic(mockPlayer.gameProfile)
            mannequin.dataTracker.set(MannequinEntityAccessor.getPROFILE(), profileComponent)

            // Set a pose
            mannequin.setPose(EntityPose.CROUCHING)

            context.world.spawnEntity(mannequin).shouldBeTrue()

            // Verify the mannequin has the player's profile and correct pose
            mannequin.isAlive.shouldBeTrue()
            mannequin.pose.shouldBe(EntityPose.CROUCHING)
            val profile = mannequin.dataTracker.get(MannequinEntityAccessor.getPROFILE())
            profile.gameProfile.id.shouldBe(mockPlayer.gameProfile.id)
        }
    }

    @GameTest
    fun testSetStandingPose(context: TestContext) = runCatchingAssertion(logger, context) {
        testSetPose(context, EntityPose.STANDING)
    }

    @GameTest
    fun testSetSleepingPose(context: TestContext) = runCatchingAssertion(logger, context) {
        testSetPose(context, EntityPose.SLEEPING)
    }

    @GameTest
    fun testSetSwimmingPose(context: TestContext) = runCatchingAssertion(logger, context) {
        testSetPose(context, EntityPose.SWIMMING)
    }

    @GameTest
    fun testSetCrouchingPose(context: TestContext) = runCatchingAssertion(logger, context) {
        testSetPose(context, EntityPose.CROUCHING)
    }

    @GameTest
    fun testSetGlidingPose(context: TestContext) = runCatchingAssertion(logger, context) {
        testSetPose(context, EntityPose.GLIDING)
    }

    @GameTest
    fun testSetPositionAndRotation(context: TestContext) = runCatchingAssertion(logger, context) {
        context.addInstantFinalTask(logger) {
            val spawnPos = context.getAbsolutePos(BlockPos(1, 3, 2))
            val yaw = 45.0f
            val pitch = 30.0f

            val mannequin = MannequinEntity.create(EntityType.MANNEQUIN, context.world)!!
            mannequin.refreshPositionAndAngles(spawnPos, yaw, pitch)

            context.world.spawnEntity(mannequin).shouldBeTrue()

            // Verify position (refreshPositionAndAngles sets to block center)
            mannequin.x.shouldBe(spawnPos.x.toDouble() + 0.5)
            mannequin.y.shouldBe(spawnPos.y.toDouble())
            mannequin.z.shouldBe(spawnPos.z.toDouble() + 0.5)

            // Verify rotation
            mannequin.yaw.shouldBe(yaw)
            mannequin.pitch.shouldBe(pitch)
        }
    }

    @GameTest
    fun testMannequinPersistence(context: TestContext) = runCatchingAssertion(logger, context) {
        val spawnPos = context.getAbsolutePos(BlockPos(0, 2, 0))

        val mannequin = MannequinEntity.create(EntityType.MANNEQUIN, context.world)!!
        mannequin.refreshPositionAndAngles(spawnPos, 90f, 0f)
        mannequin.setPose(EntityPose.SWIMMING)

        context.world.spawnEntity(mannequin).shouldBeTrue()

        // Wait a few ticks and verify the mannequin is still there with correct state
        context.waitAndRun(5, logger) {
            context.addInstantFinalTask(logger) {
                mannequin.isAlive.shouldBeTrue()
                mannequin.pose.shouldBe(EntityPose.SWIMMING)
                mannequin.yaw.shouldBe(90f)
            }
        }
    }

    @GameTest
    fun testMannequinKill(context: TestContext) = runCatchingAssertion(logger, context) {
        val spawnPos = context.getAbsolutePos(BlockPos(0, 2, 0))

        val mannequin = MannequinEntity.create(EntityType.MANNEQUIN, context.world)!!
        mannequin.refreshPositionAndAngles(spawnPos, 0f, 0f)

        context.world.spawnEntity(mannequin).shouldBeTrue()
        mannequin.isAlive.shouldBeTrue()

        // Kill the mannequin
        mannequin.kill(context.world)

        // Verify it's removed
        context.waitAndRun(2, logger) {
            context.addInstantFinalTask(logger) {
                mannequin.isAlive.shouldBe(false)
            }
        }
    }

    private fun testSetPose(context: TestContext, pose: EntityPose) {
        context.addInstantFinalTask(logger) {
            val spawnPos = context.getAbsolutePos(BlockPos(0, 2, 0))
            val mannequin = MannequinEntity.create(EntityType.MANNEQUIN, context.world)!!

            mannequin.refreshPositionAndAngles(spawnPos, 0f, 0f)
            mannequin.setPose(pose)

            context.world.spawnEntity(mannequin).shouldBeTrue()

            // Verify the pose was set correctly
            mannequin.pose.shouldBe(pose)
            mannequin.isAlive.shouldBeTrue()
        }
    }

    companion object {
        val logger by DelegatedLogger()
    }
}
