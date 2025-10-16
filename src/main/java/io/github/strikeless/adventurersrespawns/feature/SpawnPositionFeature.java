package io.github.strikeless.adventurersrespawns.feature;

import io.github.strikeless.adventurersrespawns.AdventurersRespawns;
import io.github.strikeless.adventurersrespawns.util.CallbackManager;
import net.minecraft.block.BedBlock;
import net.minecraft.registry.RegistryKey;
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
import java.util.random.RandomGenerator;

public class SpawnPositionFeature {
    public static final CallbackManager<SpawnChunkSearchStatus> CHUNK_SEARCH_STATUS_CALLBACK = new CallbackManager<>();

    public record SpawnChunkSearchStatus(
            Integer currentSearchExtent, // null when done is set!
            boolean done
    ) {}

    public static Optional<BlockPos> getSpawnPosition(ServerPlayerEntity player) {
        final var world = player.getServerWorld();

        final var spawnStructureTypes = getSpawnStructureKeys(world);

        final var spawnStructureBounds = findSpawnStructure(player, spawnStructureTypes).orElse(null);
        if (spawnStructureBounds == null) return Optional.empty();

        return findSpawnPosition(world, spawnStructureBounds);
    }


    private static List<RegistryKey<Structure>> getSpawnStructureKeys(ServerWorld world) {
        final var config = AdventurersRespawns.getConfig();
        final var structureRegistry = world.getRegistryManager().getOrThrow(RegistryKeys.STRUCTURE);
        final var structureSetRegistry = world.getRegistryManager().getOrThrow(RegistryKeys.STRUCTURE_SET);

        var spawnStructureTypes = new ArrayList<Structure>();

        for (final var structureIdentifierString : config.respawnStructureIdentifiers) {
            final var structureIdentifier = Identifier.validate(structureIdentifierString).result().orElse(null);
            if (structureIdentifier == null) {
                AdventurersRespawns.getLogger().error("Invalid structure identifier '{}'.", structureIdentifierString);
                continue;
            }

            final var structure = structureRegistry.get(structureIdentifier);
            final var structureSet = structureSetRegistry.get(structureIdentifier);

            if (structure != null) {
                spawnStructureTypes.add(structure);
                continue;
            }

            if (structureSet != null) {
                List<Structure> setStructures = structureSet.structures().stream()
                        .map(setEntry -> {
                            var setStructureEntry = setEntry.structure();

                            return setStructureEntry.getKeyOrValue()
                                    .map(
                                            structureRegistry::get,
                                            setStructure -> setStructure
                                    );
                        })
                        .filter(Objects::nonNull) // Null filtering for structures in the set not registered in the current world.
                        .toList();

                AdventurersRespawns.getLogger().debug("Found {} structure types in set '{}'.", setStructures.size(), structureIdentifierString);
                spawnStructureTypes.addAll(setStructures);
                continue;
            }

            AdventurersRespawns.getLogger().warn("Didn't find structure type '{}'.", structureIdentifierString);
        }

        AdventurersRespawns.getLogger().debug("Found {} structure types for respawning.", spawnStructureTypes.size());

        return spawnStructureTypes.stream()
                .map(structureType -> structureRegistry.getKey(structureType).orElseThrow())
                .toList();
    }


    private static Optional<BlockBox> findSpawnStructure(ServerPlayerEntity player, List<RegistryKey<Structure>> structureKeys) {
        final var config = AdventurersRespawns.getConfig();

        /*
         * First try searching in the fuzzy range, picking a random structure if many are found within the range.
         */
        final var fuzzyRangeStructureBounds = findAllStructures(player, structureKeys, config.respawnStructureFuzzyExtentChunks);

        if (!fuzzyRangeStructureBounds.isEmpty()) {
            final var randomIndex = RandomGenerator.getDefault().nextInt(0, fuzzyRangeStructureBounds.size());
            final var randomStructure = fuzzyRangeStructureBounds.get(randomIndex);
            return Optional.of(randomStructure);
        }

        /*
         * No structures were found in the fuzzy range, search for any structure within the maximum search range.
         */
        AdventurersRespawns.getLogger().debug("No spawn structure found in fuzzy range, searching for closest...");
        return findClosestStructure(player, structureKeys, config.respawnStructureFuzzyExtentChunks, config.respawnStructureMaxSearchExtentChunks);
    }

    private static List<BlockBox> findAllStructures(ServerPlayerEntity player, List<RegistryKey<Structure>> structureKeys, int searchExtentChunks) {
        final var world = player.getServerWorld();

        var foundStructureBounds = new ArrayList<BlockBox>();

        final var playerChunkX = ChunkSectionPos.getSectionCoord(player.getBlockX());
        final var playerChunkZ = ChunkSectionPos.getSectionCoord(player.getBlockZ());

        for (int chunkOffsetX = -searchExtentChunks; chunkOffsetX <= searchExtentChunks; ++chunkOffsetX) {
            var currentSearchExtent = Math.abs(chunkOffsetX);
            CHUNK_SEARCH_STATUS_CALLBACK.dispatch(new SpawnChunkSearchStatus(currentSearchExtent, false));

            for (int chunkOffsetZ = -searchExtentChunks; chunkOffsetZ <= searchExtentChunks; ++chunkOffsetZ) {
                final var chunkX = playerChunkX + chunkOffsetX;
                final var chunkZ = playerChunkZ + chunkOffsetZ;

                final var chunk = world.getChunk(chunkX, chunkZ, ChunkStatus.STRUCTURE_STARTS);

                final var chunkFoundStructureBounds = getChunkStructures(world, chunk, structureKeys);
                foundStructureBounds.addAll(chunkFoundStructureBounds);
            }
        }

        CHUNK_SEARCH_STATUS_CALLBACK.dispatch(new SpawnChunkSearchStatus(null, true));
        AdventurersRespawns.getLogger().debug("Found {} structures.", foundStructureBounds.size());
        return foundStructureBounds;
    }

    private static Optional<BlockBox> findClosestStructure(ServerPlayerEntity player, List<RegistryKey<Structure>> structureKeys, int minSearchExtentChunks, int maxSearchExtentChunks) {
        final var world = player.getServerWorld();

        final var playerChunkX = ChunkSectionPos.getSectionCoord(player.getBlockX());
        final var playerChunkZ = ChunkSectionPos.getSectionCoord(player.getBlockZ());

        var currentSearchExtent = minSearchExtentChunks;
        while (currentSearchExtent < maxSearchExtentChunks) {
            CHUNK_SEARCH_STATUS_CALLBACK.dispatch(new SpawnChunkSearchStatus(currentSearchExtent, false));

            for (var chunkOffsetX = -currentSearchExtent; chunkOffsetX <= currentSearchExtent; ++chunkOffsetX) {
                for (var chunkOffsetZ = -currentSearchExtent; chunkOffsetZ <= currentSearchExtent; ++chunkOffsetZ) {
                    // Ugly way to filter chunks that have been checked in previous extent iterations, no need to recheck those.
                    if (Math.abs(chunkOffsetX) != currentSearchExtent && Math.abs(chunkOffsetZ) != currentSearchExtent) {
                        continue;
                    }

                    final var chunkX = playerChunkX + chunkOffsetX;
                    final var chunkZ = playerChunkZ + chunkOffsetZ;

                    final var chunk = world.getChunk(chunkX, chunkZ, ChunkStatus.STRUCTURE_STARTS);
                    final var chunkFoundStructureBounds = getChunkStructures(world, chunk, structureKeys);

                    if (!chunkFoundStructureBounds.isEmpty()) {
                        CHUNK_SEARCH_STATUS_CALLBACK.dispatch(new SpawnChunkSearchStatus(null, true));

                        var firstFoundStructureBounds = chunkFoundStructureBounds.getFirst();
                        return Optional.of(firstFoundStructureBounds);
                    }
                }
            }
            currentSearchExtent += 1;
        }

        CHUNK_SEARCH_STATUS_CALLBACK.dispatch(new SpawnChunkSearchStatus(null, true));
        return Optional.empty();
    }

    private static List<BlockBox> getChunkStructures(ServerWorld world, Chunk chunk, List<RegistryKey<Structure>> structureKeys) {
        final var chunkStructureStarts = chunk.getStructureStarts();
        final var chunkFitStructureBounds = new ArrayList<BlockBox>();

        for (final var structureStart : chunkStructureStarts.values()) {
            // NOTE: It's a Structure, not a StructureType, I just find this name more describing in this context.
            final var structureType = structureStart.getStructure();
            final var structureKey = world.getRegistryManager().getOrThrow(RegistryKeys.STRUCTURE).getKey(structureType).orElseThrow();

            AdventurersRespawns.getLogger().debug("Found structure of type '{}'.", structureKey);

            if (structureKeys.contains(structureKey)) {
                AdventurersRespawns.getLogger().debug("Structure of type '{}' is fit for spawning.", structureType);

                final var structureBounds = structureStart.getBoundingBox();
                chunkFitStructureBounds.add(structureBounds);
            }
        }

        return chunkFitStructureBounds;
    }


    private static Optional<BlockPos> findSpawnPosition(World world, BlockBox structureBounds) {
        final var config = AdventurersRespawns.getConfig();

        var bedPositions = new ArrayList<BlockPos>();
        var fallbackPosition = Optional.<BlockPos>empty();

        // Dirty and flawed hack to fix some weird cases on cliffs where small structures are far above or below their StructureStart bounds.
        if (config.respawnStructureIgnoreVerticalBoundsFix) {
            structureBounds = new BlockBox(
                    structureBounds.getMinX(),
                    world.getBottomY(),
                    structureBounds.getMinZ(),
                    structureBounds.getMaxX(),
                    world.getTopYInclusive(),
                    structureBounds.getMaxZ()
            );
        }

        /*
         * Collect all bed positions within the structure bounds and try to find a fallback position to spawn to if no beds are found.
         */
        for (int blockX = structureBounds.getMinX(); blockX <= structureBounds.getMaxX(); ++blockX) {
            for (int blockZ = structureBounds.getMinZ(); blockZ <= structureBounds.getMaxZ(); ++blockZ) {
                for (int blockY = structureBounds.getMinY(); blockY <= structureBounds.getMaxY(); ++blockY) {
                    final var blockPos = new BlockPos(blockX, blockY, blockZ);
                    final var blockState = world.getBlockState(blockPos);
                    final var block = blockState.getBlock();

                    if (block instanceof BedBlock) {
                        bedPositions.add(blockPos);
                    } else {
                        final var testForFallbackPosition = fallbackPosition.isEmpty() || blockY > fallbackPosition.get().getY();
                        if (testForFallbackPosition && isValidSpawnPosition(world, blockPos)) {
                            fallbackPosition = Optional.of(blockPos);
                        }
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
}
