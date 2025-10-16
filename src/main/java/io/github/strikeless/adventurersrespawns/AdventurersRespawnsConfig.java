package io.github.strikeless.adventurersrespawns;

import com.google.gson.GsonBuilder;
import dev.isxander.yacl3.api.NameableEnum;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class AdventurersRespawnsConfig {
    public static ConfigClassHandler<AdventurersRespawnsConfig> HANDLER = ConfigClassHandler.createBuilder(AdventurersRespawnsConfig.class)
            .id(Identifier.of("adventurers-respawns", "config"))
            .serializer(config -> {
                return GsonConfigSerializerBuilder.create(config)
                        .setPath(FabricLoader.getInstance().getConfigDir().resolve("adventurers_respawns.json5"))
                        .appendGsonBuilder(GsonBuilder::setPrettyPrinting)
                        .setJson5(true)
                        .build();
            })
            .build();

    @SerialEntry
    public boolean respawnAtStructures = true;

    @SerialEntry
    public int respawnStructureFuzzyExtentChunks = 12;

    @SerialEntry
    public int respawnStructureMaxSearchExtentChunks = 512;

    @SerialEntry
    public List<String> respawnStructureIdentifiers = new ArrayList<>(List.of(
            "minecraft:villages",
            "minecraft:igloo",
            // Structures from the Dungeons and Taverns datapack
            "nova_structures:taverns",
            "nova_structures:witch_villa"
    ));

    @SerialEntry
    public boolean respawnStructureIgnoreVerticalBoundsFix = false;

    @SerialEntry
    public int respawnHealth = 10;

    @SerialEntry
    public int respawnFoodLevel = 10;

    @SerialEntry
    public float respawnSaturationLevel = 0.0F;

    @SerialEntry
    public byte mapScale = 3;

    @SerialEntry
    public boolean giveDeathPositionMap = true;

    @SerialEntry
    public String deathPositionMapName = "Death position";

    @SerialEntry
    public boolean giveSpawnpointMap = false;

    @SerialEntry
    public String spawnpointMapName = "Home";

    @SerialEntry
    public boolean timeSkipOnRespawn = false;

    @SerialEntry
    public long timeSkipMinTime = 8000;

    @SerialEntry
    public long timeSkipMaxTime = 24000;

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
        public Text getDisplayName() {
            return switch (this) {
                case Vanilla -> Text.literal("Vanilla");
                case Keep -> Text.literal("Keep");
                case Drop -> Text.literal("Drop");
                case Destroy -> Text.literal("Destroy");
            };
        }
    }

    @SerialEntry
    public DeathExperienceBehavior deathExperienceBehavior = DeathExperienceBehavior.Vanilla;
}
