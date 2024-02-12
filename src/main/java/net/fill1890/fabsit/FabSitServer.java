package net.fill1890.fabsit;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fill1890.fabsit.config.ConfigManager;
import net.fill1890.fabsit.event.UseStairCallback;
import net.fill1890.fabsit.mixin.accessor.ServerLoginNetworkHandlerAccessor;
import net.fill1890.fabsit.network.PoseRequestC2SPacket;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.yukulab.fabsit.extension.ServerPlayerEntityKt;

public class FabSitServer implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        // on player joins, ping them with a fabsit check packet to see if they have the mod loaded
        ServerLoginConnectionEvents.QUERY_START.register(FabSitServer::checkLoaded);
        ServerLoginNetworking.registerGlobalReceiver(FabSit.LOADED_CHANNEL, FabSitServer::handleCheckResponse);

        // keybind receiver
        ServerPlayNetworking.registerGlobalReceiver(FabSit.REQUEST_CHANNEL, FabSitServer::handlePoseRequest);

        // use a stair to sit
        UseBlockCallback.EVENT.register(UseStairCallback::interact);
    }

    private static void handleCheckResponse(MinecraftServer server, ServerLoginNetworkHandler handler, boolean b, PacketByteBuf buf, ServerLoginNetworking.LoginSynchronizer synchronizer, PacketSender sender) {
        if (b) {
            var connection = ((ServerLoginNetworkHandlerAccessor) handler).getConnection();
            server.execute(() -> ConfigManager.loadedPlayers.add(connection.getAddress()));
        }
    }

    private static void checkLoaded(ServerLoginNetworkHandler handler, MinecraftServer server, PacketSender sender, ServerLoginNetworking.LoginSynchronizer synchronizer) {
        sender.sendPacket(FabSit.LOADED_CHANNEL, PacketByteBufs.empty());
    }

    // attempt to pose when requested
    private static void handlePoseRequest(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler networkHandler, PacketByteBuf buf, PacketSender sender) {
        ServerPlayerEntityKt.sit(player, new PoseRequestC2SPacket(buf).getPose());
    }
}
