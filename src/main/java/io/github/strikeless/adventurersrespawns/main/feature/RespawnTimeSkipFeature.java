package io.github.strikeless.adventurersrespawns.main.feature;

import io.github.strikeless.adventurersrespawns.main.AdventurersRespawns;
import net.minecraft.server.MinecraftServer;

import java.util.random.RandomGenerator;

public class RespawnTimeSkipFeature {
    public static void applyTimeSkip(MinecraftServer server) {
        var config = AdventurersRespawns.getConfig();

        long timeSkip;
        if (config.timeSkipMaxTime <= config.timeSkipMinTime) {
            timeSkip = config.timeSkipMaxTime;
        } else {
            timeSkip = RandomGenerator.getDefault().nextLong(
                    config.timeSkipMinTime,
                    config.timeSkipMaxTime
            );
        }

        for (var level : server.getAllLevels()) {
            var levelTime = level.getDayTime();
            level.setDayTime(levelTime + timeSkip);
        }
    }
}
