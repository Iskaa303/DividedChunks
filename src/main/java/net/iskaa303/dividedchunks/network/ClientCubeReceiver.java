package net.iskaa303.dividedchunks.network;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.iskaa303.dividedchunks.DividedChunks;
import net.iskaa303.dividedchunks.core.world.ChunkTags;
import net.iskaa303.dividedchunks.core.world.ColumnSectionAccess;
import net.iskaa303.dividedchunks.core.world.SectionCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Applies a received cube section to the client's sparse column cache. */
public final class ClientCubeReceiver {

	private static final boolean DEBUG = Boolean.getBoolean("dividedchunks.debugCubes");

	/** Cubes received before their column's chunk packet, per column; drained once the column is there. */
	private static final Map<Long, Map<Integer, CubePayload>> HELD = new ConcurrentHashMap<>();

	/** Sections a cube filled or emptied, so the occlusion graph stops treating them as empty. */
	private static final LongOpenHashSet BECAME_EMPTY = new LongOpenHashSet();

	private static final LongOpenHashSet BECAME_FILLED = new LongOpenHashSet();

	/** Columns worth holding, far past any window the server sends, so a stuck entry cannot pile up. */
	private static final int MAX_HELD_COLUMNS = 512;

	private ClientCubeReceiver() {
	}

	public static void handle(CubePayload payload) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null || !level.getChunkSource().hasChunk(payload.chunkX(), payload.chunkZ())) {
			// A removal for a column the client does not have needs no copy, and nothing else can be
			// placed yet, so the cube waits for the column instead of being thrown away.
			if (payload.data().length > 0 && HELD.size() < MAX_HELD_COLUMNS) {
				HELD.computeIfAbsent(ChunkPos.pack(payload.chunkX(), payload.chunkZ()),
						ignored -> new ConcurrentHashMap<>()).put(payload.sectionY(), payload);
			}
			if (DEBUG) {
				DividedChunks.LOGGER.info("Held cube {} {} {}: its column is not on the client",
						payload.chunkX(), payload.sectionY(), payload.chunkZ());
			}
			return;
		}
		apply(level, payload);
	}

	/** Places cubes whose column has arrived since they were received; called once per frame. */
	public static void applyHeld(ClientLevel level) {
		if (HELD.isEmpty()) {
			return;
		}
		HELD.forEach((column, cubes) -> {
			if (!level.getChunkSource().hasChunk(ChunkPos.getX(column), ChunkPos.getZ(column))) {
				return;
			}
			HELD.remove(column);
			cubes.values().forEach(payload -> apply(level, payload));
		});
	}

	private static void apply(ClientLevel level, CubePayload payload) {
		LevelChunk chunk = level.getChunk(payload.chunkX(), payload.chunkZ());
		ColumnSectionAccess access = (ColumnSectionAccess) chunk;
		if (payload.sectionY() == CubeSync.SKY_TOPS_Y) {
			applyTops(level, payload, access);
			return;
		}
		if (payload.data().length == 0) {
			access.dividedchunks$removeSection(payload.sectionY());
			BECAME_FILLED.remove(SectionPos.asLong(payload.chunkX(), payload.sectionY(), payload.chunkZ()));
			BECAME_EMPTY.add(SectionPos.asLong(payload.chunkX(), payload.sectionY(), payload.chunkZ()));
		} else {
			access.dividedchunks$putSection(payload.sectionY(),
					SectionCodec.decode(payload.data(), level.palettedContainerFactory()));
			BECAME_EMPTY.remove(SectionPos.asLong(payload.chunkX(), payload.sectionY(), payload.chunkZ()));
			BECAME_FILLED.add(SectionPos.asLong(payload.chunkX(), payload.sectionY(), payload.chunkZ()));
		}
		if (DEBUG) {
			DividedChunks.LOGGER.info("Applied cube {} {} {}: {} bytes", payload.chunkX(), payload.sectionY(),
					payload.chunkZ(), payload.data().length);
		}
		// The section's light was computed against its earlier content, so it is dropped before the re-mesh.
		access.dividedchunks$invalidateLightAround(payload.sectionY() << 4);
		// A section that arrives after its render section was compiled has to be re-meshed.
		level.setSectionDirtyWithNeighbors(payload.chunkX(), payload.sectionY(), payload.chunkZ());
	}

	/**
	 * Takes the server's occluder tops for a column, which shade the band under a cube the client's own
	 * section window does not reach, and re-meshes the column so the new shadow shows.
	 */
	private static void applyTops(ClientLevel level, CubePayload payload, ColumnSectionAccess access) {
		int[] tops = payload.data().length == 0
				? null
				: ChunkTags.fromBytes(payload.data()).getIntArray("tops").orElse(null);
		access.dividedchunks$setSkyOccluderTops(tops);
		if (tops == null) {
			return;
		}
		int highest = Integer.MIN_VALUE;
		for (int top : tops) {
			if (top != Integer.MIN_VALUE) {
				highest = Math.max(highest, top);
			}
		}
		if (highest == Integer.MIN_VALUE) {
			return;
		}
		// Light only flows down, so every section at or below the highest top has a new value.
		for (int sectionY = level.getMinSectionY(); sectionY <= SectionPos.blockToSectionCoord(highest); sectionY++) {
			level.setSectionDirtyWithNeighbors(payload.chunkX(), sectionY, payload.chunkZ());
		}
	}

	/**
	 * Reports the sections a cube filled or emptied to the occlusion graph, which otherwise keeps an
	 * out-of-band section in its empty set forever and never compiles it.
	 */
	public static void applyContentChanges(SectionOcclusionGraph graph) {
		if (BECAME_EMPTY.isEmpty() && BECAME_FILLED.isEmpty()) {
			return;
		}
		//? if >=26.2 {
		LongOpenHashSet emptied = new LongOpenHashSet(BECAME_EMPTY);
		LongOpenHashSet filled = new LongOpenHashSet(BECAME_FILLED);
		BECAME_EMPTY.clear();
		BECAME_FILLED.clear();
		graph.updateEmptySections(emptied, filled);
		//?} else {
		/*BECAME_EMPTY.clear();
		BECAME_FILLED.clear();
		graph.invalidate();
		*///?}
	}
}
