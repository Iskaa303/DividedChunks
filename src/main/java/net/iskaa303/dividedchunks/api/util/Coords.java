package net.iskaa303.dividedchunks.api.util;

/** Block <-> cube coordinate conversions; negative coordinates round like positive ones. */
public final class Coords {

	/** Cube side length in blocks; same as a vanilla chunk section. */
	public static final int CUBE_SIZE = 16;
	/** {@code CUBE_SIZE - 1}, used to mask a block coordinate to its local offset. */
	public static final int CUBE_MASK = CUBE_SIZE - 1;
	/** log2(CUBE_SIZE). */
	public static final int CUBE_SHIFT = 4;

	private Coords() {
	}

	public static int blockToCube(int block) {
		return block >> CUBE_SHIFT;
	}

	/** Offset of the block coordinate inside its cube, always in {@code [0, 15]}. */
	public static int blockToLocal(int block) {
		return block & CUBE_MASK;
	}

	public static int cubeToMinBlock(int cube) {
		return cube << CUBE_SHIFT;
	}

	public static int cubeToMaxBlock(int cube) {
		return (cube << CUBE_SHIFT) | CUBE_MASK;
	}

	public static int cubeToCenterBlock(int cube) {
		return (cube << CUBE_SHIFT) | (CUBE_SIZE >> 1);
	}

	/** Block coordinate of {@code local} inside the given cube; {@code local} is masked to {@code [0, 15]}. */
	public static int localToBlock(int cube, int local) {
		return (cube << CUBE_SHIFT) | (local & CUBE_MASK);
	}

	/** Same as {@link #blockToCube(int)}; chunk X/Z and cube X/Z share a coordinate. */
	public static int blockToChunk(int block) {
		return block >> CUBE_SHIFT;
	}

	public static int chunkToMinBlock(int chunk) {
		return chunk << CUBE_SHIFT;
	}
}
