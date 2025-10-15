package io.github.strikeless.adventurersrespawns;

import com.google.gson.GsonBuilder;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;
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
    public boolean respawnAtStructure = true;

    @SerialEntry
    public int structureFuzzyExtentChunks = 16;

    @SerialEntry
    public int structureMaxSearchExtentChunks = 512;

    @SerialEntry
    public List<String> structureIdentifiers = new ArrayList<>(List.of(
            "minecraft:village",
            "minecraft:igloo",
            // Structures from the Dungeons and Taverns datapack
            "nova_structures:taverns",
            "nova_structures:witch_villa"
    ));

    @SerialEntry
    public int respawnHealth = 8;

    @SerialEntry
    public int respawnFoodLevel = 8;

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
}
