package net.iskaa303.dividedchunks.network;

import net.iskaa303.dividedchunks.DividedChunks;
import net.iskaa303.dividedchunks.core.world.ColumnSectionAccess;
import net.iskaa303.dividedchunks.core.world.SectionCodec;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Server-side helpers for sending cube sections to clients. */
public final class CubeSync {

	/** Logs each flush, which is where the cost of a large edit lands. */
	private static final boolean DEBUG = Boolean.getBoolean("dividedchunks.debugCubes");

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
		for (long key : pending) {
			ChunkPos pos = new ChunkPos(SectionPos.x(key), SectionPos.z(key));
			LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x(), pos.z());
			int sectionY = SectionPos.y(key);
			broadcast(level, pos, sectionY,
					chunk == null ? null : ((ColumnSectionAccess) chunk).dividedchunks$getSectionByY(sectionY));
		}
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
