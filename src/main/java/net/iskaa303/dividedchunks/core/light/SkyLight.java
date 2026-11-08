package net.iskaa303.dividedchunks.core.light;

import java.util.Arrays;

/**
 * Sky light of one 16^3 section: the air-path distance to the nearest column open to the sky at that
 * height, so cover dims by distance and only an enclosed volume is dark.
 */
public final class SkyLight {

	public static final byte FULL = 15;

	/** Lateral blocks per light level; 1 is vanilla's 15-block reach. */
	public static int blocksPerLevel(int skyConeAngleDegrees) {
		return Math.max(1, Math.min(5, 1 + skyConeAngleDegrees / 15));
	}

	/** Highest sky-damping block of the column at world {@code x}/{@code z}, or {@link Integer#MIN_VALUE}. */
	public interface Tops {
		int at(int worldX, int worldZ);
	}

	/** Whether the block at world {@code x}/{@code y}/{@code z} stops the light. */
	public interface Blocks {
		boolean opaque(int worldX, int worldY, int worldZ);
	}

	private SkyLight() {
	}

	/** Light for the section starting at {@code baseY}, indexed {@code localY << 8 | localZ << 4 | localX}. */
	public static byte[] compute(int minX, int minZ, int baseY, int blocksPerLevel, Tops tops, Blocks blocks) {
		int reach = 15 * blocksPerLevel;
		byte[] light = new byte[4096];
		int[] ownTops = new int[256];
		int highest = Integer.MIN_VALUE;
		for (int z = 0; z < 16; z++) {
			for (int x = 0; x < 16; x++) {
				int top = ownTops[z << 4 | x] = tops.at(minX + x, minZ + z);
				highest = Math.max(highest, top);
			}
		}
		if (baseY > highest) {
			Arrays.fill(light, FULL);
			return light;
		}

		int width = 16 + 2 * reach;
		int[] topGrid = null;
		int[] distance = null;
		int[] queue = null;
		for (int localY = 0; localY < 16; localY++) {
			int worldY = baseY + localY;
			int open = 0;
			for (int i = 0; i < 256; i++) {
				if (ownTops[i] < worldY) {
					open++;
				}
			}
			int levelIndex = localY << 8;
			if (open == 256) {
				for (int i = 0; i < 256; i++) {
					light[levelIndex | i] = FULL;
				}
				continue;
			}
			if (topGrid == null) {
				topGrid = new int[width * width];
				distance = new int[width * width];
				queue = new int[width * width];
				for (int z = 0; z < width; z++) {
					for (int x = 0; x < width; x++) {
						topGrid[z * width + x] = tops.at(minX + x - reach, minZ + z - reach);
					}
				}
			}
			flood(topGrid, width, reach, minX, minZ, worldY, blocks, distance, queue);
			for (int z = 0; z < 16; z++) {
				for (int x = 0; x < 16; x++) {
					int d = distance[(z + reach) * width + x + reach];
					light[levelIndex | z << 4 | x] =
							d < 0 || d / blocksPerLevel >= 15 ? 0 : (byte) (15 - d / blocksPerLevel);
				}
			}
		}
		return light;
	}

	/** Air distance from every column open at {@code worldY}, breadth first and capped at the reach. */
	private static void flood(int[] topGrid, int width, int reach, int minX, int minZ, int worldY, Blocks blocks,
			int[] distance, int[] queue) {
		Arrays.fill(distance, -1);
		int head = 0;
		int tail = 0;
		for (int index = 0; index < topGrid.length; index++) {
			if (topGrid[index] < worldY) {
				distance[index] = 0;
				queue[tail++] = index;
			}
		}
		while (head < tail) {
			int index = queue[head++];
			int d = distance[index];
			if (d >= reach) {
				continue;
			}
			int x = index % width;
			if (x > 0) {
				tail = visit(index - 1, d, width, reach, minX, minZ, worldY, blocks, distance, queue, tail);
			}
			if (x < width - 1) {
				tail = visit(index + 1, d, width, reach, minX, minZ, worldY, blocks, distance, queue, tail);
			}
			if (index >= width) {
				tail = visit(index - width, d, width, reach, minX, minZ, worldY, blocks, distance, queue, tail);
			}
			if (index + width < topGrid.length) {
				tail = visit(index + width, d, width, reach, minX, minZ, worldY, blocks, distance, queue, tail);
			}
		}
	}

	private static int visit(int index, int from, int width, int reach, int minX, int minZ, int worldY, Blocks blocks,
			int[] distance, int[] queue, int tail) {
		if (distance[index] >= 0) {
			return tail;
		}
		int worldX = minX + index % width - reach;
		int worldZ = minZ + index / width - reach;
		if (blocks.opaque(worldX, worldY, worldZ)) {
			return tail;
		}
		distance[index] = from + 1;
		queue[tail] = index;
		return tail + 1;
	}

}
