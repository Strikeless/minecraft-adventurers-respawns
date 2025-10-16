package io.github.strikeless.adventurersrespawns.client.mixin;

import com.mojang.authlib.GameProfile;
import io.github.strikeless.adventurersrespawns.feature.SpawnPositionFeature;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.function.Function;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends PlayerEntity {
    @Unique
    private static Text originalScoreText = null;

    @Unique
    private static final Function<SpawnPositionFeature.SpawnChunkSearchStatus, Boolean> CHUNK_SEARCH_STATUS_TEXT_UPDATER = status -> {
        final var deathScreenAccessor = getDeathScreenAccessor().orElse(null);
        if (deathScreenAccessor == null) return true; // Not in death screen anymore?

        final Text text;
        if (status.done()) {
            text = originalScoreText;
        } else {
            text = Text.literal("Searching for spawn structure ")
                    .append(Text.literal(status.currentSearchExtent().toString()).formatted(Formatting.YELLOW))
                    .append(Text.literal(" chunks away..."));
        }

        deathScreenAccessor.setScoreText(text);
        return true;
    };

    static {
        SpawnPositionFeature.CHUNK_SEARCH_STATUS_CALLBACK.registerListener(CHUNK_SEARCH_STATUS_TEXT_UPDATER);
    }

    public ClientPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Inject(at = @At("HEAD"), method = "requestRespawn")
    private void requestRespawn(CallbackInfo info) {
        final var deathScreenAccessor = getDeathScreenAccessor().orElse(null);
        if (deathScreenAccessor == null) return;

        final var respawnButton = getRespawnButton(deathScreenAccessor);
        respawnButton.setMessage(Text.literal("Preparing to spawn..."));

        originalScoreText = deathScreenAccessor.getScoreText();
    }

    @Unique
    private static Optional<DeathScreenAccessor> getDeathScreenAccessor() {
        final var mc = MinecraftClient.getInstance();

        if (mc.currentScreen instanceof DeathScreen deathScreen) {
            final var deathScreenAccessor = (DeathScreenAccessor) deathScreen;
            return Optional.of(deathScreenAccessor);
        } else {
            return Optional.empty();
        }
    }

    @Unique
    private static ButtonWidget getRespawnButton(DeathScreenAccessor deathScreenAccessor) {
        return deathScreenAccessor.getButtons().getFirst();
    }
}
