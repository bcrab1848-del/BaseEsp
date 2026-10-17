# BaseESP for Fabric 1.21.8 (source)

Java 21 required. Build with Gradle using `gradle build` (Fabric dependencies require internet); the mod JAR will be in `build/libs/`. This is an untested source project, not a verified compiled release.

B toggles ESP. Right Shift opens settings. Add block IDs such as `minecraft:amethyst_cluster`, `minecraft:diamond_block`, or `minecraft:chest`; remove them with the settings list. Settings persist to `.minecraft/config/baseesp.json`.

Base Finder incrementally scans loaded chunks within the configured radius. When a chunk has amethyst buds/clusters plus at least the chosen number of selected non-amethyst blocks, it posts a potential-base alert with chunk coordinates. Naturally generated amethyst can produce false positives. It cannot access chunks not sent to your client. The initial scan may take time; it checks one chunk every three client ticks, within the player's vertical radius. Rendering uses outline boxes, not translucent solid fills.
