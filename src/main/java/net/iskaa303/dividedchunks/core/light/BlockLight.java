package net.iskaa303.dividedchunks.core.light;

import it.unimi.dsi.fastutil.ints.IntArrayList;

/**
 * Block light of one 16^3 section: a flood from the emitters within one reach, losing one level per
 * block of air and one per dampening, which is vanilla's rule.
 */
public final class BlockLight {

	public static final int MAX = 15;
	/** A cell of the padded domain: how much light a step into it costs. */
	public interface Cells {
		int dampening(int worldX, int worldY, int worldZ);
	}

	private BlockLight() {
	}

	/**
	 * Light for the section starting at {@code minX}/{@code minY}/{@code minZ}, indexed
	 * {@code localY << 8 | localZ << 4 | localX}. {@code emitters} packs {@code x, y, z, level} per
	 * emitter; the ones outside the padded domain are ignored.
	 */
	public static byte[] compute(int minX, int minY, int minZ, int[] emitters, Cells cells) {
		byte[] light = new byte[4096];
		if (emitters.length == 0) {
			// A dark section costs nothing, which is nearly all of them.
			return light;
		}
		int size = 16 + 2 * MAX;
		int area = size * size;
		byte[] level = new byte[area * size];
		IntArrayList[] buckets = new IntArrayList[MAX + 1];
		int originX = minX - MAX;
		int originY = minY - MAX;
		int originZ = minZ - MAX;
		for (int i = 0; i + 3 < emitters.length; i += 4) {
			int x = emitters[i] - originX;
			int y = emitters[i + 1] - originY;
			int z = emitters[i + 2] - originZ;
			int emission = Math.min(MAX, emitters[i + 3]);
			if (x < 0 || x >= size || y < 0 || y >= size || z < 0 || z >= size || emission <= 0) {
				continue;
			}
			int index = y * area + z * size + x;
			if (emission > level[index]) {
				level[index] = (byte) emission;
				bucket(buckets, emission).add(index);
			}
		}
		for (int value = MAX; value > 0; value--) {
			IntArrayList bucket = buckets[value];
			if (bucket == null) {
				continue;
			}
			for (int i = 0; i < bucket.size(); i++) {
				int index = bucket.getInt(i);
				if (level[index] != value) {
					continue;
				}
				int x = index % size;
				int z = index / size % size;
				int y = index / area;
				if (x > 0) {
					spread(level, buckets, index - 1, value, originX, originY, originZ, size, cells);
				}
				if (x < size - 1) {
					spread(level, buckets, index + 1, value, originX, originY, originZ, size, cells);
				}
				if (z > 0) {
					spread(level, buckets, index - size, value, originX, originY, originZ, size, cells);
				}
				if (z < size - 1) {
					spread(level, buckets, index + size, value, originX, originY, originZ, size, cells);
				}
				if (y > 0) {
					spread(level, buckets, index - area, value, originX, originY, originZ, size, cells);
				}
				if (y < size - 1) {
					spread(level, buckets, index + area, value, originX, originY, originZ, size, cells);
				}
			}
		}
		for (int localY = 0; localY < 16; localY++) {
			for (int localZ = 0; localZ < 16; localZ++) {
				for (int localX = 0; localX < 16; localX++) {
					int index = (localY + MAX) * area + (localZ + MAX) * size + localX + MAX;
					light[localY << 8 | localZ << 4 | localX] = level[index];
				}
			}
		}
		return light;
	}

	private static void spread(byte[] level, IntArrayList[] buckets, int index, int from, int originX, int originY,
			int originZ, int size, Cells cells) {
		int area = size * size;
		int x = index % size;
		int z = index / size % size;
		int y = index / area;
		int cost = Math.max(1, cells.dampening(originX + x, originY + y, originZ + z));
		int next = from - cost;
		if (next <= level[index]) {
			return;
		}
		level[index] = (byte) next;
		if (next > 0) {
			bucket(buckets, next).add(index);
		}
	}

	private static IntArrayList bucket(IntArrayList[] buckets, int value) {
		IntArrayList bucket = buckets[value];
		if (bucket == null) {
			bucket = buckets[value] = new IntArrayList();
		}
		return bucket;
	}
}
