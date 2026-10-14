# DividedChunks

## Development

Everything the client needs (Java 25, OpenGL/Vulkan, SDL3/OpenAL libraries) is provided by
[devenv](https://devenv.sh/):

```bash
devenv shell
./gradlew runActiveClient          # client of the active version
./gradlew runActiveServer          # server of the active version
./gradlew build                    # build every loader/version
./gradlew :26.3-neoforge:runClient # one specific loader/version
```

Every version is a separate Gradle project, so run tasks are always addressed with the
version/loader (`:26.1.2-fabric:runClient`) or through the active version
(`runActiveClient`). A bare `./gradlew runClient` would start *all* versions at once.

The active version lives in `.sc_active_version` (e.g. `26.3-fabric`). Switch it with
`./gradlew "Set active project to 26.3-fabric"` or by editing that file.

Version or loader specific code goes behind Stonecutter comments:

```java
//? fabric {
fabricOnlyCode();
//?} neoforge {
/*neoforgeOnlyCode();
 *///?}
```

Shared code lives in `net.iskaa303.dividedchunks`, loader specific code in
`net.iskaa303.dividedchunks.platform.<loader>`; the `Platform` interface is what shared code sees.
Per-version dependencies (Minecraft, fabric-api, NeoForge, ...) are in `stonecutter.properties.toml`.

### Vulkan on NeoForge 26.1.2 / 26.2

Minecraft 26.x lets you pick the rendering API (`options.txt` -> `preferredGraphicsBackend`, or the
video settings). OpenGL works everywhere. Vulkan on **NeoForge 26.1.2/26.2** crashes with
`GLFW error 65540: Vulkan: Window surface creation requires the window to have the client API set to
GLFW_NO_API`, because the NeoForge early loading window (FancyModLoader 11) is a GL window and
Minecraft cannot make a Vulkan surface out of it. NeoForge 26.3 (FancyModLoader 12) no longer has
that early window, so Vulkan works there untouched. To use Vulkan on the older two, disable the early
loading window in that version's run directory:

```toml
# versions/26.2-neoforge/run/config/fml.toml
earlyWindowControl = false
```

### Reading Minecraft's code

26.x ships unobfuscated, so the jars on disk use the real `net.minecraft.*` names and can be read
directly - in IntelliJ, Ctrl+click on any `net.minecraft` symbol decompiles it. They show up after the
first build or run of a version:

* Fabric: `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-<hash>/<mcver>/minecraft-merged-<hash>-<mcver>.jar`
* NeoForge: in the Gradle home (`./.gradle-home` here, `~/.gradle` elsewhere)
  * `caches/neoformruntime/artifacts/minecraft_<mcver>_client.jar` — vanilla client
  * `caches/neoformruntime/intermediate_results/mergeWithSources_*_output.jar` — patched game plus its
    decompiled `*.java` sources (that is what the IDE attaches)
  * `caches/modules-2/files-2.1/net.neoforged/neoforge/<neoforge-ver>/<hash>/neoforge-<ver>-sources.jar` — NeoForge itself

From the shell (`devenv shell` provides `javap`):

```bash
javap -p -c -classpath .gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-*/26.3/minecraft-merged-*-26.3.jar net.minecraft.client.Minecraft
jar tf .gradle-home/caches/neoformruntime/intermediate_results/mergeWithSources_*_output.jar | grep '\.java$' | head
```

## Credits

The build setup is adapted from
[rotgruengelb/stonecutter-mod-template](https://github.com/rotgruengelb/stonecutter-mod-template) (MIT),
which is itself adapted from
[murderspagurder/mod-template-java](https://github.com/murderspagurder/mod-template-java) and
[KikuGie's Elytra Trims](https://github.com/kikugie/elytra-trims).
Its license is kept in `licenses/MOD_TEMPLATE_LICENSE.txt`.

DividedChunks is licensed under the Open Software License 3.0 (`LICENSE.txt`),
Copyright (c) 2026 Iskaa303.
