# DividedChunks — Implementation Plan

> Status: **APPROVED (2026-xx-xx)** — decisions recorded in §9. Milestones are executed
> one feature at a time; the user commits each feature before the next starts.
>
> Clean-room rule: this is a from-scratch implementation. The OpenCubicChunks and
> GTNewHorizons repositories were read for *architecture and pitfalls only*. No code,
> class names, package names, or the `regionlib` storage library are copied. All new
> code is original and lives under `net.iskaa303.dividedchunks`.

## 1. Goal

A multiloader (Fabric + NeoForge, MC 26.1.2 / 26.2 / 26.3) mod that turns each overworld
dimension into a *cubic* world:

- Horizontal extent stays vanilla (~30M blocks).
- Vertical extent becomes the same order of magnitude (configurable), so the world is a
  cube, not a tall thin slab.
- All vanilla height/chunk accessors are replaced at the root, not special-cased, so
  vanilla and modded code that asks "what is at (x, y, z)?" simply works at any Y.
- Sky light is occlusion-aware over an unbounded column (a roof far above blocks light;
  a distant floating slab does not black out the ground depending on config).
- Storage is sparse and file-backed (a custom region format, no embedded database) with
  no fixed section-count ceiling.
- Default vertical range is ±30,000,000 (matching the vanilla horizontal extent); it is
  configurable.

## 2. Constraints inherited from `AGENTS.md`

- Never touch git, never publish, never edit `.sc_active_version` or user-owned files.
- One shared `src/` tree; every file must compile for all six version+loader targets.
- Stonecutter guards must use the exact `/*...*///?}` shape.
- New files written in the **active** version's comment state (`26.3-neoforge`).
- `./gradlew build` (all six) after shared changes; run the affected client to verify
  runtime behaviour; report exactly what was and was not verified.

## 3. Architecture

```
net.iskaa303.dividedchunks
├── DividedChunks                 (existing entry facade)
├── api/                          public, loader-agnostic, no MC internals beyond needed types
│   ├── util/    CubePos, Coords, CubicBox, HeightMap (interface)
│   ├── world/   ICubicWorld, ICubicColumn, ICube, IHeightMap, ICubicLevelHeightAccessor
│   └── event/   lifecycle events (world init, column load/unload, cube load/unload)
├── core/
│   ├── config/    DividedChunksConfig (per-world/per-dimension + global)
│   ├── math/      coordinate conversions, section<->cube<->block, Y addressing
│   ├── world/     Column, Cube, SectionStorage (sparse), heightmap impls
│   ├── lighting/  cubic light engine + occlusion shadow map
│   ├── storage/   on-disk format (independent of vanilla RegionFileStorage)
│   ├── network/   cube packets, world info handshake
│   ├── server/    chunk/cube loading, tickets, player tracking
│   └── client/    client cube cache, render source
├── mixin/         common + client mixins (see §5)
├── compat/        optional integration hooks
└── platform/      existing Fabric/NeoForge boundary (untouched structurally)
```

The public `api/` package is the compatibility contract promised to other mods. It is
implemented by mixin-injected interfaces on vanilla classes, exactly like the vanilla
`LevelHeightAccessor` pattern.

## 4. Core design (tentative — depends on the open questions)

### 4.1 Addressing
- Everything is expressed in **int block coordinates**. `Y` is a full signed int
  (or a configured sub-range), matching `X`/`Z`.
- `sectionY = blockY >> 4`, `localY = blockY & 15`, same for X/Z.
- No global `minY`/`maxY` array sizing anywhere: all iteration is bounds-driven, not
  array-length-driven.

### 4.2 Column / Cube storage (the heart)
- A vanilla `LevelChunk` **is** a 16×16 column. We give it a sparse
  `SectionStorage` (a Y-keyed map plus a loaded-window index), not a dense
  `LevelChunkSection[]`.
- A "cube" is one 16³ `LevelChunkSection` at `(chunkX, sectionY, chunkZ)`; cubes load
  and unload independently in Y.
- Vanilla methods that assume `sections[i]` with `i = sectionY - minSectionY` are
  replaced via mixin so lookups become `column.getSection(sectionY)`.
- Loaded window per column is bounded by the *vertical view distance*, so iteration
  stays O(loaded) and never O(world height).

### 4.3 Heightmap / occlusion shadow map + angled sky cone
- Per column, a 16×16 array of "sky source" Y values, but computed with an
  occlusion-aware rule over the **entire** column, not just loaded sections.
- Unloaded columns retain a persisted `shadow height per column` so distant terrain
  still occludes sky light without being loaded.
- A column is "fully closed" when its topmost occluding surface is part of a closed
  volume; then nothing below receives sky light. Handled by tracking, per column, the
  highest *open* Y (a column can be reopened above a roof).
- **Angled sky cone (approved):** sky light is collected from a configurable cone of
  directions around the vertical, not only straight down. A structure far above/off-axis
  therefore shadows a column only partially, while a closed roof still blocks it fully.
  The cone half-angle (`skyConeAngleDegrees`, default 0 = vertical vanilla behaviour) is
  configurable per dimension.

### 4.4 Lighting
- Block light: local flood fill inside loaded cubes, standard.
- Sky light: seeded from the shadow map, propagated through open volumes, with the
  configurable cone (Q3) affecting how far horizontally a shadow is cast.
- Light data storage is section-keyed and sparse like blocks.

### 4.5 Storage format (approved: custom region files)

- **Own format**, no embedded database. One region file per 32&#215;32 chunk columns per
  dimension (`r.<rx>.<rz>.dcr`), opened lazily with an LRU cap on open files.
- A file is a header (`DCRF` magic + format version) followed by append-only records, one
  per cube: `chunkX, sectionY, chunkZ, payload length, codec id, CRC32, payload`.
- Payloads use **zlib** (`java.util.zip`), so the mod needs no extra dependency, no native
  library and no bundling. The per-record codec id leaves room for LZ4 (already on both
  loaders' classpath via vanilla) or zstd later.
- The index is rebuilt on open by scanning record headers; bytes left by an interrupted
  write are dropped, and dead space past the threshold triggers compaction into a rewritten
  file.
- Columns/heights/biomes get their own files later; the format version is the migration
  anchor.
- Must support reading a vanilla-generated world's base band so an existing world can
  be lifted into cubic mode without regeneration (conversion path is a later milestone).

### 4.6 Network
- Vanilla sends whole columns including every section in the build range; that is
  unworkable. We add cube packets (add/remove/update) plus a world-info handshake that
  tells the client the vertical range and shadow map sizes.
- Vanilla clients are rejected (protocol mismatch), same as CubicChunks.

### 4.7 Config & bundled dependencies

- Config is TOML (`config/dividedchunks.toml`) via **NightConfig 3.9.0**, with hardcoded English
  comments (never localized) and no enable switch.
- NeoForge/FML already provides NightConfig; NeoForge builds use that copy and keep the original
  package. Fabric Loader does not provide it, so the Fabric jar shades (Shadow plugin) and relocates
  the library to `DividedChunks.libraries.electronwill.nightconfig`. No jar-in-jar.
- **Distant Horizons comparison:** this is DH's Shadow namespace relocation. We only shade on Fabric
  and let NeoForge use FML's copy, so NeoForge bytecode keeps `com.electronwill.nightconfig` and only
  the Fabric bytecode is rewritten.
- License/attribution texts ship in `META-INF/licenses/` (see `licenses/ATTRIBUTIONS.md`).

## 5. Mixin scope (initial, to be refined per milestone)

Vanilla surface that must be replaced:
- `LevelHeightAccessor` / `Level` — min/max Y, `isOutsideBuildHeight`, `getSectionsCount`.
- `ChunkAccess` / `LevelChunk` / `ProtoChunk` — sparse section storage, `getSection`,
  `getSections`, `setBlockState`, `getBlockState`, `getHighestFilledSectionIndex`.
- `Heightmap` / `ChunkSkyLightSources` — replaced by the cubic shadow map.
- `LevelLightEngine` / `SkyLightEngine` / `SkyLightSectionStorage` — cubic light data.
- `ChunkMap` / `ServerChunkCache` / `ChunkHolder` — 3D ticket/loading.
- `ClientChunkCache` / chunk packets — client cube cache.
- `SerializableChunkData` / `RegionFileStorage` — storage format (or bypass entirely).
- `ChunkGenerator` — worldgen (later milestone).

The exact list is discovered empirically: after each core change, grep the compiled
vanilla sources for the methods that still assume dense sections.

## 6. Testing strategy

- **Pure-logic unit tests** (`src/test/java`, JUnit 5): coordinate math, sparse storage
  invariants, heightmap/occlusion rules, palette codec, region index math. These need no
  Minecraft runtime and are the primary regression net. Wired in `build-logic`; Stonecutter
  processes `src/test` for every version, so all six targets run the same tests.
  - The `test` source set extends `main`'s compile/runtime classpath, so tests can use
    Minecraft and loader types when needed.
- **In-game GameTests** (26.x registry-driven framework). Clean-room, written from the
  vanilla/NeoForge/Fabric sources in this repo, not copied:
  - Shared logic and the loader entries live in `src/test/java/.../gametest/`, next to the unit
    tests. The `test` source set is folded into the main mod for dev runs, so no game-test code
    reaches the jar.
  - Fabric: `FabricGameTests` is the `fabric-gametest` entrypoint declared in the generated
    `fabric.mod.json`; the entrypoint is never loaded outside a dev/gametest run.
  - NeoForge: `NeoforgeGameTests` is an `@EventBusSubscriber` handling `RegisterGameTestsEvent`
    and registering an in-code `InlineGameTest`.
  - Built-in scene/environment: the `minecraft:empty` structure plus a self-registered empty
    environment, so no NBT templates have to be authored.
  - Runs: `./gradlew :<mc>-neoforge:runGameTestServer` and `./gradlew :<mc>-fabric:runGameTestServer`.
    Both exit non-zero if a required test fails.
- **In-game manual verification** per milestone: run `:26.3-neoforge:runClient` (and
  `:26.1.2-fabric` when convenient), read `run/logs/latest.log`, exercise the feature, then
  hand to the user.
- Every milestone states its own "done when" checklist.

## 7. Milestones (feature-by-feature; each is committed by the user before the next)

Approved order: **vanilla-compat world generation first**, cubic terrain generation
later.

- **F0 — Foundations.** Config (TOML/NightConfig), coordinate math (`Coords`, `CubePos`),
  JUnit test harness, startup logging. *(done)*
- **F1 — Sparse column storage.** Replace the dense `LevelChunkSection[]` with `ColumnStorage`,
  keeping vanilla height, so behaviour is unchanged. Split into reviewable halves:
  - **F1a** — `ColumnStorage<T>` primitive + unit tests. *(done)*
  - **F1b** — `ChunkAccessMixin` seeds `ColumnStorage` from the constructor's dense array and
    routes `getSection(int)`/`getSections()` through it (`getSections()` returns a cached
    compatibility view). `ChunkAccess`'s other accessors already go through those two methods,
    so they are map-backed as well. Behaviour-preserving; the dense array stays only as the
    construction-time seed. *(done)*
  - **F1c** — `LevelMixin` widens only `isInsideBuildHeight`/`isOutsideBuildHeight` to the
    per-dimension config range; `getMinY`/`getMaxY` and the whole section band stay on
    `dimensionType()`, so heightmaps, light, natural spawning and every other loop bounded by the
    min/max Y keep vanilla behaviour. `LevelChunk` reads/writes blocks at any section Y through
    `ColumnStorage` — creating sections on demand and skipping the heightmap/light side effects
    outside the band — and `ChunkHolder.blockChanged` ignores out-of-band changes. Verified
    in-game: a client world ticks with a player logged in (natural spawning) without crashing,
    and `/setblock` at y=+1,000,000 and y=-1,000,000 succeeds with `execute if block` read-back
    and `/data get block` on a chest at y=1,000,001. *(done)*
  - **F1c follow-ups:** out-of-band sections are in-memory only (persistence is F2); the light
    engine, renderer, network and `BulkSectionAccess` still see the band only (F3/F4).
  - **F1d** — In-game GameTest harness: tests live in `src/test/java/.../gametest/` alongside the
    unit tests, with the `test` source set folded into the main mod for dev runs and per-loader
    `runGameTestServer` configurations. The first test writes a block at y=+1,000,000 and reads it
    back; verified passing on NeoForge and Fabric. *(done)*
- **F2 — Storage format.** Persist arbitrary-Y cubes to disk and read them back. Split:
  - **F2a** — `core.storage.CubeStorage` + `CubeRegionStorage`: the custom
    `r.<rx>.<rz>.dcr` region format (zlib records keyed by `(chunkX, sectionY, chunkZ)`,
    CRC32, index rebuild on open, truncation recovery, compaction). JUnit tests cover
    store/read/remove/reopen, region routing, extreme coordinates, corruption and
    compaction. *(done)*
  - **F2b** — Encode a `LevelChunkSection` to bytes and wire it: write the cube when a block is set
    outside the vanilla band, and read the stored cubes back into the column when the chunk loads.
- **F3 — Network + client.** Cube packets + client cube cache + rendering at high Y.
- **F4 — Lighting.** Cubic sky light + occlusion shadow map (+ configurable cone).
- **F5 — Loading/tickets.** 3D view distance, vertical loading, unload GC.
- **F6 — World generation.** Vanilla-compat generator wrapped into a band (cubic terrain later).
- **F7 — Entities/structures/biomes/spawn** at arbitrary Y.
- **F8 — Compatibility & API polish.** Events, optional-mod hooks, docs for the API.

## 8. Risks

- **Dense-array assumption leakage.** Vanilla 26.x may reference `sections` in places
  not visible in the extracted sources (loader patches, other classes). Mitigation:
  grep compiled sources per milestone; keep the change behind a small accessor seam.
- **Light engine rewrite** is the largest single risk; it is isolated to F5.
- **World format lock-in.** A custom format is hard to change later. Design the region
  index and cube codec to be versioned from day one (Q2).
- **Performance.** Sparse maps per column, if naive, can cost more than vanilla arrays.
  Mitigation: windowed arrays inside the sparse store, benchmark in F2/F6.
- **Clean-room provenance.** Keep a short note per file describing what it was
  independently derived from, to make the clean-room claim auditable.

## 9. Resolved decisions

1. **Vertical extent:** the full signed 32-bit int range, which is also the default (effectively
   unbounded — vanilla's range without the world border); configurable per dimension. Values
   outside the range are clamped and snapped to cube boundaries. **Caveat for F1:** at this range
   `getMaxY() - getMinY()` overflows int, so height must never be derived by int subtraction; use
   long or a sentinel.
2. **Storage:** **custom region files with zlib**, no embedded database and no native
   dependency. Height configurable, default 30M.
3. **Lighting:** **angled sky cone**, configurable per dimension. Occlusion-aware shadow
   map over the full column; persisted for unloaded columns; a fully closed column blocks
   all sky light.
4. **Worldgen:** **vanilla-compat band first** — wrap the vanilla generator to fill the
   normal band; remaining cube volume is air until the cubic generator milestone.
