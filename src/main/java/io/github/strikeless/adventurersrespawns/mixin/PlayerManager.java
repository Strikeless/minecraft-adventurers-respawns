package io.github.strikeless.adventurersrespawns.mixin;

import io.github.strikeless.adventurersrespawns.AdventurersRespawns;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapDecorationType;
import net.minecraft.item.map.MapDecorationTypes;
import net.minecraft.item.map.MapState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.GlobalPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;
import java.util.random.RandomGenerator;

@Mixin(net.minecraft.server.PlayerManager.class)
public abstract class PlayerManager {
    @Shadow
    public abstract MinecraftServer getServer();

    @Inject(method = "respawnPlayer", at = @At("TAIL"))
    private void respawnPlayer(CallbackInfoReturnable<ServerPlayerEntity> info) {
        var respawnedPlayer = info.getReturnValue();
        setHealthAndFoodLevel(respawnedPlayer);
        giveMaps(respawnedPlayer);
    }

    @Unique
    private void setHealthAndFoodLevel(ServerPlayerEntity player) {
        player.setHealth(AdventurersRespawns.getConfig().respawnHealth);

        var hungerManager = player.getHungerManager();
        hungerManager.setFoodLevel(AdventurersRespawns.getConfig().respawnFoodLevel);
        hungerManager.setSaturationLevel(0.0F);
    }

    @Unique
    private void giveMaps(ServerPlayerEntity player) {
        var config = AdventurersRespawns.getConfig();

        if (config.giveDeathPositionMap) {
            var deathGlobalPos = player.getLastDeathPos().orElseThrow();
            var deathPositionMapStack = createDecoratedMap(deathGlobalPos, MapDecorationTypes.RED_X, config.deathPositionMapName);
            player.giveItemStack(deathPositionMapStack);
        }

        if (config.giveSpawnpointMap) {
            var spawnpointPos = player.getSpawnPointPosition();

            if (spawnpointPos != null) {
                var spawnpointGlobalPos = new GlobalPos(
                        player.getSpawnPointDimension(),
                        spawnpointPos
                );
                var spawnpointMapStack = createDecoratedMap(spawnpointGlobalPos, MapDecorationTypes.BLUE_MARKER, config.spawnpointMapName);
                player.giveItemStack(spawnpointMapStack);
            }
        }
    }

    @Unique
    private ItemStack createDecoratedMap(GlobalPos globalPos, RegistryEntry<MapDecorationType> decorationType, String mapName) {
        var server = this.getServer();

        var world = Objects.requireNonNull(server.getWorld(globalPos.dimension()));
        var markerPos = globalPos.pos();

        var mapScale = AdventurersRespawns.getConfig().mapScale;
        var maxMapCenterOffset = getMapScaleExtent(mapScale) / 8;
        var mapCenterX = markerPos.getX() + maxMapCenterOffset;//RandomGenerator.getDefault().nextInt(-maxMapCenterOffset, maxMapCenterOffset);
        var mapCenterZ = markerPos.getZ() + maxMapCenterOffset;//RandomGenerator.getDefault().nextInt(-maxMapCenterOffset, maxMapCenterOffset);

        var mapItemStack = FilledMapItem.createMap(
                world,
                mapCenterX,
                mapCenterZ,
                mapScale,
                true, // Show decorations
                true // Unlimited tracking (show player marker at borders even when out of map bounds)
        );
        FilledMapItem.fillExplorationMap(world, mapItemStack);
        MapState.addDecorationsNbt(mapItemStack, markerPos, "marker", decorationType);
        mapItemStack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(mapName));

        return mapItemStack;
    }

    @Unique
    private int getMapScaleExtent(byte scale) {
        return 128 * (1 << scale);
    }
}
