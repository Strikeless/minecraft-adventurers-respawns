package io.github.strikeless.adventurersrespawns.main.feature;

import io.github.strikeless.adventurersrespawns.main.AdventurersRespawns;
import io.github.strikeless.adventurersrespawns.main.util.CallbackManager;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.*;
import java.util.random.RandomGenerator;

public class SpawnPositionFeature {
    public static final CallbackManager<SpawnChunkSearchStatus> CHUNK_SEARCH_STATUS_CALLBACK = new CallbackManager<>();

    public record SpawnChunkSearchStatus(
            Integer currentSearchExtent, // null when done is set!
            boolean done
    ) {}

    public static Optional<BlockPos> getSpawnPosition(ServerPlayer player) {
        var level = player.level();

        var spawnStructureTypes = getSpawnStructureKeys(level);

        var spawnStructureBounds = findSpawnStructure(player, spawnStructureTypes).orElse(null);
        if (spawnStructureBounds == null) return Optional.empty();

        return findSpawnPosition(level, spawnStructureBounds);
    }


    private static List<ResourceKey<Structure>> getSpawnStructureKeys(ServerLevel level) {
        var config = AdventurersRespawns.getConfig();
        var structureRegistry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var structureSetRegistry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);

        var spawnStructureTypes = new ArrayList<Structure>();

        for (var structureIdentifierString : config.respawnStructureIdentifiers) {
            var structureIdentifier = Identifier.read(structureIdentifierString).result().orElse(null);
            if (structureIdentifier == null) {
                AdventurersRespawns.getLogger().error("Invalid structure identifier '{}'.", structureIdentifierString);
                continue;
            }

            var structure = structureRegistry.getValue(structureIdentifier);
            var structureSet = structureSetRegistry.getValue(structureIdentifier);

            if (structure != null) {
                spawnStructureTypes.add(structure);
                continue;
            }

            if (structureSet != null) {
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
                continue;
            }

            AdventurersRespawns.getLogger().warn("Didn't find structure type '{}'.", structureIdentifierString);
        }

        AdventurersRespawns.getLogger().debug("Found {} structure types for respawning.", spawnStructureTypes.size());

        return spawnStructureTypes.stream()
                .map(structureType -> structureRegistry.getResourceKey(structureType).orElseThrow())
                .toList();
    }


    private static Optional<BoundingBox> findSpawnStructure(ServerPlayer player, List<ResourceKey<Structure>> structureKeys) {
        var config = AdventurersRespawns.getConfig();

        /*
         * First try searching in the fuzzy range, picking a random structure if many are found within the range.
         */
        var fuzzyRangeStructureBounds = findStructuresWithinExtent(player, structureKeys, config.respawnStructureFuzzyRadiusChunks);

        if (!fuzzyRangeStructureBounds.isEmpty()) {
            var randomIndex = RandomGenerator.getDefault().nextInt(0, fuzzyRangeStructureBounds.size());
            var randomStructure = fuzzyRangeStructureBounds.get(randomIndex);
            return Optional.of(randomStructure);
        }

        /*
         * No structures were found in the fuzzy range, search for any structure within the maximum search range.
         */
        AdventurersRespawns.getLogger().debug("No spawn structure found in fuzzy range, searching for closest...");
        return findClosestStructureWithinExtent(player, structureKeys, config.respawnStructureSearchRadiusChunks);
    }

    private static List<BoundingBox> findStructuresWithinExtent(ServerPlayer player, List<ResourceKey<Structure>> structureKeys, int searchExtentChunks) {
        // This could probably be optimised to utilise StructurePlacementCalculator more directly
        // instead of loading/generating whole chunks. Something similar to how findClosestStructure does it.

        var level = player.level();

        var foundStructureBounds = new ArrayList<BoundingBox>();

        var playerChunkX = SectionPos.blockToSectionCoord(player.getBlockX());
        var playerChunkZ = SectionPos.blockToSectionCoord(player.getBlockZ());

        for (int chunkOffsetX = -searchExtentChunks; chunkOffsetX <= searchExtentChunks; ++chunkOffsetX) {
            var currentSearchExtent = Math.abs(chunkOffsetX);
            CHUNK_SEARCH_STATUS_CALLBACK.dispatch(new SpawnChunkSearchStatus(currentSearchExtent, false));

            for (int chunkOffsetZ = -searchExtentChunks; chunkOffsetZ <= searchExtentChunks; ++chunkOffsetZ) {
                var chunkX = playerChunkX + chunkOffsetX;
                var chunkZ = playerChunkZ + chunkOffsetZ;

                var chunk = level.getChunk(chunkX, chunkZ, ChunkStatus.STRUCTURE_STARTS);

                var chunkFoundStructureBounds = getChunkStructureBounds(level, chunk, structureKeys);
                foundStructureBounds.addAll(chunkFoundStructureBounds);
            }
        }

        CHUNK_SEARCH_STATUS_CALLBACK.dispatch(new SpawnChunkSearchStatus(null, true));
        AdventurersRespawns.getLogger().debug("Found {} structures.", foundStructureBounds.size());
        return foundStructureBounds;
    }

    private static Optional<BoundingBox> findClosestStructureWithinExtent(ServerPlayer player, List<ResourceKey<Structure>> structureKeys, int searchExtentChunks) {
        var level = player.level();
        var server = level.getServer();

        var structureRegistry = server.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var structureRegistryEntryList = HolderSet.direct(
                structureKeys.stream()
                        .map(structureKey -> structureRegistry.get(structureKey.identifier()).orElseThrow())
                        .toList()
        );

        CHUNK_SEARCH_STATUS_CALLBACK.dispatch(new SpawnChunkSearchStatus(searchExtentChunks, false));
        var blockPosStructureEntryPair = level.getChunkSource().getGenerator().findNearestMapStructure(
                level,
                structureRegistryEntryList,
                player.blockPosition(),
                searchExtentChunks,
                false
        );
        CHUNK_SEARCH_STATUS_CALLBACK.dispatch(new SpawnChunkSearchStatus(null, true));

        if (blockPosStructureEntryPair == null) {
            // No structure was found.
            return Optional.empty();
        }

        /*
         * A structure was found. Since the ChunkGenerator.locateStructure method only gives us the
         * starting location of the structure, we still need to find the bounds of the structure ourselves.
         */
        var structureBlockPos = blockPosStructureEntryPair.getFirst();

        var structureStartChunk = level.getChunk(structureBlockPos);
        // NOTE: Since we're passing all the searchable structure keys here, there might be an edge-case where
        //       we will find bounds of another respawn-fit structure than the one which was located, if many such structures start at the same chunk.
        //       This doesn't really matter at all but is still something to keep in mind. Could be fixed by only passing the key of the found structure.
        var chunkStructureBounds = getChunkStructureBounds(level, structureStartChunk, structureKeys);

        if (chunkStructureBounds.isEmpty()) {
            AdventurersRespawns.getLogger().error("A structure was located at {}, but structure bounds weren't resolved?", structureBlockPos);
            AdventurersRespawns.getLogger().error("This is most likely a bug in Adventurers Respawns!");
            return Optional.empty();
        }

        return chunkStructureBounds.stream().findAny();
    }

    private static List<BoundingBox> getChunkStructureBounds(ServerLevel level, ChunkAccess chunk, List<ResourceKey<Structure>> structureKeys) {
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


    private static Optional<BlockPos> findSpawnPosition(ServerLevel level, BoundingBox structureBounds) {
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
}
