package net.yukulab.fabpose.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fill1890.fabsit.config.ConfigManager
import net.fill1890.fabsit.entity.Pose
import net.fill1890.fabsit.error.PoseException
import net.fill1890.fabsit.util.Messages
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
        }
    }

    private fun registerPoseCommands(dispatcher: CommandDispatcher<ServerCommandSource>) {
        Pose.entries.forEach { pose ->
            dispatcher.register(
                literal<ServerCommandSource?>(pose.getStaticName())
                    .requires(Permissions.require(pose.getPermissionName(), true))
                    .executes { context ->
                        val source = context.source
                        val player = source.player ?: run {
                            source.sendError(Text.of("You must be a player to run this command!"))
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
                                    source.sendFeedback({ Messages.configLoadSuccess(source.player) }, false)
                                    0
                                },
                                {
                                    source.sendError(Messages.configLoadError(source.player))
                                    -1
                                },
                            )
                        },
                ),
        )
    }
}
