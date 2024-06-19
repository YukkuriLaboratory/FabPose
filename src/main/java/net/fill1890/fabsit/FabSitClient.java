package net.fill1890.fabsit;

import net.fabricmc.api.ClientModInitializer;
import net.fill1890.fabsit.keybind.PoseKeybinds;

public class FabSitClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // keybinds for posing
        PoseKeybinds.register();
    }
}
