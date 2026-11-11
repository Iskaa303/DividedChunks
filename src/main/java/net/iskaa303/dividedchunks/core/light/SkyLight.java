package net.iskaa303.dividedchunks.core.light;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.Arrays;

/**
 * Sky light of one 16^3 section: distance to the nearest column open to the sky, so cover dims by
 * distance and only an enclosed volume is dark. Light also falls, so it reaches under a cover from a
 * lit cell above instead of needing an opening at the same height.
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

	/**
	 * Light for the section starting at {@code baseY}, indexed {@code localY << 8 | localZ << 4 | localX}.
	 * {@code above} is the bottom row of the section above when it is already lit; its light falls in here.
	 */
	public static byte[] compute(int minX, int minZ, int baseY, int blocksPerLevel, Tops tops, Blocks blocks,
			byte[] above) {
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
		IntArrayList[] buckets = null;
		int[] carried = new int[256];
		if (above != null) {
			for (int i = 0; i < 256; i++) {
				carried[i] = above[i] & 0xFF;
			}
		}
		for (int localY = 15; localY >= 0; localY--) {
			int worldY = baseY + localY;
			int levelIndex = localY << 8;
			int open = 0;
			for (int i = 0; i < 256; i++) {
				if (ownTops[i] < worldY) {
					open++;
				}
			}
			if (open == 256) {
				for (int i = 0; i < 256; i++) {
					light[levelIndex | i] = FULL;
				}
				continue;
			}
			if (topGrid == null) {
				topGrid = new int[width * width];
				distance = new int[width * width];
				buckets = new IntArrayList[reach + 1];
				for (int z = 0; z < width; z++) {
					for (int x = 0; x < width; x++) {
						topGrid[z * width + x] = tops.at(minX + x - reach, minZ + z - reach);
					}
				}
			}
			flood(topGrid, width, reach, minX, minZ, worldY, blocksPerLevel, blocks, distance, buckets, carried);
			for (int z = 0; z < 16; z++) {
				for (int x = 0; x < 16; x++) {
					int d = distance[(z + reach) * width + x + reach];
					byte value = d < 0 || d / blocksPerLevel >= 15 ? 0 : (byte) (15 - d / blocksPerLevel);
					light[levelIndex | z << 4 | x] = value;
					carried[z << 4 | x] = value;
				}
			}
		}
		return light;
	}

	/** Air distance from every column open at {@code worldY} and from the light falling in from above. */
	private static void flood(int[] topGrid, int width, int reach, int minX, int minZ, int worldY,
			int blocksPerLevel, Blocks blocks, int[] distance, IntArrayList[] buckets, int[] carried) {
		Arrays.fill(distance, -1);
		for (IntArrayList bucket : buckets) {
			if (bucket != null) {
				bucket.clear();
			}
		}
		for (int index = 0; index < topGrid.length; index++) {
			if (topGrid[index] < worldY) {
				distance[index] = 0;
				bucket(buckets, 0).add(index);
			}
		}
		for (int z = 0; z < 16; z++) {
			for (int x = 0; x < 16; x++) {
				int value = carried[z << 4 | x];
				if (value <= 0) {
					continue;
				}
				int index = (z + reach) * width + x + reach;
				if (distance[index] >= 0 || blocks.opaque(minX + x, worldY, minZ + z)) {
					continue;
				}
				int d = (15 - value) * blocksPerLevel;
				distance[index] = d;
				bucket(buckets, d).add(index);
			}
		}
		for (int d = 0; d < reach; d++) {
			IntArrayList bucket = buckets[d];
			if (bucket == null) {
				continue;
			}
			for (int i = 0; i < bucket.size(); i++) {
				int index = bucket.getInt(i);
				if (distance[index] != d) {
					continue;
				}
				int x = index % width;
				if (x > 0) {
					visit(index - 1, d, width, reach, minX, minZ, worldY, blocks, distance, buckets);
				}
				if (x < width - 1) {
					visit(index + 1, d, width, reach, minX, minZ, worldY, blocks, distance, buckets);
				}
				if (index >= width) {
					visit(index - width, d, width, reach, minX, minZ, worldY, blocks, distance, buckets);
				}
				if (index + width < topGrid.length) {
					visit(index + width, d, width, reach, minX, minZ, worldY, blocks, distance, buckets);
				}
			}
		}
	}

	private static void visit(int index, int from, int width, int reach, int minX, int minZ, int worldY,
			Blocks blocks, int[] distance, IntArrayList[] buckets) {
		int next = from + 1;
		if (next > reach || (distance[index] >= 0 && distance[index] <= next)) {
			return;
		}
		int worldX = minX + index % width - reach;
		int worldZ = minZ + index / width - reach;
		if (blocks.opaque(worldX, worldY, worldZ)) {
			return;
		}
		distance[index] = next;
		bucket(buckets, next).add(index);
	}

	private static IntArrayList bucket(IntArrayList[] buckets, int distance) {
		IntArrayList bucket = buckets[distance];
		if (bucket == null) {
			bucket = buckets[distance] = new IntArrayList();
		}
		return bucket;
	}

}
