package io.github.strikeless.adventurersrespawns.main.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;

public class PlayerUtil {
    /// Ugly utility function to get a MinecraftServer from a ServerPlayer without having to explicitly deal with closing the level object,
    /// as you should do with "serverPlayer.level().getServer()", since a ServerLevel/Level is AutoCloseable, implementing closing for at least a chunk source (whatever that process does).
    ///
    /// Note that this just hides closing the level object and will throw an unmarked RuntimeException, if for some reason the closer fails with an IOException (or any other unmarked exception for that matter).
    public static MinecraftServer getServer(ServerPlayer serverPlayer) {
        try (var level = serverPlayer.level()) {
            return level.getServer();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
