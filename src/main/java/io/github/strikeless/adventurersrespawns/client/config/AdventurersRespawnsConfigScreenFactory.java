package io.github.strikeless.adventurersrespawns.client.config;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.impl.controller.EnumControllerBuilderImpl;
import dev.isxander.yacl3.impl.controller.IntegerSliderControllerBuilderImpl;
import dev.isxander.yacl3.impl.controller.LongSliderControllerBuilderImpl;
import dev.isxander.yacl3.impl.controller.TickBoxControllerBuilderImpl;
import io.github.strikeless.adventurersrespawns.main.AdventurersRespawns;
import io.github.strikeless.adventurersrespawns.main.AdventurersRespawnsConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class AdventurersRespawnsConfigScreenFactory {
    public static Screen getConfigScreen(Screen parentScreen) {
        var def = AdventurersRespawnsConfig.HANDLER.defaults();
        var config = AdventurersRespawns.getConfig();

        return YetAnotherConfigLib.createBuilder()
                .title(Component.literal("Adventurer's Respawns Configuration"))
                .category(
                        ConfigCategory.createBuilder()
                                .name(Component.literal("Adventurer's Respawns Configuration"))
                                .group(
                                        OptionGroup.createBuilder()
                                                .name(Component.literal("Respawn at structures"))
                                                .option(
                                                        Option.<Boolean>createBuilder()
                                                                .name(Component.literal("Enabled"))
                                                                .description(OptionDescription.of(
                                                                        Component.literal("Whether to respawn at a nearby fit structure (e.g. a village) instead of the vanilla spawnpoint."),
                                                                        Component.empty(),
                                                                        Component.literal("You may modify the structures the player can respawn at in the mod's configuration file.")
                                                                ))
                                                                .binding(def.respawnAtStructures, () -> config.respawnAtStructures, val -> config.respawnAtStructures = val)
                                                                .controller(TickBoxControllerBuilderImpl::new)
                                                                .build()
                                                )
                                                .option(
                                                        Option.<Integer>createBuilder()
                                                                .name(Component.literal("Structure fuzzy range (chunks)"))
                                                                .description(OptionDescription.of(
                                                                        Component.literal(
                                                                                "The extent in chunks from which a random fit structure will " +
                                                                                        "be selected for spawning at, instead of the very closest one."
                                                                        ),
                                                                        Component.empty(),
                                                                        Component.literal(
                                                                                "Higher values can make you travel a bit for your dropped items " +
                                                                                        "and may add variety to respawning if dying often in the same area."
                                                                        ),
                                                                        Component.empty(),
                                                                        Component.literal("Performance intensive upon respawning!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                                                                        Component.literal(
                                                                                "Higher values may hang the game for a while upon spawning due to chunk generation, " +
                                                                                        "since every chunk within this range from the death position will be at least partially generated before spawning."
                                                                        ).withStyle(ChatFormatting.RED),
                                                                        Component.literal(
                                                                                "Even a value of 32 chunks can take seconds on a good computer in non-pregenerated worlds, " +
                                                                                        "depending on your world generator."
                                                                        ).withStyle(ChatFormatting.RED)
                                                                ))
                                                                .binding(def.respawnStructureFuzzyRadiusChunks, () -> config.respawnStructureFuzzyRadiusChunks, val -> config.respawnStructureFuzzyRadiusChunks = val)
                                                                .controller(opt -> new IntegerSliderControllerBuilderImpl(opt).range(0, 128).step(1))
                                                                .build()
                                                )
                                                .option(
                                                        Option.<Boolean>createBuilder()
                                                                .name(
                                                                        Component.literal("FLAWED FIX: ")
                                                                                .append(Component.literal("Ignore structure vertical bounds"))
                                                                                .withStyle(ChatFormatting.GRAY)
                                                                )
                                                                .description(OptionDescription.of(
                                                                        Component.literal("Ignores the vertical bounds of structures when searching for a spawn position."),
                                                                        Component.empty(),
                                                                        Component.literal(
                                                                                "This is a very flawed and hacky fix for a bug where a structure at a cliff may be far above or below " +
                                                                                        "it's intended bounds, which can lead to the player spawning e.g. inside a cave below an igloo at a cliff."
                                                                        ),
                                                                        Component.literal(
                                                                                "In some cases, this option does more harm than good, possibly spawning you at the top of a tree, for example. " +
                                                                                        "In other cases, it lets you spawn at a structure which would otherwise have been wrongly ruled out. " +
                                                                                        "Experiment and see what works best for your use case."
                                                                        ).withStyle(ChatFormatting.RED)
                                                                ))
                                                                .binding(def.respawnStructureIgnoreVerticalBoundsFix, () -> config.respawnStructureIgnoreVerticalBoundsFix, val -> config.respawnStructureIgnoreVerticalBoundsFix = val)
                                                                .controller(TickBoxControllerBuilderImpl::new)
                                                                .build()
                                                )
                                                .build()
                                )
                                .group(
                                        OptionGroup.createBuilder()
                                                .name(Component.literal("Spawn condition"))
                                                .option(
                                                        Option.<Integer>createBuilder()
                                                                .name(Component.literal("Health"))
                                                                .description(OptionDescription.of(
                                                                        Component.literal("The health points to respawn the player with.")
                                                                ))
                                                                .binding(def.respawnHealth, () -> config.respawnHealth, val -> config.respawnHealth = val)
                                                                .controller(opt -> new IntegerSliderControllerBuilderImpl(opt).range(1, 20).step(1))
                                                                .build()
                                                )
                                                .option(
                                                        Option.<Integer>createBuilder()
                                                                .name(Component.literal("Food level"))
                                                                .description(OptionDescription.of(
                                                                        Component.literal("The food level to respawn the player with.")
                                                                ))
                                                                .binding(def.respawnFoodLevel, () -> config.respawnFoodLevel, val -> config.respawnFoodLevel = val)
                                                                .controller(opt -> new IntegerSliderControllerBuilderImpl(opt).range(1, 20).step(1))
                                                                .build()
                                                )
                                                .option(
                                                        Option.<AdventurersRespawnsConfig.DeathExperienceBehavior>createBuilder()
                                                                .name(Component.literal("Experience"))
                                                                .description(OptionDescription.of(
                                                                        Component.literal("What should happen to the player's experience upon death and respawning."),
                                                                        Component.empty(),
                                                                        Component.literal("Vanilla:").withStyle(ChatFormatting.BOLD),
                                                                        Component.literal("No modification."),
                                                                        Component.empty(),
                                                                        Component.literal("Keep:").withStyle(ChatFormatting.BOLD),
                                                                        Component.literal("Keep all experience."),
                                                                        Component.literal("(like with keepInventory on)"),
                                                                        Component.empty(),
                                                                        Component.literal("Drop:").withStyle(ChatFormatting.BOLD),
                                                                        Component.literal("Drop all experience."),
                                                                        Component.literal("(like with keepInventory off)"),
                                                                        Component.empty(),
                                                                        Component.literal("Destroy:").withStyle(ChatFormatting.BOLD),
                                                                        Component.literal("Permanently lose all experience upon death, not dropping nor keeping any of it.")
                                                                ))
                                                                .binding(def.deathExperienceBehavior, () -> config.deathExperienceBehavior, val -> config.deathExperienceBehavior = val)
                                                                .controller(opt -> new EnumControllerBuilderImpl<>(opt).enumClass(AdventurersRespawnsConfig.DeathExperienceBehavior.class))
                                                                .build()
                                                )
                                                .build()
                                )
                                .group(
                                        OptionGroup.createBuilder()
                                                .name(Component.literal("Assistance items"))
                                                .option(
                                                        Option.<Boolean>createBuilder()
                                                                .name(Component.literal("Give death position map"))
                                                                .description(OptionDescription.of(
                                                                        Component.literal("Whether to give the player a map with a marker to their death position upon respawning.")
                                                                ))
                                                                .binding(def.giveDeathPositionMap, () -> config.giveDeathPositionMap, val -> config.giveDeathPositionMap = val)
                                                                .controller(TickBoxControllerBuilderImpl::new)
                                                                .build()
                                                )
                                                .option(
                                                        Option.<Boolean>createBuilder()
                                                                .name(Component.literal("Give spawnpoint map"))
                                                                .description(OptionDescription.of(
                                                                        Component.literal("Whether to give the player a map with a marker to their spawnpoint upon respawning."),
                                                                        Component.empty(),
                                                                        Component.literal("Note that spawnpoint refers to your vanilla spawnpoint, and not the position you spawn at with structure respawns enabled.")
                                                                ))
                                                                .binding(def.giveSpawnpointMap, () -> config.giveSpawnpointMap, val -> config.giveSpawnpointMap = val)
                                                                .controller(TickBoxControllerBuilderImpl::new)
                                                                .build()
                                                )
                                                .option(
                                                        Option.<AdventurersRespawnsConfig.GivenCompassType>createBuilder()
                                                                .name(Component.literal("Give compass"))
                                                                .description(OptionDescription.of(
                                                                        Component.literal("Whether and what kind of a compass to give the player upon respawning"),
                                                                        Component.empty(),
                                                                        Component.literal("None:").withStyle(ChatFormatting.BOLD),
                                                                        Component.literal("No compass given."),
                                                                        Component.empty(),
                                                                        Component.literal("Normal compass:").withStyle(ChatFormatting.BOLD),
                                                                        Component.literal("Normal compass given, pointing towards the world spawnpoint in the vanilla game."),
                                                                        Component.empty(),
                                                                        Component.literal("Recovery compass:").withStyle(ChatFormatting.BOLD),
                                                                        Component.literal("Recovery compass given, pointing towards the death position of the player.")
                                                                ))
                                                                .binding(def.giveCompassType, () -> config.giveCompassType, val -> config.giveCompassType = val)
                                                                .controller(opt -> new EnumControllerBuilderImpl<>(opt).enumClass(AdventurersRespawnsConfig.GivenCompassType.class))
                                                                .build()
                                                )
                                                .build()
                                )
                                .group(
                                        OptionGroup.createBuilder()
                                                .name(Component.literal("Time skip on respawn"))
                                                .option(
                                                        Option.<Boolean>createBuilder()
                                                                .name(Component.literal("Enabled"))
                                                                .description(OptionDescription.of(
                                                                        Component.literal("Whether to enable skipping world time of day upon respawning.")
                                                                ))
                                                                .binding(def.timeSkipOnRespawn, () -> config.timeSkipOnRespawn, val -> config.timeSkipOnRespawn = val)
                                                                .controller(TickBoxControllerBuilderImpl::new)
                                                                .build()
                                                )
                                                .option(
                                                        Option.<Integer>createBuilder()
                                                                .name(Component.literal("Minimum time skip (ticks)"))
                                                                .description(OptionDescription.of(
                                                                        Component.literal("Minimum time in ticks to skip upon respawning."),
                                                                        Component.empty(),
                                                                        Component.literal("One full in-game day is 24000 ticks.")
                                                                ))
                                                                .binding(def.timeSkipMinTimeTicks, () -> config.timeSkipMinTimeTicks, val -> config.timeSkipMinTimeTicks = val)
                                                                .controller(opt -> new IntegerSliderControllerBuilderImpl(opt).range(0, 24000).step(100))
                                                                .build()
                                                )
                                                .option(
                                                        Option.<Integer>createBuilder()
                                                                .name(Component.literal("Maximum time skip (ticks)"))
                                                                .description(OptionDescription.of(
                                                                        Component.literal("Maximum time in ticks to skip upon respawning."),
                                                                        Component.empty(),
                                                                        Component.literal("One full in-game day is 24000 ticks.")
                                                                ))
                                                                .binding(def.timeSkipMaxTimeTicks, () -> config.timeSkipMaxTimeTicks, val -> config.timeSkipMaxTimeTicks = val)
                                                                .controller(opt -> new IntegerSliderControllerBuilderImpl(opt).range(0, 24000).step(100))
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .save(() -> AdventurersRespawnsConfig.HANDLER.save())
                .build()
                .generateScreen(parentScreen);
    }
}
