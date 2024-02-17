package net.fill1890.fabsit;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fill1890.fabsit.event.UseStairCallback;

public class FabSitServer implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        // use a stair to sit
        UseBlockCallback.EVENT.register(UseStairCallback::interact);
    }
}
