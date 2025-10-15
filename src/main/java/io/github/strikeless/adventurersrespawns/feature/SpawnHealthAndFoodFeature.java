package io.github.strikeless.adventurersrespawns.feature;

import io.github.strikeless.adventurersrespawns.AdventurersRespawns;
import net.minecraft.server.network.ServerPlayerEntity;

public class SpawnHealthAndFoodFeature {
    public static void setSpawnHealthAndFood(ServerPlayerEntity player) {
        final var config = AdventurersRespawns.getConfig();
        final var hungerManager = player.getHungerManager();

        player.setHealth(config.respawnHealth);
        hungerManager.setFoodLevel(config.respawnFoodLevel);
        hungerManager.setSaturationLevel(config.respawnSaturationLevel);
    }
}
