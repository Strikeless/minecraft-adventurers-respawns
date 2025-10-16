package io.github.strikeless.adventurersrespawns.feature;

import io.github.strikeless.adventurersrespawns.AdventurersRespawns;
import net.minecraft.block.BedBlock;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.structure.Structure;

import java.util.*;
import java.util.function.Function;
import java.util.random.RandomGenerator;

public class SpawnPositionFeature {
    private static final List<Function<Integer, Boolean>> CURRENT_CHUNK_SEARCH_EXTENT_LISTENERS = new ArrayList<>();

    public static Optional<BlockPos> getSpawnPosition(ServerPlayerEntity player) {
        final var world = player.getServerWorld();

        final var spawnStructureTypes = getSpawnStructureTypes(world);

        final var spawnStructureBounds = findSpawnStructure(player, spawnStructureTypes).orElse(null);
        if (spawnStructureBounds == null) return Optional.empty();

        return findSpawnPosition(world, spawnStructureBounds);
    }


    private static List<Structure> getSpawnStructureTypes(ServerWorld world) {
        final var config = AdventurersRespawns.getConfig();
        var spawnStructureTypes = new ArrayList<Structure>();

        final var worldStructureRegistry = world.getRegistryManager().getOrThrow(RegistryKeys.STRUCTURE);
        final var worldStructureSetRegistry = world.getRegistryManager().getOrThrow(RegistryKeys.STRUCTURE_SET);

        for (final var structureIdentifierString : config.structureIdentifiers) {
            final var structureIdentifier = Identifier.validate(structureIdentifierString).result().orElse(null);
            if (structureIdentifier == null) {
                AdventurersRespawns.getLogger().error("Invalid structure identifier '{}'.", structureIdentifierString);
                continue;
            }

            final var structure = worldStructureRegistry.get(structureIdentifier);
            final var structureSet = worldStructureSetRegistry.get(structureIdentifier);

            if (structure != null) {
                spawnStructureTypes.add(structure);
            }

            if (structureSet != null) {
                List<Structure> setStructures = structureSet.structures().stream()
                        .map(setEntry -> {
                            var setStructureEntry = setEntry.structure();

                            return setStructureEntry.getKeyOrValue()
                                    .map(
                                            worldStructureRegistry::get,
                                            setStructure -> setStructure
                                    );
                        })
                        .filter(Objects::nonNull) // Null filtering for structures in the set not registered in the current world.
                        .toList();

                AdventurersRespawns.getLogger().debug("Found {} structure types in set '{}'.", setStructures.size(), structureIdentifierString);
                spawnStructureTypes.addAll(setStructures);
            }
        }

        AdventurersRespawns.getLogger().debug("Found {} structure types for respawning.", spawnStructureTypes.size());
        return spawnStructureTypes;
    }


    private static Optional<BlockBox> findSpawnStructure(ServerPlayerEntity player, List<Structure> structureTypes) {
        final var config = AdventurersRespawns.getConfig();

        /*
         * First try searching in the fuzzy range, picking a random structure if many are found within the range.
         */
        final var fuzzyRangeStructureBounds = findAllStructures(player, structureTypes, config.structureFuzzyExtentChunks);

        if (!fuzzyRangeStructureBounds.isEmpty()) {
            final var randomIndex = RandomGenerator.getDefault().nextInt(0, fuzzyRangeStructureBounds.size());
            final var randomStructure = fuzzyRangeStructureBounds.get(randomIndex);
            return Optional.of(randomStructure);
        }

        /*
         * No structures were found in the fuzzy range, search for any structure within the maximum search range.
         */
        AdventurersRespawns.getLogger().debug("No spawn structure found in fuzzy range, searching for closest...");
        return findClosestStructure(player, structureTypes, config.structureFuzzyExtentChunks, config.structureMaxSearchExtentChunks);
    }

    private static List<BlockBox> findAllStructures(ServerPlayerEntity player, List<Structure> structureTypes, int searchExtentChunks) {
        final var world = player.getServerWorld();

        var foundStructureBounds = new ArrayList<BlockBox>();

        final var playerChunkX = ChunkSectionPos.getSectionCoord(player.getBlockX());
        final var playerChunkZ = ChunkSectionPos.getSectionCoord(player.getBlockZ());

        for (int chunkOffsetX = -searchExtentChunks; chunkOffsetX <= searchExtentChunks; ++chunkOffsetX) {
            announceCurrentChunkSearchExtent(Math.abs(chunkOffsetX));

            for (int chunkOffsetZ = -searchExtentChunks; chunkOffsetZ <= searchExtentChunks; ++chunkOffsetZ) {
                final var chunkX = playerChunkX + chunkOffsetX;
                final var chunkZ = playerChunkZ + chunkOffsetZ;

                final var chunk = world.getChunk(chunkX, chunkZ, ChunkStatus.STRUCTURE_STARTS);

                final var chunkStructure = getChunkStructure(chunk, structureTypes);
                chunkStructure.ifPresent(foundStructureBounds::add);
            }
        }

        AdventurersRespawns.getLogger().debug("Found {} structures.", foundStructureBounds.size());
        return foundStructureBounds;
    }

    private static Optional<BlockBox> findClosestStructure(ServerPlayerEntity player, List<Structure> structureTypes, int minSearchExtentChunks, int maxSearchExtentChunks) {
        final var world = player.getServerWorld();

        final var playerChunkX = ChunkSectionPos.getSectionCoord(player.getBlockX());
        final var playerChunkZ = ChunkSectionPos.getSectionCoord(player.getBlockZ());

        var currentSearchExtent = minSearchExtentChunks;
        while (currentSearchExtent < maxSearchExtentChunks) {
            announceCurrentChunkSearchExtent(currentSearchExtent);

            for (var chunkOffsetX = -currentSearchExtent; chunkOffsetX <= currentSearchExtent; ++chunkOffsetX) {
                for (var chunkOffsetZ = -currentSearchExtent; chunkOffsetZ <= currentSearchExtent; ++chunkOffsetZ) {
                    // Ugly way to filter chunks that have been checked in previous extent iterations, no need to recheck those.
                    if (Math.abs(chunkOffsetX) != currentSearchExtent && Math.abs(chunkOffsetZ) != currentSearchExtent) {
                        continue;
                    }

                    final var chunkX = playerChunkX + chunkOffsetX;
                    final var chunkZ = playerChunkZ + chunkOffsetZ;

                    final var chunk = world.getChunk(chunkX, chunkZ, ChunkStatus.STRUCTURE_STARTS);
                    final var chunkStructure = getChunkStructure(chunk, structureTypes);

                    if (chunkStructure.isPresent()) {
                        return chunkStructure;
                    }
                }
            }
            currentSearchExtent += 1;
        }

        return Optional.empty();
    }

    private static Optional<BlockBox> getChunkStructure(Chunk chunk, List<Structure> structureTypes) {
        final var chunkStructureStarts = chunk.getStructureStarts();

        for (final var structureStartEntry : chunkStructureStarts.entrySet()) {
            // NOTE: It's a Structure, not a StructureType, I just find this name more describing in this context.
            final var structureType = structureStartEntry.getKey();
            final var structureStart = structureStartEntry.getValue();

            if (structureTypes.contains(structureType)) {
                AdventurersRespawns.getLogger().debug("Found structure '{}'.", structureType);

                final var structureBounds = structureStart.getBoundingBox();
                return Optional.of(structureBounds);
            }
        }

        return Optional.empty();
    }


    private static Optional<BlockPos> findSpawnPosition(World world, BlockBox structureBounds) {
        var bedPositions = new ArrayList<BlockPos>();
        var fallbackPosition = Optional.<BlockPos>empty();

        /*
         * Collect all bed positions within the structure bounds and try to find a fallback position to spawn to if no beds are found.
         */
        for (int blockX = structureBounds.getMinX(); blockX <= structureBounds.getMaxX(); ++blockX) {
            for (int blockZ = structureBounds.getMinZ(); blockZ <= structureBounds.getMaxZ(); ++blockZ) {
                for (int blockY = structureBounds.getMinY(); blockY <= structureBounds.getMaxY(); ++blockY) {
                    final var blockPos = new BlockPos(blockX, blockY, blockZ);
                    final var blockState = world.getBlockState(blockPos);
                    final var block = blockState.getBlock();

                    if (block instanceof BedBlock && !blockState.get(BedBlock.OCCUPIED)) {
                        bedPositions.add(blockPos);
                    } else if (fallbackPosition.isEmpty() && isValidSpawnPosition(world, blockPos)) {
                        fallbackPosition = Optional.of(blockPos);
                    }
                }
            }
        }

        // Shuffle the bed position list to randomize bed if many were found, instead of always spawning at the same one.
        Collections.shuffle(bedPositions);

        for (final var bedPos : bedPositions) {
            for (var blockOffsetX = -1; blockOffsetX <= 1; ++blockOffsetX) {
                for (var blockOffsetZ = -1; blockOffsetZ <= 1; ++blockOffsetZ) {
                    for (var blockOffsetY = 0; blockOffsetY <= 1; ++blockOffsetY) {
                        final var bedNeighborPos = new BlockPos(
                                bedPos.getX() + blockOffsetX,
                                bedPos.getY() + blockOffsetY,
                                bedPos.getZ() + blockOffsetZ
                        );

                        if (isValidSpawnPosition(world, bedNeighborPos)) {
                            return Optional.of(bedNeighborPos);
                        }
                    }
                }
            }
        }

        // No beds to spawn next to were found, use the fallback position if one exists.
        return fallbackPosition;
    }

    private static boolean isValidSpawnPosition(World world, BlockPos pos) {
        // Must be air
        final var blockState = world.getBlockState(pos);
        if (!blockState.isAir()) return false;

        // Block below must have a full top side
        final var blockStateBelow = world.getBlockState(pos.down());
        if (!blockStateBelow.isSideSolidFullSquare(world, pos, Direction.UP)) return false;

        // Block above must also be air, since the player is two blocks tall.
        final var blockStateAbove = world.getBlockState(pos.up());
        return blockStateAbove.isAir();
    }

    public static void registerCurrentChunkSearchExtentListener(Function<Integer, Boolean> listener) {
        CURRENT_CHUNK_SEARCH_EXTENT_LISTENERS.add(listener);
    }

    public static void unregisterCurrentChunkSearchExtentListener(Function<Integer, Boolean> listener) {
        CURRENT_CHUNK_SEARCH_EXTENT_LISTENERS.remove(listener);
    }

    private static void announceCurrentChunkSearchExtent(int extent) {
        CURRENT_CHUNK_SEARCH_EXTENT_LISTENERS.removeIf(listener -> !listener.apply(extent));
    }
}
