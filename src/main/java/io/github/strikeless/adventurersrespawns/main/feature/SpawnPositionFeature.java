package io.github.strikeless.adventurersrespawns.main.feature;

import io.github.strikeless.adventurersrespawns.main.AdventurersRespawns;
import io.github.strikeless.adventurersrespawns.main.util.CallbackManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

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

    public static Optional<DimensionalBlockPos> getSpawnPosition(ServerPlayer player) {
        var effectiveDeathPosition = getEffectiveDeathDimensionalPosition(player)
                .orElse(null);
        if (effectiveDeathPosition == null) {
            AdventurersRespawns.getLogger().warn("Couldn't find effective death position. This should not happen besides for unaccounted edge-cases.");
            return Optional.empty();
        }

        var spawnStructureTypes = getSpawnStructureResourceKeysInLevel(effectiveDeathPosition.level());

        var spawnStructureBounds = findBoundsOfNearbyStructureOfType(effectiveDeathPosition, spawnStructureTypes)
                .orElse(null);
        if (spawnStructureBounds == null) {
            AdventurersRespawns.getLogger().warn(
                    "Didn't find a structure to spawn in. Make sure there are configured spawn structures that generate in {}",
                    effectiveDeathPosition.level().dimension().identifier().getPath()
            );
            return Optional.empty();
        }

        var favorableSpawnBlockPos = findFavorableSpawnBlockPosInStructureBounds(effectiveDeathPosition.level(), spawnStructureBounds)
                .orElse(null);
        if (favorableSpawnBlockPos == null) {
            AdventurersRespawns.getLogger().warn("Didn't find a favorable spawn position in the selected spawn structure.");
            return Optional.empty();
        }

        return Optional.of(
                new DimensionalBlockPos(
                        favorableSpawnBlockPos,
                        effectiveDeathPosition.level()
                )
        );
    }


    private static List<ResourceKey<Structure>> getSpawnStructureResourceKeysInLevel(ServerLevel level) {
        var config = AdventurersRespawns.getConfig();
        var structureRegistry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var structureSetRegistry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);

        var spawnStructureTypes = new ArrayList<Structure>();

        for (var structureIdentifierString : config.respawnStructureIdentifiers) {
            var structureIdentifier = Identifier.read(structureIdentifierString)
                    .result()
                    .orElse(null);

            if (structureIdentifier == null) {
                AdventurersRespawns.getLogger().error("Invalid structure identifier '{}'.", structureIdentifierString);
                continue;
            }

            var structure = structureRegistry.getValue(structureIdentifier);
            var structureSet = structureSetRegistry.getValue(structureIdentifier);

            if (structure != null) {
                spawnStructureTypes.add(structure);
            } else if (structureSet != null) {
                List<Structure> setStructures = structureSet.structures().stream()
                        .map(setEntry -> {
                            var setStructureEntry = setEntry.structure();

                            return setStructureEntry.unwrap()
                                    .map(
                                            structureRegistry::getValueOrThrow,
                                            setStructure -> setStructure
                                    );
                        })
                        .toList();

                AdventurersRespawns.getLogger().debug("Found {} structure types in set '{}'.", setStructures.size(), structureIdentifierString);
                spawnStructureTypes.addAll(setStructures);
            } else {
                AdventurersRespawns.getLogger().debug("Didn't find structure type '{}'. It may be registered for another dimension, in which case this is OK.", structureIdentifierString);
            }
        }

        AdventurersRespawns.getLogger().debug("Found {} structure types for respawning.", spawnStructureTypes.size());

        return spawnStructureTypes.stream()
                .map(structureType -> structureRegistry.getResourceKey(structureType).orElseThrow())
                .toList();
    }


    private static Optional<BoundingBox> findBoundsOfNearbyStructureOfType(DimensionalBlockPos dimensionalPos, List<ResourceKey<Structure>> structureKeys) {
        var config = AdventurersRespawns.getConfig();

        /*
         * First try searching in the fuzzy range, picking a random structure if many are found within the range.
         */
        var fuzzyRangeStructureBounds = findAllStructuresOfTypeWithinExtent(dimensionalPos, structureKeys, config.respawnStructureFuzzyRadiusChunks);
        if (!fuzzyRangeStructureBounds.isEmpty()) {
            // Found one or more structures within the fuzzy search range. Pick one of them.
            var randomIndex = RandomGenerator.getDefault().nextInt(0, fuzzyRangeStructureBounds.size());
            var randomStructure = fuzzyRangeStructureBounds.get(randomIndex);
            return Optional.of(randomStructure);
        }

        /*
         * No structures were found in the fuzzy range, search for any structure within the maximum search range.
         */
        AdventurersRespawns.getLogger().debug("No spawn structure found in fuzzy range, searching for closest...");
        return findClosestStructureOfType(dimensionalPos, structureKeys, config.respawnStructureSearchRadiusChunks);
    }

    private static List<BoundingBox> findAllStructuresOfTypeWithinExtent(DimensionalBlockPos dimensionalPos, List<ResourceKey<Structure>> structureKeys, int searchExtentChunks) {
        // This could probably be optimised to utilise StructurePlacementCalculator more directly
        // instead of loading/generating whole chunks. Something similar to how findClosestStructure does it.

        var foundStructureBounds = new ArrayList<BoundingBox>();

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

                var chunkFoundStructureBounds = getBoundsOfStructuresOfTypeWithinChunk(
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

    private static Optional<BoundingBox> findClosestStructureOfType(DimensionalBlockPos dimensionalPos, List<ResourceKey<Structure>> structureKeys, int searchExtentChunks) {
        var server = dimensionalPos.level().getServer();
        var levelChunkGenerator = dimensionalPos.level().getChunkSource().getGenerator();

        var structureRegistry = server.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var structureRegistryEntryList = HolderSet.direct(
                structureKeys.stream()
                        .map(structureKey -> structureRegistry.get(structureKey.identifier()).orElseThrow())
                        .toList()
        );

        CHUNK_SEARCH_STATUS_CALLBACK.dispatch(new SpawnChunkSearchStatus(false, searchExtentChunks, null));
        var blockPosStructureEntryPair = levelChunkGenerator.findNearestMapStructure(
                dimensionalPos.level(),
                structureRegistryEntryList,
                dimensionalPos.blockPos(),
                searchExtentChunks,
                false
        );
        CHUNK_SEARCH_STATUS_CALLBACK.dispatch(new SpawnChunkSearchStatus(true, searchExtentChunks, null));

        if (blockPosStructureEntryPair == null) {
            // No structure was found.
            return Optional.empty();
        }

        /*
         * A structure was found. Since the ChunkGenerator.locateStructure method only gives us the
         * starting location of the structure, we still need to find the bounds of the structure ourselves.
         */
        var structureBlockPos = blockPosStructureEntryPair.getFirst();
        var structureStartChunk = dimensionalPos.level().getChunk(structureBlockPos);

        // NOTE: Since we're passing all the searchable structure keys here, there might be an edge-case where
        //       we find the bounds of another respawn-fit structure than the one which was located, if many such structures start at the same chunk.
        //       This doesn't really matter at all but is still something to keep in mind. Could be fixed by only passing the key of the found structure.
        var chunkStructureBounds = getBoundsOfStructuresOfTypeWithinChunk(
                dimensionalPos.level(),
                structureStartChunk,
                structureKeys
        );
        if (chunkStructureBounds.isEmpty()) {
            AdventurersRespawns.getLogger().error("A structure was located at {}, but its bounds weren't resolved? This is likely a bug, please report it.", structureBlockPos);
            return Optional.empty();
        }

        // Found bounds of one or more matching structures within the chunk; Return any one of them. See above note for more clarity.
        return chunkStructureBounds.stream().findAny();
    }

    private static List<BoundingBox> getBoundsOfStructuresOfTypeWithinChunk(ServerLevel level, ChunkAccess chunk, List<ResourceKey<Structure>> structureKeys) {
        var chunkStructureStarts = chunk.getAllStarts();
        var chunkFitStructureBounds = new ArrayList<BoundingBox>();

        for (var structureStart : chunkStructureStarts.values()) {
            // NOTE: It's a Structure, not a StructureType, I just find this name more describing in this context.
            var structureType = structureStart.getStructure();
            var structureKey = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getResourceKey(structureType).orElseThrow();

            AdventurersRespawns.getLogger().debug("Found structure of type '{}'.", structureKey);

            if (structureKeys.contains(structureKey)) {
                AdventurersRespawns.getLogger().debug("Structure of type '{}' is fit for spawning.", structureType);

                var structureBounds = structureStart.getBoundingBox();
                chunkFitStructureBounds.add(structureBounds);
            }
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
