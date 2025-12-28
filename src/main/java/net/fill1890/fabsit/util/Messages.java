package net.fill1890.fabsit.util;

import net.fill1890.fabsit.config.ConfigManager;
import net.fill1890.fabsit.entity.Pose;
import net.fill1890.fabsit.error.PoseException;
import net.fill1890.fabsit.error.PoseException.BlockOccupied;
import net.fill1890.fabsit.error.PoseException.MidairException;
import net.fill1890.fabsit.error.PoseException.PoseDisabled;
import net.fill1890.fabsit.error.PoseException.SpectatorException;
import net.fill1890.fabsit.error.PoseException.StateException;
import net.fill1890.fabsit.mixin.accessor.ServerCommonPacketListenerImplAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

import static net.fill1890.fabsit.error.PoseException.*;

// this may not be the best way of doing this kind of function
// but it works for now

/**
 * Message lookup functions
 *
 * Supports both server-side and client-side translation
 * Given a player, will check if the player has the mod loaded locally for local translation support
 * If so, will return a translatable key for local translation
 * If not, will return a static response based on the server locale
 */
public class Messages {
    private static final String ACTION = "action.fabsit.";
    private static final String CHAT = "chat.fabsit.";

    // stop posing action message
    public static Component getPoseStopMessage(ServerPlayer player, Pose pose) {
        var connection = ((ServerCommonPacketListenerImplAccessor) player.connection).getConnection();
        if (connection.fabSit$isModEnabled()) {
            return Component.translatable(ACTION + "stop_" + pose, Component.keybind("key.sneak"));
        } else {
            return Component.nullToEmpty(ConfigManager.LANG.get(ACTION + "stop_" + pose).formatted(ConfigManager.LANG.get("key.fabsit.sneak")));
        }
    }

    // get either a server or client translated string based on whether the player has the mod
    private static Component getChatMessageByKey(ServerPlayer player, String key_base) {
        var connection = ((ServerCommonPacketListenerImplAccessor) player.connection).getConnection();
        if (connection.fabSit$isModEnabled()) {
            return Component.translatable(CHAT + key_base);
        } else {
            return Component.nullToEmpty(ConfigManager.LANG.get(CHAT + key_base));
        }
    }

    // trying to pose in midair
    public static Component getMidairError(ServerPlayer player, Pose pose) {
        return getChatMessageByKey(player, switch(pose) {
            case SITTING -> "sit_air_error";
            default -> "pose_air_error";
        });
    }

    // trying to pose while a spectator
    public static Component getSpectatorError(ServerPlayer player, Pose pose) {
        return getChatMessageByKey(player, switch(pose) {
            case SITTING -> "sit_spectator_error";
            default -> "pose_spectator_error";
        });
    }

    // trying to pose while swimming/sleeping/flying/etc
    public static Component getStateError(ServerPlayer player, Pose pose) {
        return getChatMessageByKey(player, switch (pose) {
            case SITTING -> "sit_state_error";
            default -> "pose_state_error";
        });
    }

    // pose disabled
    public static Component poseDisabledError(ServerPlayer player, Pose pose) {
        return getChatMessageByKey(player, switch (pose) {
            case SITTING -> "sit_disabled";
            default -> "pose_disabled";
        });
    }

    public static Component configLoadSuccess(ServerPlayer player) {
        return getChatMessageByKey(player, "reload_success");
    }

    public static Component configLoadError(ServerPlayer player) {
        return getChatMessageByKey(player, "reload_error");
    }

    public static Component blockOccupiedError(ServerPlayer player, Pose pose) {
        return getChatMessageByKey(player, switch(pose) {
            case SITTING -> "sit_block_occupied";
            default -> "pose_block_occupied";
        });
    }

    public static void sendByException(ServerPlayer player, Pose pose, PoseException e) {
        if(e instanceof MidairException) {
            player.sendSystemMessage(getMidairError(player, pose));
        } else if(e instanceof SpectatorException) {
            player.sendSystemMessage(getSpectatorError(player, pose));
        } else if(e instanceof StateException) {
            player.sendSystemMessage(getStateError(player, pose));
        } else if(e instanceof PoseDisabled) {
            player.sendSystemMessage(poseDisabledError(player, pose));
        } else if(e instanceof BlockOccupied) {
            player.sendSystemMessage(blockOccupiedError(player, pose));
        }
    }
}
