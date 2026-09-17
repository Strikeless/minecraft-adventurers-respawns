package io.github.strikeless.adventurersrespawns.main;

import com.google.gson.GsonBuilder;
import dev.isxander.yacl3.api.NameableEnum;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AdventurersRespawnsConfig {
    public static ConfigClassHandler<AdventurersRespawnsConfig> HANDLER = ConfigClassHandler.createBuilder(AdventurersRespawnsConfig.class)
            .id(Identifier.fromNamespaceAndPath("adventurers-respawns", "config"))
            .serializer(config -> {
                return GsonConfigSerializerBuilder.create(config)
                        .setPath(FabricLoader.getInstance().getConfigDir().resolve("adventurers_respawns.json5"))
                        .appendGsonBuilder(GsonBuilder::setPrettyPrinting)
                        .setJson5(true)
                        .build();
            })
            .build();

    @SerialEntry(nullable = true)
    public @Nullable String logLevelOverride = null;

    @SerialEntry
    public boolean respawnAtStructures = true;

    @SerialEntry
    public int respawnStructureFuzzyRadiusChunks = 12;

    @SerialEntry
    public int respawnStructureSearchRadiusChunks = 256;

    // TODO: Expose this in a user-friendly manner in the modmenu GUI.
    @SerialEntry(comment = "Structures or structure tags, at which the player may respawn in.")
    public List<String> respawnStructureKeys = new ArrayList<>(List.of(
            "#minecraft:village",
            "minecraft:igloo",
            "#minecraft:village",
            "minecraft:igloo",
            // Structures from various mods/datapacks.
            "#nova_structures:taverns",
            "#nova_structures:remnants",
            "#nova_structures:ruin_town",
            "nova_structures:witch_villa",
            "nova_structures:bunker",
            "structory:graveyard",
            "structory:firetower",
            "structory:old_manor"
    ));

    @SerialEntry(comment = "Blocks or block tags, next to which respawns will first be tried, in descending order of preference.")
    public List<String> respawnNextToBlocks = new ArrayList<>(List.of(
            "#minecraft:beds",
            "minecraft:chest",
            "minecraft:barrel",
            "#minecraft:doors"
    ));

    // TODO: Expose this in the modmenu GUI once respawn structures can be edited from there.
    @SerialEntry(comment = "Whether to always respawn in the overworld, if the player died in the end or nether.\nCoordinates are translated as if the player had traveled through a portal.")
    public boolean respawnStructureAlwaysInOverworld = true;

    @SerialEntry
    public boolean respawnStructureIgnoreVerticalBoundsFix = true;

    @SerialEntry
    public int respawnHealth = 20;

    @SerialEntry
    public int respawnFoodLevel = 20;

    @SerialEntry
    public float respawnSaturation = 0.0F;

    @SerialEntry
    public byte mapScale = 3;

    @SerialEntry
    public boolean giveDeathPositionMap = false;

    @SerialEntry
    public String deathPositionMapName = "Death position";

    @SerialEntry
    public boolean giveSpawnpointMap = false;

    @SerialEntry
    public String spawnpointMapName = "Home";

    @SerialEntry
    public boolean timeSkipOnRespawn = false;

    @SerialEntry
    public int timeSkipMinTimeTicks = 8000;

    @SerialEntry
    public int timeSkipMaxTimeTicks = 24000;

    public enum DeathExperienceBehavior implements NameableEnum {
        /// Don't alter behavior
        Vanilla,

        /// Always keep experience points upon death, even with keepInventory off.
        Keep,

        /// Always drop experience points upon death, even with keepInventory on.
        Drop,

        /// Always permanently lose experience points upon death, even with keepInventory on.
        Destroy;

        @Override
        public Component getDisplayName() {
            return switch (this) {
                case Vanilla -> Component.literal("Vanilla");
                case Keep -> Component.literal("Keep");
                case Drop -> Component.literal("Drop");
                case Destroy -> Component.literal("Destroy");
            };
        }
    }

    @SerialEntry
    public DeathExperienceBehavior deathExperienceBehavior = DeathExperienceBehavior.Vanilla;

    public enum GivenCompassType implements NameableEnum {
        None,
        NormalCompass,
        RecoveryCompass;

        @Override
        public Component getDisplayName() {
            return switch (this) {
                case None -> Component.literal("None");
                case NormalCompass -> Component.literal("Normal compass");
                case RecoveryCompass -> Component.literal("Recovery compass");
            };
        }
    }

    @SerialEntry
    public GivenCompassType giveCompassType = GivenCompassType.None;
}
