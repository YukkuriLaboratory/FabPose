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
import net.fill1890.fabsit.mixin.accessor.LivingEntityAccessor
import net.fill1890.fabsit.mixin.accessor.MannequinAccessor
import net.fill1890.fabsit.util.Messages
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.Pose as EntityPose
import net.minecraft.world.entity.decoration.Mannequin
import net.minecraft.world.item.component.ResolvableProfile
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

    private fun registerPoseCommands(dispatcher: CommandDispatcher<CommandSourceStack>) {
        Pose.entries.forEach { pose ->
            dispatcher.register(
                literal<CommandSourceStack?>(pose.getStaticName())
                    .requires(Permissions.require(pose.getPermissionName(), true))
                    .executes { context ->
                        val source = context.source
                        val player = source?.player ?: run {
                            source?.sendFailure(Component.nullToEmpty("You must be a player to run this command!"))
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

    private fun registerReloadCommand(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            literal<CommandSourceStack?>(MOD_ID)
                .then(
                    literal<CommandSourceStack?>("reload")
                        .requires(Permissions.require("$MOD_ID.reload", 2))
                        .executes {
                            val source = it.source
                            runCatching {
                                ConfigManager.loadConfig()
                            }.fold(
                                {
                                    source?.sendSuccess({ Messages.configLoadSuccess(source.player) }, false)
                                    0
                                },
                                {
                                    source?.sendFailure(Messages.configLoadError(source.player))
                                    -1
                                },
                            )
                        },
                ),
        )
    }

    private fun registerDebugCommand(dispatcher: CommandDispatcher<CommandSourceStack>) {
        val poses = listOf("STANDING", "SLEEPING", "SWIMMING", "CROUCHING", "GLIDING", "SPIN_ATTACK")
        dispatcher.register(
            literal<CommandSourceStack?>(MOD_ID)
                .then(
                    literal<CommandSourceStack?>("debug")
                        .requires(Permissions.require("$MOD_ID.debug", 2))
                        .then(
                            literal<CommandSourceStack?>("mannequin")
                                .then(
                                    argument<CommandSourceStack?, String?>("pose", StringArgumentType.word())
                                        .suggests { _, builder ->
                                            poses.forEach { builder.suggest(it) }
                                            builder.buildFuture()
                                        }
                                        .executes { context ->
                                            val source = context.source
                                            val player = source?.player ?: run {
                                                source?.sendFailure(Component.nullToEmpty("You must be a player to run this command!"))
                                                return@executes -1
                                            }
                                            val poseName = StringArgumentType.getString(context, "pose")
                                            val entityPose = runCatching {
                                                EntityPose.valueOf(poseName.uppercase())
                                            }.getOrElse {
                                                source.sendFailure(Component.nullToEmpty("Invalid pose: $poseName. Valid: ${poses.joinToString()}"))
                                                return@executes -1
                                            }

                                            val world = source.level
                                            val mannequin = Mannequin.create(EntityType.MANNEQUIN, world) ?: run {
                                                source.sendFailure(Component.nullToEmpty("Failed to create MannequinEntity"))
                                                return@executes -1
                                            }

                                            // Set position in front of player (at foot level)
                                            val direction = player.lookAngle.scale(2.0)
                                            val spawnX = player.x + direction.x
                                            val spawnZ = player.z + direction.z
                                            mannequin.snapTo(spawnX, player.y, spawnZ, player.yRot, 0f)

                                            // Set player's profile for skin
                                            val profileComponent = ResolvableProfile.createResolved(player.gameProfile)
                                            mannequin.entityData.set(MannequinAccessor.getPROFILE(), profileComponent)

                                            // Set pose
                                            mannequin.pose = entityPose

                                            // Hide the "NPC" label below the name tag
                                            mannequin.entityData.set(MannequinAccessor.getDESCRIPTION(), java.util.Optional.empty())

                                            // Handle SPIN_ATTACK pose - set LIVING_FLAGS for riptide spinning
                                            if (entityPose == EntityPose.SPIN_ATTACK) {
                                                mannequin.entityData.set(LivingEntityAccessor.getLIVING_FLAGS(), 0x04.toByte())
                                                // Set pitch to -90 degrees to make entity spin vertically (upward)
                                                mannequin.setXRot(-90f)
                                                // Don't show name for SPIN_ATTACK (position would be wrong due to rotation)
                                            } else {
                                                // Set player's name as custom name
                                                mannequin.customName = player.name
                                                mannequin.isCustomNameVisible = true
                                            }

                                            // Sync equipment from player
                                            EquipmentSlot.entries.forEach { slot ->
                                                mannequin.setItemSlot(slot, player.getItemBySlot(slot).copy())
                                            }

                                            world.addFreshEntity(mannequin)
                                            source.sendSuccess(
                                                { Component.nullToEmpty("Spawned MannequinEntity with pose: $poseName") },
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
