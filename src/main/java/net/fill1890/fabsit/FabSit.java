package net.fill1890.fabsit;

import net.fabricmc.api.ModInitializer;
import net.fill1890.fabsit.config.ConfigManager;
import net.fill1890.fabsit.entity.ChairEntity;
import net.fill1890.fabsit.error.LoadConfigException;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FabSit implements ModInitializer {

	// mod info
	public static final String MOD_ID = "fabsit";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);


	// Pose manager and chair entities
	public static final EntityType<ChairEntity> CHAIR_ENTITY_TYPE = ChairEntity.register();

	// packet channel for checking if mod loaded
	public static final Identifier LOADED_CHANNEL = new Identifier(MOD_ID, "check_loaded");
	// packet channel for pose requests (keybinds etc.)
	public static final Identifier REQUEST_CHANNEL = new Identifier(MOD_ID, "request_pose");


	@Override
	public void onInitialize() {
		try {
			ConfigManager.loadConfig();
		} catch(LoadConfigException ignored) {
            LOGGER.warn("FabPose config not loaded! Using default settings");
		}

        LOGGER.info("FabPose loaded");
	}
}
