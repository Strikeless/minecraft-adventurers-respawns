package io.github.strikeless.adventurersrespawns.mixin;

import com.mojang.authlib.GameProfile;
import io.github.strikeless.adventurersrespawns.AdventurersRespawns;
import net.minecraft.block.BedBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.function.Function;
import java.util.random.RandomGenerator;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity {
    @Shadow
    public abstract ServerWorld getServerWorld();

    public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Inject(method = "getRespawnTarget", at = @At("RETURN"), cancellable = true)
    private void getRespawnTarget(CallbackInfoReturnable<TeleportTarget> info) {
        if (!AdventurersRespawns.getConfig().respawnAtStructure) {
            return;
        }

        // Search for a structure of a configured tag near the death position of the player.
        var respawnStructurePos = locateRespawnStructure().orElse(null);
        if (respawnStructurePos == null) {
            // TODO: Handle this situation maybe
            AdventurersRespawns.getLogger().info("it's all fucked");
            return;
        }

        // Add slight variation in the structure position so that the player won't always spawn next to the same bed in a village, for example.
        respawnStructurePos = respawnStructurePos.add(
                RandomGenerator.getDefault().nextInt(-16, 16),
                0,
                RandomGenerator.getDefault().nextInt(-16, 16)
        );

        /*
         * Found a structure to respawn at, now we need to come up with a specific position to spawn at.
         * First, let's try finding an unobstructed bed to spawn next to;
         * If one isn't found, try to fall back to the highest and horizontally nearest position with a block below.
         */
        final var finalRespawnStructurePos = respawnStructurePos;
        final var respawnPos = findNearbyBedPos(respawnStructurePos)
                .or(() -> findFallbackSpawnPos(finalRespawnStructurePos))
                .orElse(null);

        if (respawnPos == null) {
            // Not much we can do here.
            AdventurersRespawns.getLogger().warn("No nearby bed or fallback spawn position found for structure");
            return;
        }

        info.setReturnValue(new TeleportTarget(
                this.getServerWorld(),
                respawnPos.toBottomCenterPos(),
                Vec3d.ZERO, // Velocity
                RandomGenerator.getDefault().nextFloat(0.0F, 360.0F), // Yaw
                0.0F, // Pitch
                false, // Missing respawn
                false, // As passenger
                Set.of(), // Relatives
                TeleportTarget.NO_OP // Post teleport transition
        ));
    }

    @Unique
    private Optional<BlockPos> locateRespawnStructure() {
        final var structureTags = AdventurersRespawns.getConfig().structureTags;
        if (structureTags.isEmpty()) {
            AdventurersRespawns.getLogger().warn("No structure types defined!");
            return Optional.empty();
        }

        /*
         * Search for structures of each configured tag, storing the position of the nearest structure of each tag.
         */
        // Search for structures of all configured tags
        final var deathPos = this.getBlockPos();
        final var structurePositions = new ArrayList<BlockPos>();
        for (final var structureTagIdentifier : structureTags) {
            var locatedStructurePos = locateStructureOfTag(deathPos, structureTagIdentifier);
            locatedStructurePos.ifPresent(structurePositions::add);
        }

        if (structurePositions.isEmpty()) {
            // No structures of any configured tag were found within the configured location radius.
            return Optional.empty();
        }

        /*
         * Select a random located structure within the closest "fuzzy distance group"
         */
        final var structurePositionComparator = Comparator
                .comparingInt((BlockPos pos) -> {
                    var accurateBlockDistance = (int) pos.getSquaredDistance(deathPos);

                    System.out.println("accurateBlockDistance " + deathPos + " -> " + pos + " = " + accurateBlockDistance);
                    System.out.println("    fuzzy " + (accurateBlockDistance / AdventurersRespawns.getConfig().structureClosestBlockFuzziness));

                    // Add fuzziness into the distance calculation by grouping nearby-distances as the same distance in the comparison.
                    return accurateBlockDistance / AdventurersRespawns.getConfig().structureClosestBlockFuzziness;
                })
                .thenComparing(_pos -> RandomGenerator.getDefault().nextInt());

        // SAFETY: get should never fail since we check and return on structurePositions.isEmpty() before sorting.
        return structurePositions.stream().min(structurePositionComparator);
    }

    @Unique
    private Optional<BlockPos> locateStructureOfTag(BlockPos pos, Identifier tagIdentifier) {
        final var world = this.getServerWorld();
        final var worldStructureRegistry = world.getRegistryManager().getOrThrow(RegistryKeys.STRUCTURE);

        final var structureTag = TagKey.of(RegistryKeys.STRUCTURE, tagIdentifier);

        if (worldStructureRegistry.getOptional(structureTag).isEmpty()) {
            AdventurersRespawns.getLogger().warn("Structure tag '{}' isn't registered", tagIdentifier);
            return Optional.empty();
        }

        AdventurersRespawns.getLogger().debug("Locating structures with tag '{}'", tagIdentifier);
        final var locatedStructurePos = world.locateStructure(
                structureTag,
                pos,
                AdventurersRespawns.getConfig().closestStructureFuzzyRangeChunks,
                false
        );

        return Optional.ofNullable(locatedStructurePos);
    }


    @Unique
    private Optional<BlockPos> findNearbyBedPos(BlockPos structurePos) {
        final var world = getServerWorld();

        return findBlockPos(structurePos, 256, world.getTopYInclusive(), world.getBottomY(), bedPos -> {
            final var blockState = world.getBlockState(bedPos);

            if (!(blockState.getBlock() instanceof BedBlock)) {
                return Optional.empty();
            }

            return findBlockPos(bedPos, 2, bedPos.getY(), bedPos.getY(), airPos -> {
                final var airBlockState = world.getBlockState(airPos);
                final var aboveAirBlockState = world.getBlockState(airPos.add(0, 1, 0));

                if (airBlockState.isAir() && aboveAirBlockState.isAir()) {
                    return Optional.of(airPos);
                } else {
                    return Optional.empty();
                }
            });
        });
    }

    @Unique
    private Optional<BlockPos> findFallbackSpawnPos(BlockPos structurePos) {
        final var world = getServerWorld();

        return findBlockPos(structurePos, 16, world.getTopYInclusive(), world.getBottomY(), blockPos -> {
            if (world.getBlockState(blockPos).isSolid()) {
                return Optional.of(blockPos.add(0, 1, 0));
            } else {
                return Optional.empty();
            }
        });
    }

    @Unique
    private Optional<BlockPos> findBlockPos(BlockPos sourcePos, int maxHorizontalExtent, int topY, int bottomY, Function<BlockPos, Optional<BlockPos>> matcherFunction) {
        var currentHorizontalExtent = 0;
        while (currentHorizontalExtent <= maxHorizontalExtent) {
            for (int offsetX : new int[] { -currentHorizontalExtent, currentHorizontalExtent }) {
                for (int offsetZ = -currentHorizontalExtent; offsetZ <= currentHorizontalExtent; ++offsetZ) {
                    // No need to recheck positions "inside" of the extent, as these have already been checked in a previous iteration.
                    // This whole method is probably stupidly inefficient but let's not make it even more so by doing exponentially more useless checks.
                    if (Math.abs(offsetX) != currentHorizontalExtent && Math.abs(offsetZ) != currentHorizontalExtent) {
                        continue;
                    }

                    for (int y = topY; y >= bottomY; --y) {
                        final var currentPos = sourcePos.add(offsetX, 0, offsetZ).withY(y);
                        final var matchedPos = matcherFunction.apply(currentPos);
                        if (matchedPos.isPresent()) return matchedPos;
                    }
                }
            }

            currentHorizontalExtent += 1;
        }

        return Optional.empty();
    }
}
