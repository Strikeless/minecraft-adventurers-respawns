package io.github.strikeless.adventurersrespawns.main.mixin;

import io.github.strikeless.adventurersrespawns.main.AdventurersRespawns;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity {
    @Shadow
    public abstract boolean isSpectator();

    @Shadow
    public int experienceLevel;

    protected PlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "getBaseExperienceReward", at = @At("HEAD"), cancellable = true)
    private void getBaseExperienceReward(ServerLevel level, CallbackInfoReturnable<Integer> info) {
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

    @Inject(method = "isAlwaysExperienceDropper", at = @At("HEAD"), cancellable = true)
    private void isAlwaysExperienceDropper(CallbackInfoReturnable<Boolean> info) {
        var config = AdventurersRespawns.getConfig();

        switch (config.deathExperienceBehavior) {
            case Vanilla -> {}
            case Keep -> info.setReturnValue(false);
            case Drop, Destroy -> info.setReturnValue(true);
        }
    }

    // Mixin ServerPlayer.restoreFrom also related to the deathExperienceBehavior feature, found in ServerPlayerMixin.
}
