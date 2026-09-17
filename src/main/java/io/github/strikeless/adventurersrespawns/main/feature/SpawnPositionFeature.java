package io.github.strikeless.adventurersrespawns.main.feature;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import io.github.strikeless.adventurersrespawns.main.AdventurersRespawns;
import io.github.strikeless.adventurersrespawns.main.util.CallbackManager;
import io.github.strikeless.adventurersrespawns.main.util.iter.Iterators;
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class SpawnPositionFeature {
    public static final CallbackManager<SpawnChunkSearchStatus> CHUNK_SEARCH_STATUS_CALLBACK = new CallbackManager<>();

    public record SpawnChunkSearchStatus(
            boolean done,
            int searchExtentChunks,
            Integer searchedChunkCount
    ) {
        public int totalSearchChunkCount() {
            // It's correct, don't woorry aboout it. The extent logic should probably be changed to form a circle anyway.
            return (this.searchExtentChunks * 2) * (this.searchExtentChunks * 2) + (this.searchExtentChunks * 4) + 1;
        }
    }

    public record DimensionalBlockPos(
            BlockPos blockPos,
            ServerLevel level
    ) {
    }

    private record StructureBounds(
            ResourceKey<Structure> structureKey,
            BoundingBox bounds
    ) {
    }

    public static Optional<DimensionalBlockPos> getSpawnPosition(ServerPlayer player) {
        var effectiveDeathPosition = getEffectiveDeathDimensionalPosition(player)
                .orElse(null);
        if (effectiveDeathPosition == null) {
            AdventurersRespawns.getLogger().warn("Couldn't find effective death position. This should not happen besides for unaccounted edge-cases.");
            return Optional.empty();
        }

        var allowedSpawnStructureKeys = getSpawnStructureResourceKeysInLevel(effectiveDeathPosition.level());
        var spawnStructureCandidates = searchSpawnStructureCandidates(effectiveDeathPosition, allowedSpawnStructureKeys);

        /*
         * Try every spawn structure candidate in order,
         * falling back to the next candidate if one isn't usable.
         */
        while (spawnStructureCandidates.hasNext()) {
            var spawnStructureCandidate = Objects.requireNonNull(spawnStructureCandidates.next());
            AdventurersRespawns.getLogger().info("Found spawn structure candidate of type {} at {}", spawnStructureCandidate.structureKey().identifier(), spawnStructureCandidate.bounds());

            var favorableSpawnBlockPos = findFavorableSpawnBlockPosInStructureBounds(
                    effectiveDeathPosition.level(),
                    spawnStructureCandidate.bounds()
            ).orElse(null);
            if (favorableSpawnBlockPos == null) {
                AdventurersRespawns.getLogger().warn("Didn't find a favorable spawn position within structure {} at {}", spawnStructureCandidate.structureKey().identifier(), spawnStructureCandidate.bounds());
                continue;
            }

            var spawnDimensionalBlockPos = new DimensionalBlockPos(favorableSpawnBlockPos, effectiveDeathPosition.level());
            return Optional.of(spawnDimensionalBlockPos);
        }

        // Not a single spawn structure candidate was usable.
        return Optional.empty();
    }

    private static List<ResourceKey<Structure>> getSpawnStructureResourceKeysInLevel(ServerLevel level) {
        var config = AdventurersRespawns.getConfig();
        var levelStructureRegistry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);

        var spawnStructureResourceKeys = new ArrayList<ResourceKey<Structure>>();

        for (var structureInput : config.respawnStructures) {
            Either<ResourceKey<Structure>, TagKey<Structure>> structureResourceOrTagKey;
            try {
                structureResourceOrTagKey = ResourceOrTagKeyArgument.resourceOrTagKey(Registries.STRUCTURE)
                        .parse(new StringReader(structureInput))
                        .unwrap();
            } catch (CommandSyntaxException ex) {
                AdventurersRespawns.getLogger().error("Invalid respawn structure definition '{}': {}", ex.getInput(), ex.getMessage());
                continue;
            }

            var structureHolderSet = structureResourceOrTagKey.map(
                    // This is a direct structure resource key, like "minecraft:village_plains".
                    structureResourceKey -> levelStructureRegistry.get(structureResourceKey)
                            .map(HolderSet::direct)
                            .orElse(null),
                    // This is a tag, like "#minecraft:village".
                    structureTagKey -> levelStructureRegistry.get(structureTagKey)
                            .orElse(null)
            );
            if (structureHolderSet == null) {
                AdventurersRespawns.getLogger().debug("No structure resource or tag {} registered for level {}. It may be a typo or registered for another dimension.", structureInput, level.dimension().identifier());
                continue;
            }

            for (var structureHolder : structureHolderSet) {
                var structureHolderKey = structureHolder.unwrapKey().orElse(null);
                if (structureHolderKey == null) {
                    AdventurersRespawns.getLogger().warn("A structure type matches '{}', but it has no resource key and cannot be used.", structureInput);
                    continue;
                }

                spawnStructureResourceKeys.add(structureHolderKey);
            }
        }

        AdventurersRespawns.getLogger().debug("Found {} structure types for respawning.", spawnStructureResourceKeys.size());
        return spawnStructureResourceKeys;
    }

    private static Iterator<StructureBounds> searchSpawnStructureCandidates(DimensionalBlockPos dimensionalPos, List<ResourceKey<Structure>> allowedStructureKeys) {
        var config = AdventurersRespawns.getConfig();

        /*
         * Search all structure candidates within the configured fuzzy range,
         * shuffling them so that their preference order is randomized.
         */
        var fuzzyRangeSpawnStructureCandidates = findAllStructuresOfTypeWithinExtent(dimensionalPos, allowedStructureKeys, config.respawnStructureFuzzyRadiusChunks);
        Collections.shuffle(fuzzyRangeSpawnStructureCandidates);

        /*
         * Search for the closest structure candidate within the configured full search radius.
         * TODO: We should implement this as a lazy iterator that searches for the next closest structure candidate
         *       with each next() call, so that even if we can't spawn at the very closest structure, we could try
         *       the next closest structure and so on, instead of immediately falling back to vanilla behavior.
         */
        var fullRangeSpawnStructureCandidate = findNearestStructureOfTypeWithinExtent(dimensionalPos, allowedStructureKeys, config.respawnStructureSearchRadiusChunks);

        return Iterators.chain(
                fuzzyRangeSpawnStructureCandidates.iterator(),
                Iterators.onceOrNever(fullRangeSpawnStructureCandidate)
        );
    }

    private static List<StructureBounds> findAllStructuresOfTypeWithinExtent(DimensionalBlockPos dimensionalPos, List<ResourceKey<Structure>> structureKeys, int searchExtentChunks) {
        // This could probably be optimised to utilise StructurePlacementCalculator more directly
        // instead of loading/generating whole chunks. Something similar to how findClosestStructure does it.

        var foundStructureBounds = new ArrayList<StructureBounds>();

        var playerChunkX = SectionPos.blockToSectionCoord(dimensionalPos.blockPos().getX());
        var playerChunkZ = SectionPos.blockToSectionCoord(dimensionalPos.blockPos().getZ());

        var searchedChunkCount = 0;
        for (int chunkOffsetX = -searchExtentChunks; chunkOffsetX <= searchExtentChunks; ++chunkOffsetX) {
            for (int chunkOffsetZ = -searchExtentChunks; chunkOffsetZ <= searchExtentChunks; ++chunkOffsetZ) {
                searchedChunkCount += 1;
                CHUNK_SEARCH_STATUS_CALLBACK.dispatch(new SpawnChunkSearchStatus(false, searchExtentChunks, searchedChunkCount));

                var chunkX = playerChunkX + chunkOffsetX;
                var chunkZ = playerChunkZ + chunkOffsetZ;
                var chunk = dimensionalPos.level().getChunk(chunkX, chunkZ, ChunkStatus.STRUCTURE_STARTS);

                var chunkFoundStructureBounds = getStructureBoundsWithinChunk(
                        dimensionalPos.level(),
                        chunk,
                        structureKeys
                );
                foundStructureBounds.addAll(chunkFoundStructureBounds);
            }
        }
        CHUNK_SEARCH_STATUS_CALLBACK.dispatch(new SpawnChunkSearchStatus(true, searchExtentChunks, searchedChunkCount));

        AdventurersRespawns.getLogger().debug("Found {} structures.", foundStructureBounds.size());
        return foundStructureBounds;
    }

    private static Optional<StructureBounds> findNearestStructureOfTypeWithinExtent(DimensionalBlockPos dimensionalPos, List<ResourceKey<Structure>> structureKeys, int searchExtentChunks) {
        var server = dimensionalPos.level().getServer();
        var levelChunkGenerator = dimensionalPos.level().getChunkSource().getGenerator();

        var structureRegistry = server.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var structureRegistryEntryList = HolderSet.direct(
                structureKeys.stream()
                        .map(structureKey -> structureRegistry.get(structureKey.identifier()).orElseThrow())
                        .toList()
        );

        CHUNK_SEARCH_STATUS_CALLBACK.dispatch(new SpawnChunkSearchStatus(false, searchExtentChunks, null));
        var foundBlockPosStructureEntryPair = levelChunkGenerator.findNearestMapStructure(
                dimensionalPos.level(),
                structureRegistryEntryList,
                dimensionalPos.blockPos(),
                searchExtentChunks,
                false
        );
        CHUNK_SEARCH_STATUS_CALLBACK.dispatch(new SpawnChunkSearchStatus(true, searchExtentChunks, null));

        if (foundBlockPosStructureEntryPair == null) {
            // No structure was found.
            return Optional.empty();
        }

        /*
         * A structure was found. Since the ChunkGenerator.locateStructure method only gives us the
         * starting location of the structure, we still need to find the bounds of the structure ourselves.
         */
        var structureBlockPos = foundBlockPosStructureEntryPair.getFirst();
        var structureStartChunk = dimensionalPos.level().getChunk(structureBlockPos);

        // NOTE: Since we're passing all the searchable structure keys here, there can be an edge-case where
        //       we find the bounds of another respawn-fit structure than the one which was located, if many such structures start at the same chunk.
        //       This doesn't really matter at all but is still something to keep in mind. Could be fixed by only passing the key of the found structure.
        var chunkStructureBounds = getStructureBoundsWithinChunk(
                dimensionalPos.level(),
                structureStartChunk,
                structureKeys
        );
        if (chunkStructureBounds.isEmpty()) {
            AdventurersRespawns.getLogger().error("A structure was located at {}, but its bounds weren't resolved? This is a bug, please report it.", structureBlockPos);
            return Optional.empty();
        }

        // Found bounds of one or more matching structures within the chunk; Return any one of them. See above note for more clarity.
        return chunkStructureBounds.stream().findAny();
    }

    private static List<StructureBounds> getStructureBoundsWithinChunk(ServerLevel level, ChunkAccess chunk, List<ResourceKey<Structure>> allowedStructureKeys) {
        var chunkFitStructureBounds = new ArrayList<StructureBounds>();

        var chunkStructureStarts = chunk.getAllStarts();
        for (var structureStart : chunkStructureStarts.values()) {
            var structureImplementation = structureStart.getStructure();
            var structureBounds = structureStart.getBoundingBox();

            var structureKey = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getResourceKey(structureImplementation).orElse(null);
            if (structureKey == null) {
                AdventurersRespawns.getLogger().warn("Found a structure at {}, but it is not registered in the level's structure registry.", structureBounds);
                continue;
            }

            if (!allowedStructureKeys.contains(structureKey)) {
                AdventurersRespawns.getLogger().debug("Found unfit structure {}.", structureKey);
                continue;
            }
            AdventurersRespawns.getLogger().debug("Found fit structure {} at {}", structureKey, structureBounds);

            chunkFitStructureBounds.add(new StructureBounds(structureKey, structureBounds));
        }

        return chunkFitStructureBounds;
    }


    private static Optional<BlockPos> findFavorableSpawnBlockPosInStructureBounds(ServerLevel level, BoundingBox structureBounds) {
        var config = AdventurersRespawns.getConfig();

        var bedPositions = new ArrayList<BlockPos>();
        var fallbackPosition = Optional.<BlockPos>empty();

        // Dirty and flawed hack to fix some weird cases on cliffs where small structures are far above or below their StructureStart bounds.
        if (config.respawnStructureIgnoreVerticalBoundsFix) {
            structureBounds = new BoundingBox(
                    structureBounds.minX(),
                    level.getMinY(),
                    structureBounds.minZ(),
                    structureBounds.maxX(),
                    level.getMaxY(),
                    structureBounds.maxZ()
            );
        }

        /*
         * Collect all bed positions within the structure bounds and try to find a fallback position to spawn to if no beds are found.
         */
        for (int blockX = structureBounds.minX(); blockX <= structureBounds.maxX(); ++blockX) {
            for (int blockZ = structureBounds.minZ(); blockZ <= structureBounds.maxZ(); ++blockZ) {
                for (int blockY = structureBounds.minY(); blockY <= structureBounds.maxY(); ++blockY) {
                    var blockPos = new BlockPos(blockX, blockY, blockZ);
                    var blockState = level.getBlockState(blockPos);
                    var block = blockState.getBlock();

                    if (block instanceof BedBlock) {
                        bedPositions.add(blockPos);
                    } else {
                        var testForFallbackPosition = fallbackPosition.isEmpty() || blockY > fallbackPosition.get().getY();
                        if (testForFallbackPosition && isValidSpawnPosition(level, blockPos)) {
                            fallbackPosition = Optional.of(blockPos);
                        }
                    }
                }
            }
        }

        // Shuffle the bed position list to randomize bed if many were found, instead of always spawning at the same one.
        Collections.shuffle(bedPositions);

        for (var bedPos : bedPositions) {
            for (var blockOffsetX = -1; blockOffsetX <= 1; ++blockOffsetX) {
                for (var blockOffsetZ = -1; blockOffsetZ <= 1; ++blockOffsetZ) {
                    for (var blockOffsetY = 0; blockOffsetY <= 1; ++blockOffsetY) {
                        var bedNeighborPos = new BlockPos(
                                bedPos.getX() + blockOffsetX,
                                bedPos.getY() + blockOffsetY,
                                bedPos.getZ() + blockOffsetZ
                        );

                        if (isValidSpawnPosition(level, bedNeighborPos)) {
                            return Optional.of(bedNeighborPos);
                        }
                    }
                }
            }
        }

        // No beds to spawn next to were found, use the fallback position if one exists.
        return fallbackPosition;
    }

    private static boolean isValidSpawnPosition(Level level, BlockPos pos) {
        // Must be air
        var blockState = level.getBlockState(pos);
        if (!blockState.isAir()) return false;

        // Block below must have a full/"sturdy" top side
        var blockStateBelow = level.getBlockState(pos.below());
        if (!blockStateBelow.isFaceSturdy(level, pos, Direction.UP)) return false;

        // Block above must also be air, since the player is two blocks tall.
        var blockStateAbove = level.getBlockState(pos.above());
        return blockStateAbove.isAir();
    }

    private static Optional<DimensionalBlockPos> getEffectiveDeathDimensionalPosition(ServerPlayer player) {
        var config = AdventurersRespawns.getConfig();
        var playerLevel = player.level();
        var playerDeathPosition = Vec3.atBottomCenterOf(player.blockPosition()); // At this point in execution, blockPosition() seems like the right call.
        var server = playerLevel.getServer();

        if (config.respawnStructureAlwaysInOverworld && playerLevel.dimension() != Level.OVERWORLD) {
            /*
             * The player died in another dimension than the overworld, and the mod is configured to always spawn players in the overworld.
             * If the player died in the end or nether, translate their death position to overworld coordinates and use that as the effective death dimension.
             */
            var serverOverworldLevel = server.overworld();
            var dimensionTeleportationScale = DimensionType.getTeleportationScale(
                    playerLevel.dimensionType(),
                    serverOverworldLevel.dimensionType()
            );
            var dimensionTranslatedPosition = serverOverworldLevel.getWorldBorder().clampToBounds(
                    playerDeathPosition.x() * dimensionTeleportationScale,
                    playerDeathPosition.y(),
                    playerDeathPosition.z() * dimensionTeleportationScale
            );

            return Optional.of(
                    new DimensionalBlockPos(
                            dimensionTranslatedPosition,
                            serverOverworldLevel
                    )
            );
        }

        return Optional.of(
                new DimensionalBlockPos(
                        BlockPos.containing(playerDeathPosition),
                        playerLevel
                )
        );
    }
}
