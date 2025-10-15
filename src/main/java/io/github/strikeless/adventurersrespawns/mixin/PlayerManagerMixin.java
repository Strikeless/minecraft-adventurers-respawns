package io.github.strikeless.adventurersrespawns.mixin;

import io.github.strikeless.adventurersrespawns.AdventurersRespawns;
import io.github.strikeless.adventurersrespawns.feature.RespawnMapsFeature;
import io.github.strikeless.adventurersrespawns.feature.SpawnHealthAndFoodFeature;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {
    @Inject(method = "respawnPlayer", at = @At("TAIL"))
    private void respawnPlayer(CallbackInfoReturnable<ServerPlayerEntity> info) {
        final var config = AdventurersRespawns.getConfig();
        final var respawnedPlayer = info.getReturnValue();

        SpawnHealthAndFoodFeature.setSpawnHealthAndFood(respawnedPlayer);

        if (config.giveDeathPositionMap) {
            RespawnMapsFeature.giveDeathPositionMap(respawnedPlayer);
        }
        if (config.giveSpawnpointMap) {
            RespawnMapsFeature.giveSpawnpointMap(respawnedPlayer);
        }
    }
}
