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

### 3.3 Sky light under cover

- Per column, a 16×16 array of sky-source Y values computed over the whole column, not just
  loaded sections.
- Unloaded columns keep a persisted per-column shadow height, so distant terrain still occludes.
- Sky light is an **air-path distance**: for a block, the shortest path through air to a column that
  is open to the sky, following light sideways and downwards. Light therefore travels around cover
  instead of stopping at the column above it: a lone or distant obstruction dims by distance only, a
  wide roof leaves a gradient, and a sealed volume stays dark because no air path reaches it. Light
  falls without losing a level, so a wall that does not reach the ceiling still lets the far side
  light up, and a cell under a cover is lit from the lit cell above it rather than staying black.
- `skyConeAngleDegrees` sets the reach: one block of travel per light level per 15 degrees, so 0
  is vanilla's 15-block shadow and the default 45 reaches 60 blocks (level = 15 − distance / 4).
- The light of a section is computed from the section above it, so a column is lit top-down: each
  cached section asks for the bottom row of the one above (up to 24 sections, or until the column
  has no more stored sections). An absent section is air, which the open-column rule already covers.

### 3.4 Lighting

- Block light: a flood from every emitter within 15 blocks of the section, inside a padded domain so
  a lamp in a neighbouring cube or column lights across the seam. One level per block of air, one per
  dampening, which is vanilla's rule; an enclosed room lit by a lamp lights up, stone swallows it, and
  the vanilla band's own lamps count as sources. The band itself stays vanilla-lit, so an out-of-band
  lamp does not light band cells.
- Sky light: computed per out-of-band section from the column tops and block opacity, then cached
  per section. Light only travels downwards, so a change at one Y invalidates the cached sections at
  and below it in the edited column and in the columns the reach can touch; the occluder tops of
  those columns are dropped only when the edit is at or above the lowest one. Block light reaches in
  every direction, so a change drops it for the column and its 8 neighbours instead. Light data is
  section-keyed and sparse like blocks, and an emitter-free section costs nothing (its cache entry
  is an all-zero array).

### 3.5 Storage format (custom region files)

- One `r.<rx>.<rz>.dcr` per 32×32 chunk columns per dimension, opened lazily with an LRU cap.
- Header (`DCRF` magic + version), then append-only records, one per cube:
  `chunkX, sectionY, chunkZ, payload length, codec id, CRC32, payload`.
- Payloads use **zlib** (`java.util.zip`): no extra dependency and no native library. The
  per-record codec id leaves room for LZ4 or zstd later.
- The index is rebuilt on open by scanning record headers; an interrupted trailing write is
  dropped, and dead space past a threshold triggers compaction.
- Chunk data lives only in `.dcr` (F9): every section of the column, band included, plus the column
  payloads - chunk metadata, entities and points of interest - are records in the same store, told
  apart by a reserved section Y (`Integer.MIN_VALUE`, `+1`, `+2`). No `.mca` is read or written for
  any of them; a `.mca` left by an earlier world is ignored, so converting a vanilla world is a
  later milestone.

### 3.6 Network

- Vanilla sends whole columns; we add cube packets (add/remove/update) plus a world-info handshake
  carrying the vertical range and shadow-map sizes.
- Cubes are sent per player and kept to the box around them: on login, on a section the player enters,
  and when a chunk is newly tracked a rescan sends the stored sections of that column that fall in
  the window; sections the player leaves are removed again, so a client only holds
  `verticalViewDistanceCubes` above and below itself. The window is recomputed when the player's
  section or chunk changes and otherwise once a second, which is also what drops the cubes of a
  column that unloaded. Sections are read from the live column, so nothing waits on the store. An
  edit outside a player's window is broadcast anyway, because the client's render window is taller
  than the sent one; that section then stays on the client until its chunk unloads.
  `-Ddividedchunks.debugCubes=true` logs each rescan's window.
- Vanilla packs a `BlockPos` on the wire into 12 bits of Y, so anything outside ±2048 wraps: a click
  on y=10089 reaches the server as y=1897 and is dropped as out of reach. We widen
  `FriendlyByteBuf.writeBlockPos`/`readBlockPos` (the one seam for every block position) to
  full-range coordinates, which makes both directions Y-correct.
- A client without the mod cannot interoperate: the widened positions leave extra bytes in a packet
  frame, so the connection dies at the first block position instead of corrupting state. NeoForge
  additionally denies such a client through FML's required-payload negotiation.

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
  - F3a Per-section `CubePayload` on both loaders, sent to a joining player and broadcast on every
    out-of-band edit; the client applies sections and removals to its sparse column. *(done)*
  - F3b Render out-of-band sections. The client renders over a fixed-height vertical window that
    follows the player and is rebuilt once the player leaves its middle half; `SectionCopy` fills a
    compiled section from the sparse column when its index is outside the vanilla band. The window is
    taller than the cube so a vertical move does not rebuild it; what is inside it is decided by the
    server's `verticalViewDistanceCubes` (F5). Out-of-band meshes were unlit until F4.
    *(done; 26.1.2 verified in game, 26.2/26.3 compile- and mixin-application-verified only)*
  - F3c Out-of-band interaction. `BlockPos` carries the full Y on the wire, and the build-limit gate
    on click/break uses the configured range instead of the vanilla band, so right-click placement,
    breaking and pick-block work out of band and persist. *(done)*
- **F4 — Lighting.** Vanilla stores light for the band only and reports full sky light below an
  out-of-band roof unless `Level.setBlock` kicked the engine, so the sky layer is answered by
  `LightEngineMixin` from per-section light arrays.
  - F4a Sky light. *(done)* Each out-of-band section gets a light array of air-path distances to
    the nearest column open to the sky, following light sideways and down, cached on the column and
    seeded from the section above so light falls past a wall that does not reach the ceiling. Shadows
    are soft by distance and only an enclosed volume is dark, so a lone block or a floating island
    never blackens what is below it.
  - F4b Block light. *(done)* Every out-of-band section gets a light array flooded from the emitters
    within one reach of it, which is enough to carry a lamp across a cube or chunk seam. A sealed
    room lights up from its own lamp, stone swallows the light, and breaking the lamp darkens it
    again; lamps inside the vanilla band count as sources, while band cells themselves stay
    vanilla-lit.
- **F5 — Loading/tickets.** *(done)* Chunks load as whole columns, so vanilla's per-column tickets
  already cover the vertical axis for storage and entities. What had to be added is what the client
  holds: every player gets the stored sections inside `verticalViewDistanceCubes` of their own
  section, sent when they enter and removed when they leave, and a rescan once a second drops the
  cubes of chunks that unloaded. Cubes come from the live column rather than the store, so a joining
  player never waits on disk. The sky-light carry walks the stored run above a section instead of
  recursing a bounded number of sections, so light that enters a shaft sideways keeps falling
  through any number of stored sections.
- **F6 — World generation.** Vanilla-compat generator wrapped into a band; cubic terrain later.
- **F7 — Entities/structures/biomes/spawn** at arbitrary Y.
- **F8 — Compatibility & API polish.** Events, optional-mod hooks, API docs.
- **F9 — Full-column storage.** *(done)* Chunk sections, the chunk metadata tag, entities and
  points of interest are records of the same `.dcr` store; nothing writes `.mca` any more and light
  is recomputed on load. Converting an existing vanilla world (`.mca` → `.dcr`) is a later
  milestone.

## 7. Risks

- **Dense-array assumption leakage.** Vanilla may reference `sections` in places not visible in
  the sources. Mitigation: grep the compiled sources per milestone; keep the change behind a small
  accessor seam.
- **Light engine rewrite** is the largest single risk.
- **Section position ceiling.** `SectionPos` packs Y into 20 bits, so section Y wraps beyond ±524288
  (±8.4M blocks). The configured vertical range is clamped to that on load, so no block Y outside it
  can be stored or addressed; widening it would mean replacing `SectionPos` itself.
- **Stale entity payloads.** A column saved before a mob wanders off keeps that mob in its entity
  payload, so loading it next to the mob's real column adds one UUID twice. Vanilla drops the second
  copy and logs `UUID of added entity already exists`, so nothing is duplicated, but the surviving
  copy can be the stale one and snap the mob back to where it used to be. Vanilla's `.mca` entity
  files have the same hole; closing it needs an entity-to-column index (F7).
- **Render section allocation.** Centring the render window on the player adds a vertical window of
  sections beside the vanilla band, about 67% more render sections at the default radius of 8. The
  window the server fills is `verticalViewDistanceCubes` above and below the player, so a render
  distance larger than that value draws fewer cubes than it has rows for; raise the config with the
  render distance.
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
5. **Protocol guard:** no custom handshake. A side without the mod, or with another payload version,
   already fails on the widened block positions; only NeoForge rejects it cleanly, through FML.
