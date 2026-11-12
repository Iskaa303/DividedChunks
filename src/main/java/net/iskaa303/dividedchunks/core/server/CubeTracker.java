package net.iskaa303.dividedchunks.core.server;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.iskaa303.dividedchunks.DividedChunks;
import net.iskaa303.dividedchunks.core.config.DividedChunksConfig;
import net.iskaa303.dividedchunks.core.world.ColumnSectionAccess;
import net.iskaa303.dividedchunks.network.CubeSync;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Keeps the cubes one client holds to the box around its player: a section that enters the vertical
 * window is sent, one that leaves it is removed, and a column the player stops tracking drops all of
 * its own. Vanilla tracks chunks per column, so the vertical axis is this class's job.
 */
public final class CubeTracker {

	/** Logs each rescan's window, which the fog keeps from being visible in game. */
	private static final boolean DEBUG = Boolean.getBoolean("dividedchunks.debugCubes");

	/** Ticks between rescans while the player stands still, which picks up columns that unloaded. */
	private static final int RESCAN_TICKS = 20;

	/** Cubes on the client, packed with {@link SectionPos#asLong(int, int, int)}. */
	private final LongSet sent = new LongOpenHashSet();

	private int lastSectionY = Integer.MIN_VALUE;
	private long lastChunkPos = Long.MIN_VALUE;
	private int idleTicks;

	/**
	 * Lowest section of the window: {@code half} cubes below the player, never past the stored range
	 * and never past the player's own section, so the window cannot end up empty.
	 */
	public static int windowMin(int playerSectionY, int half, int minSectionY) {
		return Math.max(playerSectionY - half, Math.min(playerSectionY, minSectionY));
	}

	/** Highest section of the window, clamped like {@link #windowMin}. */
	public static int windowMax(int playerSectionY, int half, int maxSectionY) {
		return Math.min(playerSectionY + half, Math.max(playerSectionY, maxSectionY));
	}

	/** Rescans once the player has moved to another section or column, and every second besides. */
	public void tick(ServerPlayer player, ServerLevel level) {
		int sectionY = SectionPos.blockToSectionCoord(player.blockPosition().getY());
		long chunkPos = player.chunkPosition().pack();
		boolean moved = sectionY != this.lastSectionY || chunkPos != this.lastChunkPos;
		if (!moved && ++this.idleTicks < RESCAN_TICKS) {
			return;
		}
		this.idleTicks = 0;
		this.lastSectionY = sectionY;
		this.lastChunkPos = chunkPos;
		this.rescan(player, level, sectionY);
	}

	private void rescan(ServerPlayer player, ServerLevel level, int playerSectionY) {
		DividedChunksConfig.DimensionConfig range = DividedChunks.config()
				.forDimension(level.dimension().identifier().toString());
		int half = DividedChunks.config().verticalViewDistanceCubes;
		int storedMin = SectionPos.blockToSectionCoord(range.minY);
		int storedMax = SectionPos.blockToSectionCoord(range.maxY);
		int minSectionY = windowMin(playerSectionY, half, storedMin);
		int maxSectionY = windowMax(playerSectionY, half, storedMax);

		LongSet wanted = new LongOpenHashSet();
		int bandMin = level.getMinSectionY();
		int bandMax = level.getMaxSectionY();
		// Band sections arrive with the chunk packet; sending them as cubes duplicates them, and
		// removing one later would leave vanilla's band array without a section at that index.
		player.getChunkTrackingView().forEach(pos -> {
			LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x(), pos.z());
			if (chunk == null) {
				return;
			}
			((ColumnSectionAccess) chunk).dividedchunks$forEachSection((sectionY, section) -> {
				if (sectionY >= minSectionY && sectionY <= maxSectionY && !section.hasOnlyAir()
						&& (sectionY < bandMin || sectionY > bandMax)) {
					wanted.add(SectionPos.asLong(pos.x(), sectionY, pos.z()));
				}
			});
		});

		for (long key : wanted) {
			if (!this.sent.contains(key)) {
				send(level, player, key);
			}
		}
		for (long key : this.sent) {
			if (!wanted.contains(key)) {
				remove(level, player, key);
			}
		}
		if (DEBUG) {
			DividedChunks.LOGGER.info("Cube window around section {}: {} held, {} of them new", playerSectionY,
					wanted.size(), wanted.size() - this.sent.size());
		}
		this.sent.clear();
		this.sent.addAll(wanted);
	}

	private static void send(ServerLevel level, ServerPlayer player, long key) {
		ChunkPos pos = new ChunkPos(SectionPos.x(key), SectionPos.z(key));
		LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x(), pos.z());
		int sectionY = SectionPos.y(key);
		CubeSync.send(player, level, pos, sectionY,
				chunk == null ? null : ((ColumnSectionAccess) chunk).dividedchunks$getSectionByY(sectionY));
	}

	private static void remove(ServerLevel level, ServerPlayer player, long key) {
		CubeSync.send(player, level, new ChunkPos(SectionPos.x(key), SectionPos.z(key)), SectionPos.y(key), null);
	}
}
