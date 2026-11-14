package net.iskaa303.dividedchunks.core.client;

/**
 * Rotation of a section storage grid along its vertical axis. The grid has no vertical wrapping, so a
 * band that has to follow the camera is otherwise rebuilt from scratch, which drops every mesh in it.
 */
public final class BandRotation {

	private BandRotation() {
	}

	/**
	 * Rotates every column of {@code nodes} by {@code delta} sections, so the value at section
	 * {@code y} becomes the one from {@code y + delta}. The grid is laid out
	 * {@code (z * height + y) * sizeXZ + x}, so one column's sections sit every {@code sizeXZ} entries.
	 */
	public static void rotate(Object[] nodes, int height, int sizeXZ, int delta) {
		if (delta == 0) {
			return;
		}
		Object[] column = new Object[height];
		for (int x = 0; x < sizeXZ; x++) {
			for (int z = 0; z < sizeXZ; z++) {
				int base = z * height * sizeXZ + x;
				for (int index = 0; index < height; index++) {
					column[index] = nodes[base + Math.floorMod(index + delta, height) * sizeXZ];
				}
				for (int index = 0; index < height; index++) {
					nodes[base + index * sizeXZ] = column[index];
				}
			}
		}
	}
}
