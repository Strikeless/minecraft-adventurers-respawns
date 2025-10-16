package io.github.strikeless.adventurersrespawns.mixin;

import io.github.strikeless.adventurersrespawns.AdventurersRespawns;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {
    @Shadow
    public abstract boolean isSpectator();

    @Shadow
    public int experienceLevel;

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "getExperienceToDrop", at = @At("HEAD"), cancellable = true)
    private void getExperienceToDrop(ServerWorld world, CallbackInfoReturnable<Integer> info) {
        var config = AdventurersRespawns.getConfig();

        switch (config.deathExperienceBehavior) {
            case Vanilla -> {}
            case Keep, Destroy -> {
                info.setReturnValue(0);
            }
            case Drop -> {
                // Unfortunately the calculation logic is in the same method so we'll just rewrite that here and
                // hope there aren't any other mods changing experience drops that we're breaking by doing this...
                var experienceToDrop = !this.isSpectator() ? Math.min(this.experienceLevel * 7, 100) : 0;
                info.setReturnValue(experienceToDrop);
            }
        }
    }

    @Inject(method = "shouldAlwaysDropExperience", at = @At("HEAD"), cancellable = true)
    private void shouldAlwaysDropExperience(CallbackInfoReturnable<Boolean> info) {
        var config = AdventurersRespawns.getConfig();

        switch (config.deathExperienceBehavior) {
            case Vanilla -> {}
            case Keep -> info.setReturnValue(false);
            case Drop, Destroy -> info.setReturnValue(true);
        }
    }

    // Mixin ServerPlayerEntity.copyFrom also related to the deathExperienceBehavior feature, found in ServerPlayerEntityMixin.
}
