# Changelog for 1.5.0
- Respawn structures are now configured using structure resource/tag keys instead of structure/structureset resource keys. This makes configuration easier when structures from mods are present, since you can use the same identifiers as you'd use with the locate command.
- Made internal logic for respawn location search noticeably more tolerant, when the first structure/position candidate isn't usable.
- Made preferred spawn-next-to blocks configurable using block resource/tag keys.

# Update notes
- If you had previously configured custom respawn structures, you will have to configure those again, as the mod was updated to use the same identifiers as the locate command.
