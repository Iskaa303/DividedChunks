# AGENTS.md

Guidance for agents working in this repository. Read this whole file before changing anything.

## What this is

A multiloader Minecraft mod workspace built on [Stonecutter](https://stonecutter.kikugie.dev/wiki/v2/):
one shared source tree, six Gradle projects, two loaders × three Minecraft versions.

| | |
|---|---|
| mod id | `dividedchunks` |
| package | `net.iskaa303.dividedchunks` |
| targets | Fabric + NeoForge on Minecraft `26.1.2`, `26.2`, `26.3` |
| toolchain | Java 25 (Minecraft 26.x requires it) |
| mod version | (in `stonecutter.properties.toml`) |

## Ground rules (do not break these)

* **Clean-room code only — never copy code.** Do not copy source, comments, identifiers, data or
  file structure from any other project, and do not paste code from the internet. In particular the
  [OpenCubicChunks/CubicChunks](https://github.com/OpenCubicChunks/CubicChunks) and
  [GTNewHorizons/CubicChunks1710](https://github.com/GTNewHorizons/CubicChunks1710) repositories
  (and Distant Horizons, and any other mod) may be read for architecture and pitfalls only. Every
  line written here must be original. Attribution belongs in `licenses/` and `README.md`, never as
  copied code.
* **Never touch git.** No `git init`, `git add`, `git commit`, `git checkout`, branch/tag/stash, no
  `.github/` workflows. The user owns the repository; leave changes in the working tree for them.
* **Never edit `.sc_active_version`** (and never run `Set active project to <v>`, `Refresh active
  project` or `Reset active project` unless the user asks for it). Those tasks run the Stonecutter
  comment processor and can rewrite the comment state of files in `src/`. The active version is the
  user's choice; address a specific version with `:26.2-neoforge:<task>` instead of switching.
* **Don't rewrite user-owned files**: `README.md`, `LICENSE.txt`, `licenses/`, `CHANGELOG.md`.
  Add to them only when explicitly asked.
* `versions/<mc>-<loader>/run/**` is **runtime state** (options.txt, config/, logs, saves, mods) —
  gitignored, machine-specific, and not a place to fix bugs. Never diagnose a problem by editing it
  unless the user is debugging runtime settings; check `run/logs/latest.log` instead.
* Generated/ignored: `build/`, `versions/*/build/`, `.gradle/`, `.gradle-home/`, `.devenv/`,
  `devenv.lock`. Never hand-edit build output, generated manifests, or cache contents.
* **Do not re-add what was deliberately removed**: Forge/legacy-forge, more Minecraft versions. Ask
  first if a task seems to need one. (Datagen and Mod Menu are already wired up — see below.)
* **Never publish or release**: don't run `publishMods` / `publishGithub` / `publishModrinth` /
  `publishCurseforge` (they upload), don't push or delete tags, and don't edit `.github/workflows/`,
  `cliff.toml`, `CHANGELOG.md` or the publishing logic in `build-logic/` unless the user asks for it.
* **Don't bump version pins casually** (`stonecutter.properties.toml`, `gradle/libs.versions.toml`,
  `settings.gradle.kts`): stonecutter 0.9.8, loom-back-compat 0.4.1 + loom 1.18.4, ModDevGradle
  2.0.148, fabric-loader 0.19.5 are a tested combination.
* Keep the odd formatting of Stonecutter guards exactly as it is (`/*...*///?}` on one line); the
  comment processor depends on the syntax, not on prettiness.

## Environment

Everything needed is declared in `devenv.nix` (JDK 25, OpenGL/Vulkan, SDL3/OpenAL, audio libs, and
`GRADLE_USER_HOME` pointed at `./.gradle-home` so caches stay out of a small `$HOME`).

Check before assuming: use the dev shell **only if the machine needs it** — i.e. it is NixOS
(`test -e /etc/NIXOS`) or has no usable JDK 25 / graphics libraries:

```bash
devenv shell -- ./gradlew build      # non-interactive
devenv shell                         # interactive, then ./gradlew ...
```

On a machine that already has JDK 25 and working graphics drivers, plain `./gradlew ...` is fine.
Run every Gradle command from the repository root. `devenv` evaluates `devenv.nix` on each call
(≈20 s), and `LD_LIBRARY_PATH`/`XDG_DATA_DIRS` are only wired up inside that shell — a build outside
it will still work, a *client run* outside it may crash on GL/Vulkan/OpenAL.

## Project layout

```
build-logic/                      convention plugins (Kotlin DSL): manifest generation, jar tasks,
                                  publishing, root tasks. Change only when the build itself changes.
build.fabric.gradle.kts           per-loader build logic: loom, runs, fabric-api deps
build.neoforge.gradle.kts         per-loader build logic: moddevgradle, runs, ATs
settings.gradle.kts               plugin repositories, the Stonecutter version matrix
stonecutter.gradle.kts            active version, loader constants, mod_* swaps
stonecutter.properties.toml       mod metadata + per-version dependencies (the version matrix deps)
gradle/libs.versions.toml         plugin/library versions
devenv.nix                        Nix dev shell (see above)
src/main/java/net/iskaa303/dividedchunks/
                                  the ONLY source tree: shared code + platform.<loader> code
src/main/resources/               assets, dividedchunks.mixins.json, aw/<mcver>.{accesswidener,cfg}
versions/<mc>-<loader>/           generated Gradle projects: build/ and run/ only, no sources
```

`versions/*` contain no Java sources. Every version compiles the same `src/` with Stonecutter's
comments resolved for that version+loader, so a single source file must be valid for *all* six
targets.

## How the build fits together

* `settings.gradle.kts` `stonecutter { create(rootProject) { match("26.2", "fabric", "neoforge") } }`
  creates `versions/26.2-fabric` etc., each using `build.<loader>.gradle.kts`.
* `build-logic` (`Loader.kt`, `LoaderMetadata.kt`, `Context.kt`, `ModPlatformPlugin.kt`) writes
  `fabric.mod.json` / `META-INF/neoforge.mods.toml` at build time from `stonecutter.properties.toml`
  plus the `platform { dependencies { ... } }` block of the per-loader build script. Consequences:
  * Entrypoints are hardcoded to `net.iskaa303.dividedchunks.platform.<loader>.{FabricEntrypoint,
    FabricClientEntrypoint, NeoforgeEntrypoint, ...}`. Renaming/moving those classes means editing
    `build-logic/src/main/kotlin/Loader.kt` too (and adding new entrypoints means the same).
  * Both manifests reference `dividedchunks.mixins.json` and `aw/<mcver>.*`; the files must exist.
  * `mod.description`, `mod.sources_url`, `mod.homepage_url`, `mod.issues_url`, `mod.discord_url` may
    stay empty (optional in `Context.kt`). `mod.version`, the `mod.channel_tag` key, `mod.license.*`,
    `mod.environment` and `mod.authors` are required.
* `stonecutter.gradle.kts` swaps `/*$ mod_id*/`, `/*$ mod_name*/`, `/*$ mod_group*/`,
  `/*$ mod_version*/`, `/*$ minecraft*/` — keep those markers next to the literals in
  `DividedChunks.java`; edit the values in `stonecutter.properties.toml`, not in the Java.
* `dev_jvm_args` from `stonecutter.properties.toml` are added to every client *and* server run.
* Adding an MC version: `match(...)` in `settings.gradle.kts`, the `[fabric."<v>"]` and
  `[neoforge."<v>"]` tables in `stonecutter.properties.toml`, and `src/main/resources/aw/<v>.cfg` +
  `aw/<v>.accesswidener` (missing access transformer/widener files break the loader plugins).

## Commands

```bash
./gradlew build                          # compile + jar all six targets (run this after shared-code changes)
./gradlew buildAndCollect                # same, and copy jars to build/libs/0.0.1-dev-SNAPSHOT/
./gradlew :26.3-fabric:build             # one target
./gradlew :26.2-neoforge:runClient       # dev client, one target
./gradlew runActiveClient                # client of the version in .sc_active_version
./gradlew runActiveServer                # ... and its server
```

A bare `./gradlew runClient` starts **all six** clients (task names resolve across every project
that has them) — always qualify the project, or use `runActiveClient`. Runtime logs live in
`versions/<mc>-<loader>/run/logs/latest.log`.

## CI/CD and publishing

Two GitHub Actions workflows in `.github/workflows/`:

* `build.yml` — on every push/PR: JDK 25 + `./gradlew buildAndCollect --no-daemon`, uploads
  `build/libs/` as the `build-artifacts` artifact. Needs no configuration.
* `release.yml` — on tag push: reads `[mod].version` + `[mod].channel_tag` from
  `stonecutter.properties.toml` and **deletes the tag and the GitHub release if the tag does not match**
  (only `{version}{channel_tag}`, e.g. `0.0.1-dev`, is accepted), regenerates `CHANGELOG.md` with
  git-cliff using `cliff.toml` (`%%%repo_url%%%` is replaced with the repository URL), then builds and
  runs `./gradlew publishMods` with `MOD_IS_RELEASE=true` (drops the `-SNAPSHOT` suffix from versions).

Publishing reads the environment, falling back to a `.env` file in the repo root (gitignored;
`.env.template` documents every key):

| variable | kind | effect |
|---|---|---|
| `PUB_GITHUB_ENABLE` | repo variable | `true` publishes GitHub releases |
| `PUB_DRY_RUN` | repo variable | `true` simulates publishing, uploads nothing |
| `PUB_MODS_ENABLE` | repo variable | gates Modrinth + CurseForge **only**; GitHub releases do not need it (see `PublishingLogic.kt`) |
| `PUB_MODRINTH_PROJECT_ID`, `PUB_CURSEFORGE_PROJECT_ID` | repo variable | project ids, needed only once mods publishing is enabled |
| `PUB_MODRINTH_STAGING`, `PUB_MAVEN_ENABLE`, `PUB_MAVEN_CENTRAL_ENABLE`, `PUB_MAVEN_CENTRAL_SNAPSHOTS` | repo variable | staging / maven toggles |
| `PUB_MODRINTH_TOKEN`, `PUB_CURSEFORGE_TOKEN` | secret | platform API tokens |
| `PUB_MAVEN_CENTRAL_USERNAME`, `PUB_MAVEN_CENTRAL_PASSWORD`, `PUB_SIGNING_KEY`, `PUB_SIGNING_ID`, `PUB_SIGNING_PASSWORD` | secret | Maven Central + signing |
| `MOD_IS_RELEASE` | set by `release.yml` | release build (no `-SNAPSHOT`) |

Currently only `PUB_GITHUB_ENABLE=true` and `PUB_DRY_RUN=false` are configured; Modrinth/CurseForge and
Maven Central are deliberately unconfigured (no project ids/tokens), and with nothing enabled the
per-platform `publishMods` tasks stay a no-op dry run.

## Data generation (Fabric datagen)

Datagen uses fabric-api and writes into per-version, committed directories:

```bash
./gradlew :26.3-fabric:runDatagen      # -> versions/datagen/26.3/src/main/generated/
```

* Providers are registered in
  `net.iskaa303.dividedchunks.platform.fabric.datagen.FabricDataGeneratorEntrypoint` via `pack.addProvider(...)`
  (it registers none right now, so a run is a no-op).
* The fabric manifest in `build-logic` declares that class as the `fabric-datagen` entrypoint, so the
  class and its entry in `Loader.kt` must both exist.
* `build.fabric.gradle.kts` points `fabricApi.configureDataGeneration` at
  `versions/datagen/<mcver>/src/main/generated` (`client = true`), and `build.neoforge.gradle.kts` adds
  the same directory as an extra resource source dir — generated data ships on both loaders, but only
  Fabric can generate it.
* Output is per Minecraft version and meant to be committed; `.gitkeep` files keep the empty dirs.

## Mod Menu (Fabric dev client only)

`build.fabric.gradle.kts` pulls `com.terraformersmc:modmenu` from the TerraformersMC Maven as
`modLocalRuntime`, with the version pinned per Minecraft version through `deps.modmenu` in
`stonecutter.properties.toml` (18.0.2 / 20.0.3 / 21.0.0 for 26.1.2 / 26.2 / 26.3). It is deliberately
**not** a declared dependency: it never appears in `fabric.mod.json` and is not shipped — it only gives
the Fabric dev client a mods list screen (NeoForge has one built in). Bump it by picking the Mod Menu
release that lists the Minecraft version (`https://maven.terraformersmc.com/releases/com/terraformersmc/modmenu/`
or the Mod Menu page on Modrinth/CurseForge).

## Writing code

Shared code goes in `net.iskaa303.dividedchunks`, loader-specific code in
`net.iskaa303.dividedchunks.platform.<loader>`. Loader-agnostic code reaches the loader through the
`Platform` interface (`DividedChunks.xplat()`).

### Stonecutter comments

```java
// loader guards (constants are set from the loader in stonecutter.gradle.kts)
//? fabric {
FabricLoader.getInstance().isModLoaded(modId);
//?} neoforge {
/*ModList.get().isLoaded(modId);
 *///?}
```

Keep the `*///?}` shape and the leading space of the ` *` lines. Inactive branches must stay
commented out with those exact markers; when in doubt, copy the pattern from an existing file
(`DividedChunks.java`, `platform/*/*.java`).

Version conditions (`//? >=26.2 { ... //?}`), `else` branches and boolean expressions are documented
in the [Stonecutter wiki v2](https://stonecutter.kikugie.dev/wiki/v2/) and the
[multiloader guide](https://stonecutter.kikugie.dev/wiki/tips/multiloader) — check there instead of
inventing syntax. Note that version expressions are evaluated on the version string
(`sc.current.parsed`), so a full `./gradlew build` is the real check that all six variants compile.

The **active** version in `.sc_active_version` is compiled straight from the raw files in `src/`,
while every other version is compiled from Stonecutter's processed copies in
`versions/<ver>/build/generated/stonecutter/main/java/`. So a new file with guards must be written in
the *active* version's comment state (match the arrangement of a neighbouring file, or diff against
that generated directory) — otherwise the active version's build fails on code that the other five
versions would compile fine.

### 26.x API notes (verified here, but confirm against the real jars)

* `net.minecraft.resources.Identifier` — `ResourceLocation` was renamed in 26.x, and
  `Identifier.fromNamespaceAndPath(...)` is the factory.
* 26.1+ ships **unobfuscated**, so no mappings are configured (no Parchment/yarn/Mojmap blocks in
  `build.fabric.gradle.kts` — that is intentional, don't "fix" it).
* NeoForge 26.x: `FMLLoader.getCurrent().getVersionInfo().mcVersion()` and
  `!FMLLoader.getCurrent().isProduction()`; entrypoints use `@Mod`, client init `@EventBusSubscriber`.
* Fabric: `FabricLoader.getInstance().getRawGameVersion()`; entrypoints via
  `dev.kikugie.fletching_table.annotation.fabric.Entrypoint` + `fabric.mod.json`.
* NeoForge 26.2+ manifests use `iconFile` (the build-logic manifest already emits both
  `logoFile`/`iconFile`).

### Reading Minecraft / NeoForge / Fabric code

The game jars are already on disk after any build or run of a version, and are plain (unobfuscated)
class files. `GRADLE_USER_HOME` is `./.gradle-home` in this workspace (`~/.gradle` elsewhere);
`<project>/.gradle/` is the Gradle project cache.

| what | where |
|---|---|
| Fabric game jar (classes, real names) | `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-<hash>/<mcver>/minecraft-merged-<hash>-<mcver>.jar` |
| NeoForge: vanilla client jar | `$GRADLE_USER_HOME/caches/neoformruntime/artifacts/minecraft_<mcver>_client.jar` |
| NeoForge: patched game + decompiled `*.java` sources | `$GRADLE_USER_HOME/caches/neoformruntime/intermediate_results/mergeWithSources_*_output.jar` |
| NeoForge's own sources | `$GRADLE_USER_HOME/caches/modules-2/files-2.1/net.neoforged/neoforge/<neoforge-ver>/<hash>/neoforge-<ver>-sources.jar` |

Quick ways to look inside (inside the dev shell, which has JDK 25):

```bash
# signatures of one class
javap -p -c -classpath .gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-*/26.3/minecraft-merged-*-26.3.jar net.minecraft.client.Minecraft
# a generated mixin/extra source file, or a whole class, from the merged sources jar
jar tf .gradle-home/caches/neoformruntime/intermediate_results/mergeWithSources_*_output.jar | grep '\.java$' | head
```

In IntelliJ, Ctrl+click on any `net.minecraft.*` symbol opens the bundled decompiler — that is the
fastest way to read vanilla/loader behaviour. Prefer this over guessing API shapes.

## Verification expectations

* After touching shared code or build scripts: `./gradlew build` (all six variants) — one file is
  compiled six times, so a change can break only some targets.
* After touching runtime behaviour: run the affected `:mc-loader:runClient` and read
  `versions/<mc>-<loader>/run/logs/latest.log` for the expected lines; don't report success from a
  compile alone.
* Javadoc "no comment" warnings during `build` are noise and expected.
* Report what you actually verified and what you did not.

## Known issues (already diagnosed — don't re-investigate from scratch)

* **Vulkan on NeoForge 26.1.2/26.2 crashes** with
  `GLFW error 65540: Vulkan: Window surface creation requires the window to have the client API set to
  GLFW_NO_API` (FancyModLoader 11's early loading window is a GL window). Workaround: set
  `earlyWindowControl = false` in `versions/<mc>-neoforge/run/config/fml.toml`. NeoForge 26.3 (FML 12)
  is fine, so don't add such a file for 26.3.
* After such a crash Minecraft writes `startedCleanly:false` to that version's `run/options.txt` and
  then *forces OpenGL* on the next launch ("Detected unexpected shutdown during last game startup").
  A single clean quit clears it. It is not a mod or build problem.
* Sound requires `libpulse`/`libpipewire` to be dlopen-able; lack of them gives
  `Error starting SoundSystem ... Failed to open OpenAL device`. `devenv.nix` provides them — only
  relevant for runs outside the dev shell.
* NeoForge dev clients show harmless `Failed to load library flite` narrator errors.

## Documentation links

Minecraft (version notes, changes, requirements — Java 25, new `year.drop.hotfix` format):

* 26.1.2 — https://minecraft.wiki/w/Java_Edition_26.1.2
* 26.2 — https://minecraft.wiki/w/Java_Edition_26.2
* 26.3 — https://minecraft.wiki/w/Java_Edition_26.3
* version format — https://minecraft.wiki/w/Version_formats

NeoForge:

* docs (current release) — https://docs.neoforged.net/docs/gettingstarted/
* modding primer, versioned: 26.1 — https://docs.neoforged.net/primer/docs/26.1/ ,
  26.2 — https://docs.neoforged.net/primer/docs/26.2/ ,
  26.3 — https://docs.neoforged.net/primer/docs/26.3/
* ModDevGradle toolchain docs — https://docs.neoforged.net/toolchain/docs/
* the main docs site's version selector only goes up to 1.21.11; for 26.x use the primer above

Fabric:

* developer docs — https://docs.fabricmc.net/develop/ (unversioned, tracks the newest release)
* Loom docs — https://docs.fabricmc.net/develop/loom/
* Fabric API javadoc, pinned versions: 26.1.2 — https://maven.fabricmc.net/docs/fabric-api-0.155.3+26.1.2/ ,
  26.2 — https://maven.fabricmc.net/docs/fabric-api-0.161.0+26.2/ ,
  26.3 — https://maven.fabricmc.net/docs/fabric-api-0.163.0+26.3/
* Fabric Loader javadoc — https://maven.fabricmc.net/docs/fabric-loader-0.19.5/

Stonecutter:

* wiki v2 — https://stonecutter.kikugie.dev/wiki/v2/
* multiloader guide — https://stonecutter.kikugie.dev/wiki/tips/multiloader
