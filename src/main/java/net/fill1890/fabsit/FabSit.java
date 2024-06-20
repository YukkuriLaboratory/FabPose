package net.fill1890.fabsit;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fill1890.fabsit.config.ConfigManager;
import net.fill1890.fabsit.error.LoadConfigException;
import net.fill1890.fabsit.event.UseStairCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FabSit implements ModInitializer {

    // mod info
    public static final String MOD_ID = "fabsit";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);


    @Override
    public void onInitialize() {
        try {
            ConfigManager.loadConfig();
        } catch (LoadConfigException ignored) {
            LOGGER.warn("FabPose config not loaded! Using default settings");
        }
        // use a stair to sit
        UseBlockCallback.EVENT.register(UseStairCallback::interact);

        LOGGER.info("FabPose loaded");
    }
}
