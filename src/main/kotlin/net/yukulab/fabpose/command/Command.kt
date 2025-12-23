package net.yukulab.fabpose.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.loader.api.FabricLoader
import net.fill1890.fabsit.config.ConfigManager
import net.fill1890.fabsit.entity.Pose
import net.fill1890.fabsit.error.PoseException
import net.fill1890.fabsit.mixin.accessor.MannequinEntityAccessor
import net.fill1890.fabsit.util.Messages
import net.minecraft.component.type.ProfileComponent
import net.minecraft.entity.EntityPose
import net.minecraft.entity.EntityType
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.decoration.MannequinEntity
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.yukulab.fabpose.DelegatedLogger
import net.yukulab.fabpose.MOD_ID
import net.yukulab.fabpose.extension.getPermissionName
import net.yukulab.fabpose.extension.getStaticName
import net.yukulab.fabpose.extension.pose

object Command {
    private val logger by DelegatedLogger()

    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            registerPoseCommands(dispatcher)
            registerReloadCommand(dispatcher)
            if (FabricLoader.getInstance().isDevelopmentEnvironment) {
                registerDebugCommand(dispatcher)
            }
        }
    }

    private fun registerPoseCommands(dispatcher: CommandDispatcher<ServerCommandSource>) {
        Pose.entries.forEach { pose ->
            dispatcher.register(
                literal<ServerCommandSource?>(pose.getStaticName())
                    .requires(Permissions.require(pose.getPermissionName(), true))
                    .executes { context ->
                        val source = context.source
                        val player = source?.player ?: run {
                            source?.sendError(Text.of("You must be a player to run this command!"))
                            return@executes -1
                        }
                        player.pose(pose).fold(
                            { 0 },
                            {
                                logger.error("Failed to pose", it)
                                if (it is PoseException) {
                                    Messages.sendByException(player, pose, it)
                                }
                                -1
                            },
                        )
                    },
            )
        }
    }

    private fun registerReloadCommand(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            literal<ServerCommandSource?>(MOD_ID)
                .then(
                    literal<ServerCommandSource?>("reload")
                        .requires(Permissions.require("$MOD_ID.reload", 2))
                        .executes {
                            val source = it.source
                            runCatching {
                                ConfigManager.loadConfig()
                            }.fold(
                                {
                                    source?.sendFeedback({ Messages.configLoadSuccess(source.player) }, false)
                                    0
                                },
                                {
                                    source?.sendError(Messages.configLoadError(source.player))
                                    -1
                                },
                            )
                        },
                ),
        )
    }

    private fun registerDebugCommand(dispatcher: CommandDispatcher<ServerCommandSource>) {
        val poses = listOf("STANDING", "SLEEPING", "SWIMMING", "CROUCHING", "GLIDING", "SPIN_ATTACK")
        dispatcher.register(
            literal<ServerCommandSource?>(MOD_ID)
                .then(
                    literal<ServerCommandSource?>("debug")
                        .requires(Permissions.require("$MOD_ID.debug", 2))
                        .then(
                            literal<ServerCommandSource?>("mannequin")
                                .then(
                                    argument<ServerCommandSource?, String?>("pose", StringArgumentType.word())
                                        .suggests { _, builder ->
                                            poses.forEach { builder.suggest(it) }
                                            builder.buildFuture()
                                        }
                                        .executes { context ->
                                            val source = context.source
                                            val player = source?.player ?: run {
                                                source?.sendError(Text.of("You must be a player to run this command!"))
                                                return@executes -1
                                            }
                                            val poseName = StringArgumentType.getString(context, "pose")
                                            val entityPose = runCatching {
                                                EntityPose.valueOf(poseName.uppercase())
                                            }.getOrElse {
                                                source.sendError(Text.of("Invalid pose: $poseName. Valid: ${poses.joinToString()}"))
                                                return@executes -1
                                            }

                                            val world = source.world
                                            val mannequin = MannequinEntity.create(EntityType.MANNEQUIN, world) ?: run {
                                                source.sendError(Text.of("Failed to create MannequinEntity"))
                                                return@executes -1
                                            }

                                            // Set position in front of player (at foot level)
                                            val direction = player.rotationVector.multiply(2.0)
                                            val spawnX = player.x + direction.x
                                            val spawnZ = player.z + direction.z
                                            mannequin.refreshPositionAndAngles(spawnX, player.y, spawnZ, player.yaw, 0f)

                                            // Set player's profile for skin
                                            val profileComponent = ProfileComponent.ofStatic(player.gameProfile)
                                            mannequin.dataTracker.set(MannequinEntityAccessor.getPROFILE(), profileComponent)

                                            // Set pose
                                            mannequin.pose = entityPose

                                            // Sync equipment from player
                                            EquipmentSlot.entries.forEach { slot ->
                                                mannequin.equipStack(slot, player.getEquippedStack(slot).copy())
                                            }

                                            world.spawnEntity(mannequin)
                                            source.sendFeedback(
                                                { Text.of("Spawned MannequinEntity with pose: $poseName") },
                                                false,
                                            )
                                            0
                                        },
                                ),
                        ),
                ),
        )
    }
}
