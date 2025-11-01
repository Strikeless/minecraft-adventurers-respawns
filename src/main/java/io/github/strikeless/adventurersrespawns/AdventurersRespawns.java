package io.github.strikeless.adventurersrespawns;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdventurersRespawns implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("Adventurers Respawns");

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
    }
}
