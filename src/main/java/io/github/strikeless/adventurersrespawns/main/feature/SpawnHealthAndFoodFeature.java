package io.github.strikeless.adventurersrespawns.main.feature;

import io.github.strikeless.adventurersrespawns.main.AdventurersRespawns;
import net.minecraft.server.level.ServerPlayer;

public class SpawnHealthAndFoodFeature {
    public static void setSpawnHealthAndFood(ServerPlayer player) {
        var config = AdventurersRespawns.getConfig();
        var playerFoodData = player.getFoodData();

        player.setHealth(config.respawnHealth);
        playerFoodData.setFoodLevel(config.respawnFoodLevel);
        playerFoodData.setSaturation(config.respawnSaturationLevel);
    }
}
