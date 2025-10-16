package io.github.strikeless.adventurersrespawns.client.mixin;

import com.mojang.authlib.GameProfile;
import io.github.strikeless.adventurersrespawns.feature.SpawnPositionFeature;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity {
    @Unique
    private Text respawnButtonOriginalMessage = null;

    @Unique
    private Text originalScoreText = null;

    @Unique
    private static final Function<Integer, Boolean> CURRENT_CHUNK_SEARCH_EXTENT_TEXT_UPDATER = currentChunkSearchExtent -> {
        var mc = MinecraftClient.getInstance();

        if (mc.currentScreen instanceof DeathScreen deathScreen) {
            final var deathScreenAccessor = (DeathScreenAccessor) deathScreen;

            final var text = Text.literal("Searching for spawn chunks ")
                    .append(Text.literal(currentChunkSearchExtent.toString()).formatted(Formatting.YELLOW))
                    .append(Text.literal(" chunks away..."));

            deathScreenAccessor.setScoreText(text);
            return true;
        } else {
            // Not in death screen anymore? Remove listener.
            return false;
        }
    };

    public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Inject(at = @At("HEAD"), method = "getRespawnTarget")
    private void getRespawnTargetHead(CallbackInfoReturnable<TeleportTarget> info) {
        var mc = MinecraftClient.getInstance();

        if (mc.currentScreen instanceof DeathScreen deathScreen) {
            final var deathScreenAccessor = (DeathScreenAccessor) deathScreen;

            final var respawnButton = deathScreenAccessor.getButtons().getFirst();
            respawnButtonOriginalMessage = respawnButton.getMessage();
            respawnButton.setMessage(Text.literal("Looking for a spawn..."));

            originalScoreText = deathScreenAccessor.getScoreText();
            SpawnPositionFeature.registerCurrentChunkSearchExtentListener(CURRENT_CHUNK_SEARCH_EXTENT_TEXT_UPDATER);
        }
    }

    @Inject(at = @At("RETURN"), method = "getRespawnTarget")
    private void getRespawnTargetReturn(CallbackInfoReturnable<TeleportTarget> info) {
        final var mc = MinecraftClient.getInstance();

        if (mc.currentScreen instanceof DeathScreen deathScreen) {
            final var deathScreenAccessor = (DeathScreenAccessor) deathScreen;

            final var respawnButton = deathScreenAccessor.getButtons().getFirst();
            respawnButton.setMessage(respawnButtonOriginalMessage);

            SpawnPositionFeature.unregisterCurrentChunkSearchExtentListener(CURRENT_CHUNK_SEARCH_EXTENT_TEXT_UPDATER);
            deathScreenAccessor.setScoreText(originalScoreText);
        }
    }
}
