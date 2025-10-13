package io.github.strikeless.adventurersrespawns.client.config;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.gui.controllers.ControllerPopupWidget;
import dev.isxander.yacl3.impl.controller.IntegerSliderControllerBuilderImpl;
import dev.isxander.yacl3.impl.controller.TickBoxControllerBuilderImpl;
import io.github.strikeless.adventurersrespawns.AdventurersRespawns;
import io.github.strikeless.adventurersrespawns.AdventurersRespawnsConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.net.URI;
import java.nio.file.Paths;
import java.util.List;

public class AdventurersRespawnsConfigScreenFactory {
    public static Screen getConfigScreen(Screen parentScreen) {
        var def = AdventurersRespawnsConfig.HANDLER.defaults();
        var config = AdventurersRespawns.getConfig();

        return YetAnotherConfigLib.createBuilder()
                .title(Text.literal("Adventurers Respawns Configuration"))
                .category(
                        ConfigCategory.createBuilder()
                                .name(Text.literal("Adventurers Respawns Configuration"))
                                .group(
                                        OptionGroup.createBuilder()
                                                .name(Text.literal("Respawn at structures"))
                                                .option(
                                                        Option.<Boolean>createBuilder()
                                                                .name(Text.literal("Enabled"))
                                                                .description(OptionDescription.of(
                                                                        Text.literal("Whether to respawn at a nearby fit structure (e.g. a village) instead of the vanilla spawnpoint.")
                                                                ))
                                                                .binding(def.respawnAtStructure, () -> config.respawnAtStructure, val -> config.respawnAtStructure = val)
                                                                .controller(TickBoxControllerBuilderImpl::new)
                                                                .build()
                                                )
                                                .option(
                                                        Option.<Integer>createBuilder()
                                                                .name(Text.literal("Closest structure fuzzy range (chunks)"))
                                                                .description(OptionDescription.of(
                                                                        Text.literal(
                                                                                "The range in chunks from which a random fit structure will " +
                                                                                        "be selected if many are present, instead of the very closest one."
                                                                        ),
                                                                        Text.literal(""),
                                                                        Text.literal(
                                                                                "Higher values can add variety to the spawn position whenever dying many times " +
                                                                                        "in the same area, at the expense of potential chunk generation and the time it takes."
                                                                        ),
                                                                        Text.literal(""),
                                                                        Text.literal(
                                                                                "High values may hang the game for a while upon spawning due to chunk generation, " +
                                                                                        "since every chunk within this range from the death position will be generated before spawning. " +
                                                                                        "The amount of chunks needed grows fast, a value of 32 already requires 1024 nearby chunks not even accounting the vertical axis."
                                                                        ).formatted(Formatting.RED)
                                                                ))
                                                                .binding(def.closestStructureFuzzyRangeChunks, () -> config.closestStructureFuzzyRangeChunks, val -> config.closestStructureFuzzyRangeChunks = val)
                                                                .controller(opt -> new IntegerSliderControllerBuilderImpl(opt).range(0, 256).step(1))
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
                .build()
                .generateScreen(parentScreen);
    }
}
