package io.github.strikeless.adventurersrespawns.mixin;

import io.github.strikeless.adventurersrespawns.AdventurersRespawns;
import io.github.strikeless.adventurersrespawns.AdventurersRespawnsConfig;
import io.github.strikeless.adventurersrespawns.feature.RespawnAssistanceItemsFeature;
import io.github.strikeless.adventurersrespawns.feature.RespawnTimeSkipFeature;
import io.github.strikeless.adventurersrespawns.feature.SpawnHealthAndFoodFeature;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {
    @Inject(method = "respawnPlayer", at = @At("RETURN"))
    private void respawnPlayer(CallbackInfoReturnable<ServerPlayerEntity> info) {
        var config = AdventurersRespawns.getConfig();
        var respawnedPlayer = info.getReturnValue();

        SpawnHealthAndFoodFeature.setSpawnHealthAndFood(respawnedPlayer);

        if (config.giveDeathPositionMap) {
            RespawnAssistanceItemsFeature.giveDeathPositionMap(respawnedPlayer);
        }
        if (config.giveSpawnpointMap) {
            RespawnAssistanceItemsFeature.giveSpawnpointMap(respawnedPlayer);
        }

        if (config.giveCompassType != AdventurersRespawnsConfig.GivenCompassType.None) {
            RespawnAssistanceItemsFeature.giveCompass(respawnedPlayer, config.giveCompassType);
        }

        if (config.timeSkipOnRespawn) {
            var server = Objects.requireNonNull(respawnedPlayer.getServer());
            RespawnTimeSkipFeature.applyTimeSkip(server);
        }
    }
}
