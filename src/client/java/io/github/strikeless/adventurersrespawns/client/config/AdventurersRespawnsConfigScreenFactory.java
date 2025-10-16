package io.github.strikeless.adventurersrespawns.client.config;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.impl.controller.EnumControllerBuilderImpl;
import dev.isxander.yacl3.impl.controller.IntegerSliderControllerBuilderImpl;
import dev.isxander.yacl3.impl.controller.LongSliderControllerBuilderImpl;
import dev.isxander.yacl3.impl.controller.TickBoxControllerBuilderImpl;
import io.github.strikeless.adventurersrespawns.AdventurersRespawns;
import io.github.strikeless.adventurersrespawns.AdventurersRespawnsConfig;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class AdventurersRespawnsConfigScreenFactory {
    public static Screen getConfigScreen(Screen parentScreen) {
        final var def = AdventurersRespawnsConfig.HANDLER.defaults();
        final var config = AdventurersRespawns.getConfig();

        return YetAnotherConfigLib.createBuilder()
                .title(Text.literal("Adventurer's Respawns Configuration"))
                .category(
                        ConfigCategory.createBuilder()
                                .name(Text.literal("Adventurer's Respawns Configuration"))
                                .group(
                                        OptionGroup.createBuilder()
                                                .name(Text.literal("Respawn at structures"))
                                                .option(
                                                        Option.<Boolean>createBuilder()
                                                                .name(Text.literal("Enabled"))
                                                                .description(OptionDescription.of(
                                                                        Text.literal("Whether to respawn at a nearby fit structure (e.g. a village) instead of the vanilla spawnpoint."),
                                                                        Text.empty(),
                                                                        Text.literal("You may modify the structures the player can respawn at in the mod's configuration file.")
                                                                ))
                                                                .binding(def.respawnAtStructure, () -> config.respawnAtStructure, val -> config.respawnAtStructure = val)
                                                                .controller(TickBoxControllerBuilderImpl::new)
                                                                .build()
                                                )
                                                .option(
                                                        Option.<Integer>createBuilder()
                                                                .name(Text.literal("Structure fuzzy range (chunks)"))
                                                                .description(OptionDescription.of(
                                                                        Text.literal(
                                                                                "The extent in chunks from which a random fit structure will " +
                                                                                        "be selected for spawning at, instead of the very closest one."
                                                                        ),
                                                                        Text.empty(),
                                                                        Text.literal(
                                                                                "Higher values can make you travel a bit for your dropped items " +
                                                                                        "and may add variety to respawning if dying often in the same area."
                                                                        ),
                                                                        Text.empty(),
                                                                        Text.literal("Performance intensive upon respawning!").formatted(Formatting.GOLD, Formatting.BOLD),
                                                                        Text.literal(
                                                                                "Higher values may hang the game for a while upon spawning due to chunk generation, " +
                                                                                        "since every chunk within this range from the death position will be at least partially generated before spawning."
                                                                        ).formatted(Formatting.RED),
                                                                        Text.literal(
                                                                                "Even a value of 32 chunks can take seconds on a good computer in non-pregenerated worlds."
                                                                        ).formatted(Formatting.RED)
                                                                ))
                                                                .binding(def.structureFuzzyExtentChunks, () -> config.structureFuzzyExtentChunks, val -> config.structureFuzzyExtentChunks = val)
                                                                .controller(opt -> new IntegerSliderControllerBuilderImpl(opt).range(0, 128).step(1))
                                                                .build()
                                                )
                                                .build()
                                )
                                .group(
                                        OptionGroup.createBuilder()
                                                .name(Text.literal("Spawn condition"))
                                                .option(
                                                        Option.<Integer>createBuilder()
                                                                .name(Text.literal("Health"))
                                                                .description(OptionDescription.of(
                                                                        Text.literal("The health points to respawn the player with.")
                                                                ))
                                                                .binding(def.respawnHealth, () -> config.respawnHealth, val -> config.respawnHealth = val)
                                                                .controller(opt -> new IntegerSliderControllerBuilderImpl(opt).range(1, 20).step(1))
                                                                .build()
                                                )
                                                .option(
                                                        Option.<Integer>createBuilder()
                                                                .name(Text.literal("Food level"))
                                                                .description(OptionDescription.of(
                                                                        Text.literal("The food level to respawn the player with.")
                                                                ))
                                                                .binding(def.respawnFoodLevel, () -> config.respawnFoodLevel, val -> config.respawnFoodLevel = val)
                                                                .controller(opt -> new IntegerSliderControllerBuilderImpl(opt).range(1, 20).step(1))
                                                                .build()
                                                )
                                                .option(
                                                        Option.<AdventurersRespawnsConfig.DeathExperienceBehavior>createBuilder()
                                                                .name(Text.literal("Experience"))
                                                                .description(OptionDescription.of(
                                                                        Text.literal("What should happen to the player's experience upon death and respawning."),
                                                                        Text.empty(),
                                                                        Text.literal("Vanilla:").formatted(Formatting.BOLD),
                                                                        Text.literal("No modification."),
                                                                        Text.empty(),
                                                                        Text.literal("Keep:").formatted(Formatting.BOLD),
                                                                        Text.literal("Keep all experience."),
                                                                        Text.literal("(like with keepInventory on)"),
                                                                        Text.empty(),
                                                                        Text.literal("Drop:").formatted(Formatting.BOLD),
                                                                        Text.literal("Drop all experience."),
                                                                        Text.literal("(like with keepInventory off)"),
                                                                        Text.empty(),
                                                                        Text.literal("Destroy:").formatted(Formatting.BOLD),
                                                                        Text.literal("Permanently lose all experience upon death, not dropping nor keeping any of it.")
                                                                ))
                                                                .binding(def.deathExperienceBehavior, () -> config.deathExperienceBehavior, val -> config.deathExperienceBehavior = val)
                                                                .controller(opt -> new EnumControllerBuilderImpl<>(opt).enumClass(AdventurersRespawnsConfig.DeathExperienceBehavior.class))
                                                                .build()
                                                )
                                                .build()
                                )
                                .group(
                                        OptionGroup.createBuilder()
                                                .name(Text.literal("Assistance maps"))
                                                .option(
                                                        Option.<Boolean>createBuilder()
                                                                .name(Text.literal("Give death position map"))
                                                                .description(OptionDescription.of(
                                                                        Text.literal("Whether to give the player a map with a marker to their death position upon respawning")
                                                                ))
                                                                .binding(def.giveDeathPositionMap, () -> config.giveDeathPositionMap, val -> config.giveDeathPositionMap = val)
                                                                .controller(TickBoxControllerBuilderImpl::new)
                                                                .build()
                                                )
                                                .option(
                                                        Option.<Boolean>createBuilder()
                                                                .name(Text.literal("Give spawnpoint map"))
                                                                .description(OptionDescription.of(
                                                                        Text.literal("Whether to give the player a map with a marker to their spawnpoint upon respawning")
                                                                ))
                                                                .binding(def.giveSpawnpointMap, () -> config.giveSpawnpointMap, val -> config.giveSpawnpointMap = val)
                                                                .controller(TickBoxControllerBuilderImpl::new)
                                                                .build()
                                                )
                                                .build()
                                )
                                .group(
                                        OptionGroup.createBuilder()
                                                .name(Text.literal("Time skip on respawn"))
                                                .option(
                                                        Option.<Boolean>createBuilder()
                                                                .name(Text.literal("Enabled"))
                                                                .description(OptionDescription.of(
                                                                        Text.literal("Whether to enable skipping world time of day upon respawning.")
                                                                ))
                                                                .binding(def.timeSkipOnRespawn, () -> config.timeSkipOnRespawn, val -> config.timeSkipOnRespawn = val)
                                                                .controller(TickBoxControllerBuilderImpl::new)
                                                                .build()
                                                )
                                                .option(
                                                        Option.<Long>createBuilder()
                                                                .name(Text.literal("Minimum time skip"))
                                                                .description(OptionDescription.of(
                                                                        Text.literal("Minimum time to skip upon respawning.")
                                                                ))
                                                                .binding(def.timeSkipMinTime, () -> config.timeSkipMinTime, val -> config.timeSkipMinTime = val)
                                                                .controller(opt -> new LongSliderControllerBuilderImpl(opt).range(0L, 48000L).step(100L))
                                                                .build()
                                                )
                                                .option(
                                                        Option.<Long>createBuilder()
                                                                .name(Text.literal("Maximum time skip"))
                                                                .description(OptionDescription.of(
                                                                        Text.literal("Maximum time to skip upon respawning.")
                                                                ))
                                                                .binding(def.timeSkipMaxTime, () -> config.timeSkipMaxTime, val -> config.timeSkipMaxTime = val)
                                                                .controller(opt -> new LongSliderControllerBuilderImpl(opt).range(0L, 24000L).step(100L))
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
