package io.github.strikeless.adventurersrespawns.main.feature;

import io.github.strikeless.adventurersrespawns.main.AdventurersRespawns;
import net.minecraft.server.MinecraftServer;

import java.util.random.RandomGenerator;

public class RespawnTimeSkipFeature {
    public static void applyTimeSkip(MinecraftServer server) {
        var config = AdventurersRespawns.getConfig();

        int timeSkipTicks;
        if (config.timeSkipMaxTimeTicks <= config.timeSkipMinTimeTicks) {
            // The user has configured time skip max ticks to be less than min ticks, which makes no sense.
            // I don't think it's trivial to prevent this from happening from the YACL config GUI, so let's just use the max value.
            // Bit hacky, but at least it doesn't throw exceptions.
            timeSkipTicks = config.timeSkipMaxTimeTicks;
        } else {
            timeSkipTicks = RandomGenerator.getDefault().nextInt(
                config.timeSkipMinTimeTicks,
                config.timeSkipMaxTimeTicks
            );
        }

        var clockManager = server.clockManager();
        for (var level : server.getAllLevels()) {
            var levelDefaultClock = level.dimensionType().defaultClock();

            // Only add ticks to the level's default clock if one actually exists. I think the nether has no default clock?
            levelDefaultClock.ifPresent(worldClockHolder -> {
                clockManager.addTicks(worldClockHolder, timeSkipTicks);
            });
        }
    }
}
