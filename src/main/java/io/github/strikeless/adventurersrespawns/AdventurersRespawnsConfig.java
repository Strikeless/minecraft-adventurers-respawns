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
    public int closestStructureFuzzyRangeChunks = 16;

    @SerialEntry
    public int structureClosestBlockFuzziness = 256;

    @SerialEntry
    public List<Identifier> structureTags = new ArrayList<>(List.of(
            Identifier.ofVanilla("village")
    ));

    @SerialEntry
    public int respawnHealth = 8;

    @SerialEntry
    public int respawnFoodLevel = 8;

    @SerialEntry
    public byte mapScale = 3;

    @SerialEntry
    public boolean giveDeathPositionMap = true;

    @SerialEntry
    public String deathPositionMapName = "Point of death";

    @SerialEntry
    public boolean giveSpawnpointMap = true;

    @SerialEntry
    public String spawnpointMapName = "Home";
}
