package net.iskaa303.dividedchunks.api.util;

/**
 * Conversions between block coordinates and 16&#215;16&#215;16 cube coordinates.
 *
 * <p>Y is treated exactly like X and Z: a full signed {@code int}. All arithmetic uses
 * arithmetic shifts and bit masks so negative coordinates round the same way as positive
 * ones. Nothing here allocates.</p>
 */
public final class Coords {

	/** Side length of a cube, in blocks. Matches a vanilla chunk section. */
	public static final int CUBE_SIZE = 16;
	/** {@code CUBE_SIZE - 1}, used to mask a block coordinate down to its local offset. */
	public static final int CUBE_MASK = CUBE_SIZE - 1;
	/** log2(CUBE_SIZE). */
	public static final int CUBE_SHIFT = 4;

	private Coords() {
	}

	/** Cube coordinate containing the given block coordinate. */
	public static int blockToCube(int block) {
		return block >> CUBE_SHIFT;
	}

	/** Offset of the block coordinate inside its cube, always in {@code [0, 15]}. */
	public static int blockToLocal(int block) {
		return block & CUBE_MASK;
	}

	/** Lowest block coordinate of the given cube. */
	public static int cubeToMinBlock(int cube) {
		return cube << CUBE_SHIFT;
	}

	/** Highest block coordinate of the given cube. */
	public static int cubeToMaxBlock(int cube) {
		return (cube << CUBE_SHIFT) | CUBE_MASK;
	}

	/** Block coordinate at the centre of the given cube. */
	public static int cubeToCenterBlock(int cube) {
		return (cube << CUBE_SHIFT) | (CUBE_SIZE >> 1);
	}

	/** Block coordinate of {@code local} inside the given cube. {@code local} is masked to {@code [0, 15]}. */
	public static int localToBlock(int cube, int local) {
		return (cube << CUBE_SHIFT) | (local & CUBE_MASK);
	}

	/** X/Z chunk coordinate containing the given block coordinate. Identical to {@link #blockToCube(int)}. */
	public static int blockToChunk(int block) {
		return block >> CUBE_SHIFT;
	}

	/** Lowest block coordinate of the given X/Z chunk. */
	public static int chunkToMinBlock(int chunk) {
		return chunk << CUBE_SHIFT;
	}
}
