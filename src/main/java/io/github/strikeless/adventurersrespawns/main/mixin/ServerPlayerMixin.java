package io.github.strikeless.adventurersrespawns.main.mixin;

import com.mojang.authlib.GameProfile;
import io.github.strikeless.adventurersrespawns.main.AdventurersRespawns;
import io.github.strikeless.adventurersrespawns.main.feature.SpawnPositionFeature;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.random.RandomGenerator;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player {
    public ServerPlayerMixin(ServerLevel level, GameProfile gameProfile) {
        super(level, gameProfile);
    }

    @Inject(method = "findRespawnPositionAndUseSpawnBlock", at = @At("RETURN"), cancellable = true)
    private void findRespawnPositionAndUseSpawnBlock(CallbackInfoReturnable<TeleportTransition> info) {
        var thisServerPlayer = (ServerPlayer) (Object) this;
        var config = AdventurersRespawns.getConfig();

        if (config.respawnAtStructures) {
            var respawnDimensionalBlockPos = SpawnPositionFeature.getSpawnPosition(thisServerPlayer);

            if (respawnDimensionalBlockPos.isEmpty()) {
                AdventurersRespawns.getLogger().warn("No structure found for respawning. Falling back to vanilla behavior.");
                return;
            }

            info.setReturnValue(
                new TeleportTransition(
                    respawnDimensionalBlockPos.get().level(),
                    Vec3.atBottomCenterOf(respawnDimensionalBlockPos.get().blockPos()),
                    Vec3.ZERO, // Velocity
                    RandomGenerator.getDefault().nextFloat(0.0F, 360.0F), // Yaw
                    0.0F, // Pitch
                    false, // Missing respawn
                    false, // As passenger
                    Set.of(), // Relatives
                    TeleportTransition.DO_NOTHING // Post teleport transition
                )
            );
        }
    }

    @Inject(method = "restoreFrom", at = @At("RETURN"))
    private void restoreFrom(ServerPlayer oldPlayer, boolean restoreAll, CallbackInfo info) {
        // PlayerEntity.getExperienceToDrop and PlayerEntity.shouldAlwaysDropExperience also related to this feature, found in PlayerEntityMixin.
        if (!restoreAll && !this.isSpectator()) {
            var config = AdventurersRespawns.getConfig();

            switch (config.deathExperienceBehavior) {
                case Vanilla -> {}
                case Keep -> {
                    this.experienceLevel = oldPlayer.experienceLevel;
                    this.totalExperience = oldPlayer.totalExperience;
                    this.experienceProgress = oldPlayer.experienceProgress;
                }
                case Drop, Destroy -> {
                    this.experienceLevel = 0;
                    this.totalExperience = 0;
                    this.experienceProgress = 0;
                }
            }
        }
    }
}
