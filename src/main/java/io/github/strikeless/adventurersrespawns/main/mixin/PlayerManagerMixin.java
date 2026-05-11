package io.github.strikeless.adventurersrespawns.main.mixin;

import io.github.strikeless.adventurersrespawns.main.AdventurersRespawns;
import io.github.strikeless.adventurersrespawns.main.AdventurersRespawnsConfig;
import io.github.strikeless.adventurersrespawns.main.feature.RespawnAssistanceItemsFeature;
import io.github.strikeless.adventurersrespawns.main.feature.RespawnTimeSkipFeature;
import io.github.strikeless.adventurersrespawns.main.feature.SpawnHealthAndFoodFeature;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerList.class)
public abstract class PlayerManagerMixin {
    @Shadow
    public abstract MinecraftServer getServer();

    @Inject(method = "respawn", at = @At("RETURN"))
    private void respawn(CallbackInfoReturnable<ServerPlayer> info) {
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
            var server = this.getServer();
            RespawnTimeSkipFeature.applyTimeSkip(server);
        }
    }
}
