package io.github.strikeless.adventurersrespawns.client.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity {
    @Unique
    private Text respawnButtonOriginalMessage = null;

    public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Inject(at = @At("HEAD"), method = "getRespawnTarget")
    private void getRespawnTargetHead(CallbackInfoReturnable<TeleportTarget> info) {
        var mc = MinecraftClient.getInstance();

        if (mc.currentScreen instanceof DeathScreen deathScreen) {
            var deathScreenAccessor = (DeathScreenAccessor) deathScreen;
            var respawnButton = deathScreenAccessor.getButtons().getFirst();

            respawnButtonOriginalMessage = respawnButton.getMessage();
            respawnButton.setMessage(Text.literal("Looking for a spawn..."));
        }
    }

    @Inject(at = @At("RETURN"), method = "getRespawnTarget")
    private void getRespawnTargetReturn(CallbackInfoReturnable<TeleportTarget> info) {
        var mc = MinecraftClient.getInstance();

        if (mc.currentScreen instanceof DeathScreen deathScreen) {
            var deathScreenAccessor = (DeathScreenAccessor) deathScreen;
            var respawnButton = deathScreenAccessor.getButtons().getFirst();

            respawnButton.setMessage(respawnButtonOriginalMessage);
        }
    }
}
