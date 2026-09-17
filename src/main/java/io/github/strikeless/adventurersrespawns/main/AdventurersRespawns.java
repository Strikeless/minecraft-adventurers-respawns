package io.github.strikeless.adventurersrespawns.main;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdventurersRespawns implements ModInitializer {
    public static final String MOD_ID = "adventurers-respawns";

    private static Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static AdventurersRespawnsConfig getConfig() {
        return AdventurersRespawnsConfig.HANDLER.instance();
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    @Override
    public void onInitialize() {
        var configLoadedSuccessfully = AdventurersRespawnsConfig.HANDLER.load();
        if (!configLoadedSuccessfully) LOGGER.error("Couldn't load mod config with YACL");
        var config = getConfig();

        // Update logger level from the config, if set.
        if (config.logLevelOverride != null) {
            org.apache.logging.log4j.core.config.Configurator.setLevel(MOD_ID, config.logLevelOverride);
            refreshLogger();
        }
    }

    private static void refreshLogger() {
        LOGGER = LoggerFactory.getLogger(MOD_ID);
    }
}
