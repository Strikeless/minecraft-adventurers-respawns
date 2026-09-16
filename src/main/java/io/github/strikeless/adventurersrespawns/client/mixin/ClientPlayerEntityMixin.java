package io.github.strikeless.adventurersrespawns.client.mixin;

import com.mojang.authlib.GameProfile;
import io.github.strikeless.adventurersrespawns.main.feature.SpawnPositionFeature;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.function.Function;

@Mixin(LocalPlayer.class)
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayer {
    @Unique
    private static Component originalScoreText = null;

    @Unique
    private static final Function<SpawnPositionFeature.SpawnChunkSearchStatus, Boolean> CHUNK_SEARCH_STATUS_TEXT_UPDATER = status -> {
        var deathScreenAccessor = getDeathScreenAccessor().orElse(null);
        if (deathScreenAccessor == null) return true; // Not in death screen anymore?

        var text = status.done() ? originalScoreText : (
                Component.literal("Searching for spawn structure within ")
                        .append(Component.literal(status.currentSearchExtent().toString()).withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(" chunks..."))
        );

        deathScreenAccessor.setDeathScore(text);
        return true;
    };

    static {
        SpawnPositionFeature.CHUNK_SEARCH_STATUS_CALLBACK.registerListener(CHUNK_SEARCH_STATUS_TEXT_UPDATER);
    }

    public ClientPlayerEntityMixin(ClientLevel level, GameProfile gameProfile) {
        super(level, gameProfile);
    }

    @Inject(at = @At("HEAD"), method = "respawn")
    private void respawn(CallbackInfo info) {
        var deathScreenAccessor = getDeathScreenAccessor().orElse(null);
        if (deathScreenAccessor == null) return;

        var respawnButton = getRespawnButton(deathScreenAccessor);
        respawnButton.setMessage(Component.literal("Preparing to spawn..."));

        originalScoreText = deathScreenAccessor.getDeathScore();
    }

    @Unique
    private static Optional<DeathScreenAccessor> getDeathScreenAccessor() {
        var mc = Minecraft.getInstance();

        //? minecraft: < 26.2 {
        /*var currentScreen = mc.screen;
        *///? } else {
        var currentScreen = mc.gui.screen();
        //? }

        if (currentScreen instanceof DeathScreen deathScreen) {
            var deathScreenAccessor = (DeathScreenAccessor) deathScreen;
            return Optional.of(deathScreenAccessor);
        } else {
            return Optional.empty();
        }
    }

    @Unique
    private static Button getRespawnButton(DeathScreenAccessor deathScreenAccessor) {
        return deathScreenAccessor.getExitButtons().getFirst();
    }
}
