package io.github.strikeless.adventurersrespawns.feature;

import io.github.strikeless.adventurersrespawns.AdventurersRespawns;
import net.minecraft.server.MinecraftServer;

import java.util.random.RandomGenerator;

public class RespawnTimeSkipFeature {
    public static void applyTimeSkip(MinecraftServer server) {
        var config = AdventurersRespawns.getConfig();

        var timeSkip = RandomGenerator.getDefault().nextLong(
                config.timeSkipMinTime,
                config.timeSkipMaxTime
        );

        for (var world : server.getWorlds()) {
            var worldTime = world.getTimeOfDay();
            world.setTimeOfDay(worldTime + timeSkip);
        }

        for (var i = 0; i < config.timeSkipSimulatedTicks; ++i) {
            server.getTickManager().setStepTicks(config.timeSkipSimulatedTicks);
        }
    }
}
