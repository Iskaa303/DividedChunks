package net.iskaa303.dividedchunks.core.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class BandRotationTest {

	private static final int HEIGHT = 4;
	private static final int SIZE_XZ = 3;

	@Test
	void aValueMovesUpByTheShiftAndStaysInItsColumn() {
		Object[] nodes = grid();
		BandRotation.rotate(nodes, HEIGHT, SIZE_XZ, 1);

		for (int x = 0; x < SIZE_XZ; x++) {
			for (int z = 0; z < SIZE_XZ; z++) {
				for (int y = 0; y < HEIGHT; y++) {
					int fromY = Math.floorMod(y + 1, HEIGHT);
					assertEquals(name(x, fromY, z), nodes[index(x, y, z)], "slot y=" + y + " x=" + x + " z=" + z);
				}
			}
		}
	}

	@Test
	void aDownwardShiftWrapsTheOtherWay() {
		Object[] nodes = grid();
		BandRotation.rotate(nodes, HEIGHT, SIZE_XZ, -1);

		for (int x = 0; x < SIZE_XZ; x++) {
			for (int z = 0; z < SIZE_XZ; z++) {
				for (int y = 0; y < HEIGHT; y++) {
					int fromY = Math.floorMod(y - 1, HEIGHT);
					assertEquals(name(x, fromY, z), nodes[index(x, y, z)]);
				}
			}
		}
	}

	@Test
	void aFullTurnChangesNothing() {
		Object[] nodes = grid();
		Object[] before = nodes.clone();
		BandRotation.rotate(nodes, HEIGHT, SIZE_XZ, HEIGHT);

		for (int index = 0; index < nodes.length; index++) {
			assertSame(before[index], nodes[index]);
		}
	}

	/** The storage's own layout: {@code (z * height + y) * sizeXZ + x}. */
	private static int index(int x, int y, int z) {
		return (z * HEIGHT + y) * SIZE_XZ + x;
	}

	private static String name(int x, int y, int z) {
		return "x" + x + "y" + y + "z" + z;
	}

	private static Object[] grid() {
		Object[] nodes = new Object[HEIGHT * SIZE_XZ * SIZE_XZ];
		for (int x = 0; x < SIZE_XZ; x++) {
			for (int z = 0; z < SIZE_XZ; z++) {
				for (int y = 0; y < HEIGHT; y++) {
					nodes[index(x, y, z)] = name(x, y, z);
				}
			}
		}
		return nodes;
	}
}
