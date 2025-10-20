package io.github.strikeless.adventurersrespawns.feature;

import io.github.strikeless.adventurersrespawns.AdventurersRespawns;
import io.github.strikeless.adventurersrespawns.AdventurersRespawnsConfig;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapDecorationType;
import net.minecraft.item.map.MapDecorationTypes;
import net.minecraft.item.map.MapState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;

public class RespawnAssistanceItemsFeature {
    public static void giveDeathPositionMap(ServerPlayerEntity player) {
        final var config = AdventurersRespawns.getConfig();
        final var server = Objects.requireNonNull(player.getServer());

        final var deathGlobalPos = player.getLastDeathPos().orElseThrow();
        final var deathWorld = server.getWorld(deathGlobalPos.dimension());
        final var deathPos = deathGlobalPos.pos();

        final var deathPositionMapStack = createDecoratedMap(deathWorld, deathPos, MapDecorationTypes.RED_X, config.deathPositionMapName);
        player.giveItemStack(deathPositionMapStack);
    }

    public static void giveSpawnpointMap(ServerPlayerEntity player) {
        final var config = AdventurersRespawns.getConfig();
        final var server = Objects.requireNonNull(player.getServer());

        final var spawnpointPos = player.getSpawnPointPosition();
        if (spawnpointPos == null) return;

        final var spawnpointWorld = server.getWorld(player.getSpawnPointDimension());

        final var spawnpointMapStack = createDecoratedMap(spawnpointWorld, spawnpointPos, MapDecorationTypes.TARGET_X, config.spawnpointMapName);
        player.giveItemStack(spawnpointMapStack);
    }

    public static void giveCompass(ServerPlayerEntity player, AdventurersRespawnsConfig.GivenCompassType compassType) {
        final var compassItem = switch (compassType) {
            case None -> throw new IllegalArgumentException("giveCompass called with NoCompassGiven type");
            case NormalCompass -> Items.COMPASS;
            case RecoveryCompass -> Items.RECOVERY_COMPASS;
        };

        final var compassStack = new ItemStack(compassItem);
        player.giveItemStack(compassStack);
    }

    private static ItemStack createDecoratedMap(ServerWorld world, BlockPos markerPos, RegistryEntry<MapDecorationType> decorationType, String mapName) {
        final var config = AdventurersRespawns.getConfig();

        final var mapItemStack = FilledMapItem.createMap(
                world,
                markerPos.getX(),
                markerPos.getZ(),
                config.mapScale,
                true, // Show decorations
                true // Unlimited tracking (show player marker at borders even when out of map bounds)
        );
        FilledMapItem.fillExplorationMap(world, mapItemStack);
        MapState.addDecorationsNbt(mapItemStack, markerPos, "marker", decorationType);
        mapItemStack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(mapName));

        return mapItemStack;
    }
}
