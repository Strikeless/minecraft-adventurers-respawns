package io.github.strikeless.adventurersrespawns.client.config;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.impl.controller.IntegerSliderControllerBuilderImpl;
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
                                                                        Text.literal(""),
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
                                                                        Text.literal(""),
                                                                        Text.literal(
                                                                                "Higher values can make you travel a bit for your dropped items " +
                                                                                        "and may add variety to respawning if dying often in the same area."
                                                                        ),
                                                                        Text.literal(""),
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
                                                                        Text.literal("How many health points to respawn the player with")
                                                                ))
                                                                .binding(def.respawnHealth, () -> config.respawnHealth, val -> config.respawnHealth = val)
                                                                .controller(opt -> new IntegerSliderControllerBuilderImpl(opt).range(1, 20).step(1))
                                                                .build()
                                                )
                                                .option(
                                                        Option.<Integer>createBuilder()
                                                                .name(Text.literal("Food level"))
                                                                .description(OptionDescription.of(
                                                                        Text.literal("What food level to respawn the player with")
                                                                ))
                                                                .binding(def.respawnFoodLevel, () -> config.respawnFoodLevel, val -> config.respawnFoodLevel = val)
                                                                .controller(opt -> new IntegerSliderControllerBuilderImpl(opt).range(1, 20).step(1))
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
                                .build()
                )
                .save(() -> AdventurersRespawnsConfig.HANDLER.save())
                .build()
                .generateScreen(parentScreen);
    }
}
