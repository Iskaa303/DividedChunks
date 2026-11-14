package net.iskaa303.dividedchunks.network;

import net.iskaa303.dividedchunks.DividedChunks;
import net.iskaa303.dividedchunks.core.world.ChunkTags;
import net.iskaa303.dividedchunks.core.world.ColumnSectionAccess;
import net.iskaa303.dividedchunks.core.world.SectionCodec;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Server-side helpers for sending cube sections to clients. */
public final class CubeSync {

	/** Logs each flush, which is where the cost of a large edit lands. */
	private static final boolean DEBUG = Boolean.getBoolean("dividedchunks.debugCubes");

	/**
	 * Section Y of the payload that carries a column's sky occluder tops instead of a section. The store
	 * keeps its own tags at {@link net.iskaa303.dividedchunks.core.storage.CubeRegionStorage#CHUNK_TAG_Y}
	 * and up, which is well below the sections a column can hold.
	 */
	public static final int SKY_TOPS_Y = Integer.MIN_VALUE + 3;

	/** Sections edited since the last tick, per level; a 30^3 fill touches 27 of them, not 29791. */
	private static final Map<ServerLevel, Set<Long>> PENDING = new ConcurrentHashMap<>();

	private CubeSync() {
	}

	/**
	 * Records that one section changed. The cube is sent once the tick is over, so a fill of thousands
	 * of blocks costs one payload per section instead of one per block.
	 */
	public static void markChanged(ServerLevel level, ChunkPos pos, int sectionY) {
		PENDING.computeIfAbsent(level, ignored -> ConcurrentHashMap.newKeySet())
				.add(SectionPos.asLong(pos.x(), sectionY, pos.z()));
	}

	/** Sends every cube edited during the tick, one payload per section, and returns how many. */
	public static int flush(ServerLevel level) {
		Set<Long> pending = PENDING.remove(level);
		if (pending == null) {
			return 0;
		}
		long start = System.nanoTime();
		Set<ChunkPos> columns = new HashSet<>();
		for (long key : pending) {
			ChunkPos pos = new ChunkPos(SectionPos.x(key), SectionPos.z(key));
			LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x(), pos.z());
			int sectionY = SectionPos.y(key);
			broadcast(level, pos, sectionY,
					chunk == null ? null : ((ColumnSectionAccess) chunk).dividedchunks$getSectionByY(sectionY));
			columns.add(pos);
		}
		columns.forEach(pos -> broadcastTops(level, pos));
		if (DEBUG) {
			DividedChunks.LOGGER.info("Sent {} edited cubes in {} us", pending.size(),
					(System.nanoTime() - start) / 1000L);
		}
		return pending.size();
	}

	/** Sends one section to a single player; a {@code null} section removes it there. */
	public static void send(ServerPlayer player, ServerLevel level, ChunkPos pos, int sectionY,
			@Nullable LevelChunkSection section) {
		DividedChunks.xplat().sendToPlayer(player, payload(level, pos, sectionY, section));
	}

	/**
	 * Sends a column's sky occluder tops to a single player. The client needs them to shade the band
	 * under a cube far above it, which its own section window does not reach.
	 */
	public static void sendTops(ServerPlayer player, ServerLevel level, ChunkPos pos) {
		DividedChunks.xplat().sendToPlayer(player, topsPayload(level, pos));
	}

	/** Whether a column has an occluder above the vanilla band, which is the only case tops are sent for. */
	public static boolean hasOutOfBandTops(ServerLevel level, ChunkPos pos) {
		LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x(), pos.z());
		return chunk != null && highestTop((ColumnSectionAccess) chunk) > level.getMaxY();
	}

	private static void broadcastTops(ServerLevel level, ChunkPos pos) {
		DividedChunks.xplat().sendToTracking(level, pos, topsPayload(level, pos));
	}

	private static int highestTop(ColumnSectionAccess access) {
		int top = Integer.MIN_VALUE;
		for (int localZ = 0; localZ < 16; localZ++) {
			for (int localX = 0; localX < 16; localX++) {
				top = Math.max(top, access.dividedchunks$skyOccluderTop(localX, localZ));
			}
		}
		return top;
	}

	private static CubePayload topsPayload(ServerLevel level, ChunkPos pos) {
		LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x(), pos.z());
		if (chunk == null) {
			return new CubePayload(pos.x(), SKY_TOPS_Y, pos.z(), new byte[0]);
		}
		ColumnSectionAccess access = (ColumnSectionAccess) chunk;
		if (highestTop(access) <= level.getMaxY()) {
			return new CubePayload(pos.x(), SKY_TOPS_Y, pos.z(), new byte[0]);
		}
		int[] tops = new int[256];
		for (int localZ = 0; localZ < 16; localZ++) {
			for (int localX = 0; localX < 16; localX++) {
				tops[localZ << 4 | localX] = access.dividedchunks$skyOccluderTop(localX, localZ);
			}
		}
		CompoundTag tag = new CompoundTag();
		tag.putIntArray("tops", tops);
		return new CubePayload(pos.x(), SKY_TOPS_Y, pos.z(), ChunkTags.toBytes(tag));
	}

	/** Sends one section to every player tracking the chunk; a {@code null} section removes it there. */
	private static void broadcast(ServerLevel level, ChunkPos pos, int sectionY, @Nullable LevelChunkSection section) {
		DividedChunks.xplat().sendToTracking(level, pos, payload(level, pos, sectionY, section));
	}

	private static CubePayload payload(ServerLevel level, ChunkPos pos, int sectionY,
			@Nullable LevelChunkSection section) {
		byte[] data = section == null || section.hasOnlyAir()
				? new byte[0]
				: SectionCodec.encode(section, level.palettedContainerFactory());
		return new CubePayload(pos.x(), sectionY, pos.z(), data);
	}
}
