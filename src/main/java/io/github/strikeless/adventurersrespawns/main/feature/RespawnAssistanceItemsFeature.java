package io.github.strikeless.adventurersrespawns.main.feature;

import io.github.strikeless.adventurersrespawns.main.AdventurersRespawns;
import io.github.strikeless.adventurersrespawns.main.AdventurersRespawnsConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;

import java.util.Objects;

public class RespawnAssistanceItemsFeature {
    public static void giveDeathPositionMap(ServerPlayer player) {
        var config = AdventurersRespawns.getConfig();
        var server = Objects.requireNonNull(player.getServer());

        var deathGlobalPos = player.getLastDeathLocation().orElseThrow();
        var deathLevel = server.getLevel(deathGlobalPos.dimension());
        var deathPos = deathGlobalPos.pos();

        var deathPositionMapStack = createDecoratedMap(deathLevel, deathPos, MapDecorationTypes.RED_X, config.deathPositionMapName);
        player.addItem(deathPositionMapStack);
    }

    public static void giveSpawnpointMap(ServerPlayer player) {
        var config = AdventurersRespawns.getConfig();
        var server = Objects.requireNonNull(player.getServer());

        var spawnpointPos = player.getRespawnPosition();
        if (spawnpointPos == null) return;

        var spawnpointLevel = server.getLevel(player.getRespawnDimension());

        var spawnpointMapStack = createDecoratedMap(spawnpointLevel, spawnpointPos, MapDecorationTypes.TARGET_X, config.spawnpointMapName);
        player.addItem(spawnpointMapStack);
    }

    public static void giveCompass(ServerPlayer player, AdventurersRespawnsConfig.GivenCompassType compassType) {
        var compassItem = switch (compassType) {
            case None -> throw new IllegalArgumentException("giveCompass called with NoCompassGiven type");
            case NormalCompass -> Items.COMPASS;
            case RecoveryCompass -> Items.RECOVERY_COMPASS;
        };

        var compassStack = new ItemStack(compassItem);
        player.addItem(compassStack);
    }

    private static ItemStack createDecoratedMap(ServerLevel level, BlockPos markerPos, Holder<MapDecorationType> decorationType, String mapName) {
        var config = AdventurersRespawns.getConfig();

        var mapItemStack = MapItem.create(
                level,
                markerPos.getX(),
                markerPos.getZ(),
                config.mapScale,
                true, // Show decorations
                true // Unlimited tracking (show player marker at borders even when out of map bounds)
        );
        MapItem.renderBiomePreviewMap(level, mapItemStack);
        MapItemSavedData.addTargetDecoration(mapItemStack, markerPos, "marker", decorationType);
        mapItemStack.set(DataComponents.CUSTOM_NAME, Component.literal(mapName));

        return mapItemStack;
    }
}
