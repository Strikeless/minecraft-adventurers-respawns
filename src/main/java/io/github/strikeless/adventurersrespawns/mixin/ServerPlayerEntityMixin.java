package io.github.strikeless.adventurersrespawns.mixin;

import com.mojang.authlib.GameProfile;
import io.github.strikeless.adventurersrespawns.AdventurersRespawns;
import io.github.strikeless.adventurersrespawns.feature.SpawnPositionFeature;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.random.RandomGenerator;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity {
    @Shadow
    public abstract ServerWorld getServerWorld();

    @Shadow
    public abstract boolean isSpectator();

    public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Inject(method = "getRespawnTarget", at = @At("RETURN"), cancellable = true)
    private void getRespawnTarget(CallbackInfoReturnable<TeleportTarget> info) {
        final var config = AdventurersRespawns.getConfig();

        if (config.respawnAtStructure) {
            final var respawnPos = SpawnPositionFeature.getSpawnPosition((ServerPlayerEntity) (Object) this);

            if (respawnPos.isPresent()) {
                info.setReturnValue(new TeleportTarget(
                        this.getServerWorld(),
                        respawnPos.get().toBottomCenterPos(),
                        Vec3d.ZERO, // Velocity
                        RandomGenerator.getDefault().nextFloat(0.0F, 360.0F), // Yaw
                        0.0F, // Pitch
                        false, // Missing respawn
                        false, // As passenger
                        Set.of(), // Relatives
                        TeleportTarget.NO_OP // Post teleport transition
                ));
            } else {
                AdventurersRespawns.getLogger().warn("No structure found for respawning. Falling back to vanilla behavior.");
            }
        }
    }

    @Inject(method = "copyFrom", at = @At("RETURN"))
    private void copyFrom(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo info) {
        // PlayerEntity.getExperienceToDrop and PlayerEntity.shouldAlwaysDropExperience also related to this feature, found in PlayerEntityMixin.
        if (!alive && !this.isSpectator()) {
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
