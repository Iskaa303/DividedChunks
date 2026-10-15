# DividedChunks — Implementation Plan

> Approved. Milestones are executed one at a time; the user commits each before the next.
>
> Clean-room: OpenCubicChunks and GTNewHorizons were read for architecture and pitfalls only. No
> code, class names, package names or the `regionlib` storage library are copied.

Workspace rules (git, publishing, source layout, verification) live in `AGENTS.md`.

## 1. Goal

A multiloader (Fabric + NeoForge, MC 26.1.2 / 26.2 / 26.3) mod that turns each dimension into a
cube:

- Horizontal extent stays vanilla (~30M blocks); vertical extent is the same order and
  configurable (default: the full signed 32-bit int range).
- Height and chunk accessors are replaced at the root, so vanilla and modded code asking "what is
  at (x, y, z)?" works at any Y.
- Sky light is occlusion-aware over the full column: a roof far above blocks light, a distant
  floating slab does not.
- Storage is sparse and file-backed (custom region format, no embedded database), with no fixed
  section-count ceiling.

## 2. Architecture

```
net.iskaa303.dividedchunks
├── DividedChunks            entry facade
├── api/                     public loader-agnostic contract (util/, world/, event/)
├── core/                    config, world, storage, lighting, network, server, client
├── mixin/                   common + client mixins
├── compat/                  optional integration hooks
└── platform/                Fabric / NeoForge boundary
```

`api/` is the compatibility contract; it is implemented by mixin-injected interfaces on vanilla
classes, like vanilla's `LevelHeightAccessor`.

## 3. Core design

### 3.1 Addressing

- Int block coordinates throughout; Y is a full signed int, or a configured sub-range.
- `sectionY = blockY >> 4`, `localY = blockY & 15`.
- Iteration is bounds-driven, never array-length-driven: nothing sizes an array by
  `getMaxY() - getMinY()`.

### 3.2 Column / cube storage

- A vanilla `LevelChunk` is a 16×16 column; we give it a sparse `ColumnStorage` (Y-keyed map plus
  a loaded-window index) instead of a dense `LevelChunkSection[]`.
- A cube is one 16³ section at `(chunkX, sectionY, chunkZ)`; cubes load and unload independently
  in Y.
- Vanilla's `sections[i]` lookups (`i = sectionY - minSectionY`) are routed to
  `column.getSection(sectionY)` by mixin.

### 3.3 Sky shadow map + angled cone

- Per column, a 16×16 array of sky-source Y values computed over the whole column, not just
  loaded sections.
- Unloaded columns keep a persisted per-column shadow height, so distant terrain still occludes.
- A column is closed when its top occluding surface belongs to a closed volume, and can reopen
  above a roof.
- Sky light comes from a configurable cone around vertical (`skyConeAngleDegrees`, default 0 =
  vanilla): an off-axis structure shadows partially, a closed roof blocks fully.

### 3.4 Lighting

- Block light: flood fill inside loaded cubes.
- Sky light: seeded from the shadow map, propagated through open volumes.
- Light data is section-keyed and sparse like blocks.

### 3.5 Storage format (custom region files)

- One `r.<rx>.<rz>.dcr` per 32×32 chunk columns per dimension, opened lazily with an LRU cap.
- Header (`DCRF` magic + version), then append-only records, one per cube:
  `chunkX, sectionY, chunkZ, payload length, codec id, CRC32, payload`.
- Payloads use **zlib** (`java.util.zip`): no extra dependency and no native library. The
  per-record codec id leaves room for LZ4 or zstd later.
- The index is rebuilt on open by scanning record headers; an interrupted trailing write is
  dropped, and dead space past a threshold triggers compaction.
- Near term (F2, shipped): vanilla still owns the band — `.mca` keeps its sections, heightmaps,
  block entities and entities — and `.dcr` holds only sections outside the band, so a world with
  no out-of-band blocks is a vanilla world on disk.
- Long term (F9): `.dcr` owns the whole column; vanilla stops writing sections, and an existing
  world is converted by reading its `.mca` band into `.dcr` without regeneration.

### 3.6 Network

- Vanilla sends whole columns; we add cube packets (add/remove/update) plus a world-info handshake
  carrying the vertical range and shadow-map sizes.
- Vanilla clients are rejected with a protocol mismatch.

### 3.7 Config & bundling

- TOML (`config/dividedchunks.toml`) via NightConfig 3.9.0, with hardcoded English comments and no
  enable switch.
- NeoForge uses FML's bundled NightConfig (original package). Fabric Loader has none, so the
  Fabric jar shades and relocates it to `DividedChunks.libraries.electronwill.nightconfig`.
- Attribution ships in `META-INF/licenses/` (see `licenses/ATTRIBUTIONS.md`).

## 4. Mixin scope

- `Level` / `LevelHeightAccessor` — min/max Y, build-height checks.
- `ChunkAccess` / `LevelChunk` / `ProtoChunk` — sparse sections, `getSection`, `getSections`,
  `setBlockState`, `getBlockState`.
- `Heightmap` / `ChunkSkyLightSources` — replaced by the cubic shadow map.
- `LevelLightEngine` / `SkyLightEngine` / `SkyLightSectionStorage` — cubic light.
- `ChunkMap` / `ServerChunkCache` / `ChunkHolder` — 3D tickets.
- `ClientChunkCache` / chunk packets — client cube cache.
- `SerializableChunkData` / `RegionFileStorage` — storage.
- `ChunkGenerator` — worldgen.

The exact list is found empirically: after each change, grep the vanilla sources for code that
still assumes dense sections.

## 5. Testing

- JUnit 5 unit tests in `src/test/java`: coordinate math, sparse-storage invariants, shadow rules,
  codec, region index. No Minecraft runtime.
- In-game GameTests, clean-room from the vanilla/NeoForge/Fabric sources. Shared bodies and the
  loader entries live in `src/test/java/.../gametest/`; the `test` source set is folded into the
  main mod for dev runs, so nothing ships.
- Manual per-milestone verification: run the dev client, read `run/logs/latest.log`, exercise the
  feature.

## 6. Milestones

Each is committed by the user before the next. Vanilla-compat world generation comes before cubic
terrain.

- **F0 — Foundations.** Config, coordinate math (`Coords`, `CubePos`), unit-test wiring, startup
  logging. *(done)*
- **F1 — Sparse column storage.** Dense `LevelChunkSection[]` becomes `ColumnStorage`.
  - F1a `ColumnStorage<T>` + tests. *(done)*
  - F1b `ChunkAccessMixin` seeds it from the constructor array and routes `getSection`/
    `getSections` through it. *(done)*
  - F1c `LevelMixin` widens `isInsideBuildHeight` (what `Level`'s block accessors guard on) to the
    configured range. `isOutsideBuildHeight` deliberately stays the vanilla band, because vanilla
    uses it as the "outside the rendered band" proxy; widening it stalled the client for its full
    30s timeout at any out-of-band Y. `getMinY`/`getMaxY` and the section band stay vanilla, so
    heightmaps, light and spawning keep vanilla behaviour; `LevelChunk` reads and writes any
    section Y. *(done)*
  - F1d In-game GameTest harness. *(done)*
- **F2 — Storage format.** Persist and read back arbitrary-Y cubes. *(done)*
  - F2a `CubeRegionStorage` / `.dcr` region format (zlib records, CRC32, index rebuild, truncation
    recovery, compaction) + tests. *(done)*
  - F2b `SectionCodec` (name-based NBT) + `CubeStores` (one store per dimension, closed when the
    server stops), wired into `LevelChunkMixin`: lazy load on first out-of-band access, write-back
    on change. *(done)*
- **F3 — Network + client.**
  - F3a Per-section `CubePayload` on both loaders, sent on (re)join from `PlayerChunkSender` and
    broadcast on every out-of-band edit; the client applies sections and removals to its sparse
    column. *(done)*
  - F3b Render out-of-band sections. `ViewArea` spans only the vanilla band, so those blocks stay
    invisible; needs a sparse render-section mechanism (and F4 light). *(pending)*
- **F4 — Lighting.** Cubic sky light + occlusion shadow map + configurable cone.
- **F5 — Loading/tickets.** 3D view distance, vertical loading, unload GC.
- **F6 — World generation.** Vanilla-compat generator wrapped into a band; cubic terrain later.
- **F7 — Entities/structures/biomes/spawn** at arbitrary Y.
- **F8 — Compatibility & API polish.** Events, optional-mod hooks, API docs.
- **F9 — Full-column storage & `.mca`→`.dcr` conversion.** A serialization change on the F2
  format: `.dcr` owns the whole column and an existing `.mca` band is imported.

## 7. Risks

- **Dense-array assumption leakage.** Vanilla may reference `sections` in places not visible in
  the sources. Mitigation: grep the compiled sources per milestone; keep the change behind a small
  accessor seam.
- **Light engine rewrite** is the largest single risk.
- **World format lock-in.** Version the region index and cube codec from day one.
- **Performance.** Naive sparse maps can cost more than vanilla arrays; benchmark.

## 8. Resolved decisions

1. **Vertical extent:** the full signed 32-bit int range, which is also the default; configurable
   per dimension. At this range `getMaxY() - getMinY()` overflows int, so height must never be
   derived by int subtraction.
2. **Storage:** custom region files with zlib; no embedded database and no native dependency.
3. **Lighting:** an angled sky cone, configurable per dimension; an occlusion-aware shadow map
   over the full column, persisted for unloaded columns.
4. **Worldgen:** vanilla-compat band first; the rest is air until the cubic generator.
