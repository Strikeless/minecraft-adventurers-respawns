# Adventurer's Respawns
This is a modification for the Minecraft game, providing various alternative game mechanics related to player respawns.

## Features
- Respawn players in a village or similar structure(*) near their death position
- Alter the health and food level that a player respawns with
- Change a player's experience to be kept upon death
- Give a respawning player a map with a marker to their death position and/or vanilla spawnpoint
- Give a respawning player a compass or a recovery compass
- Skip some game time upon respawning (You probably shouldn't enable this on a server!)

Every listed feature can be independently disabled or enabled to your liking.

(*) _Default configuration for respawn structures includes villages and igloos, along with taverns and witch villas from the Dungeons and Taverns datapack (not required). These structures can be modified to your liking in the configuration file, although there is no GUI for this as of yet._

## For developers
PRs and issues are always welcome, but I see this mod as mostly feature complete, so don't expect any huge new features. I'm still open to smaller features though, if they sound useful enough to warrant the maintenance cost).

We're using [Stonecutter](https://codeberg.org/stonecutter/stonecutter) to hopefully ease supporting multiple game versions. Besides that, it's a very typical [Fabric](https://fabricmc.net/develop) project, using Java.
