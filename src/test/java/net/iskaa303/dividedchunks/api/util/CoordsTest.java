package net.iskaa303.dividedchunks.api.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CoordsTest {

	@Test
	void blockToCubeRoundsTowardNegativeInfinity() {
		assertEquals(0, Coords.blockToCube(0));
		assertEquals(0, Coords.blockToCube(15));
		assertEquals(1, Coords.blockToCube(16));
		assertEquals(-1, Coords.blockToCube(-1));
		assertEquals(-1, Coords.blockToCube(-16));
		assertEquals(-2, Coords.blockToCube(-17));
	}

	@Test
	void blockToLocalIsAlwaysInRange() {
		for (int block = -64; block <= 64; block++) {
			int local = Coords.blockToLocal(block);
			assertEquals(block, Coords.localToBlock(Coords.blockToCube(block), local),
					"round trip failed at block " + block);
			assertTrue(local >= 0 && local <= 15,
					"local out of range at block " + block + ": " + local);
		}
	}

	@Test
	void cubeBoundsAreContiguous() {
		for (int cube = -4; cube <= 4; cube++) {
			int min = Coords.cubeToMinBlock(cube);
			int max = Coords.cubeToMaxBlock(cube);
			assertEquals(min, Coords.cubeToMinBlock(cube));
			assertEquals(min + 15, max);
			// The next cube starts exactly where this one ends.
			assertEquals(max + 1, Coords.cubeToMinBlock(cube + 1));
		}
	}

	@Test
	void centerIsInsideTheCube() {
		for (int cube = -4; cube <= 4; cube++) {
			int center = Coords.cubeToCenterBlock(cube);
			assertTrue(center > Coords.cubeToMinBlock(cube)
					&& center < Coords.cubeToMaxBlock(cube));
		}
	}

	@Test
	void handlesTheDefaultWorldExtremes() {
		int extreme = 30_000_000;
		assertEquals(extreme, Coords.cubeToMinBlock(Coords.blockToCube(extreme)));
		assertEquals(-extreme, Coords.cubeToMinBlock(Coords.blockToCube(-extreme)));
		// Any block near the border, aligned or not, must reconstruct itself.
		for (int block : new int[] {extreme - 1, extreme, extreme + 1, -extreme - 1, -extreme, -extreme + 1}) {
			assertEquals(block, Coords.localToBlock(Coords.blockToCube(block), Coords.blockToLocal(block)));
		}
	}

	@Test
	void cubePosFromBlockAndAccessors() {
		CubePos pos = CubePos.fromBlock(-17, 1_000_000, 31);
		assertEquals(-2, pos.x());
		assertEquals(62_500, pos.y());
		assertEquals(1, pos.z());
		assertEquals(-32, pos.minBlockX());
		assertEquals(1_000_000, pos.minBlockY());
		assertEquals(16, pos.minBlockZ());
		assertEquals(-17, pos.maxBlockX());
		assertEquals(1_000_015, pos.maxBlockY());
		assertEquals(31, pos.maxBlockZ());
	}

	@Test
	void cubePosNeighbours() {
		CubePos pos = new CubePos(1, 2, 3);
		assertEquals(new CubePos(1, 3, 3), pos.above());
		assertEquals(new CubePos(1, 1, 3), pos.below());
		assertEquals(new CubePos(4, 5, 6), pos.offset(3, 3, 3));
	}

	@Test
	void recordEqualityAndHash() {
		assertEquals(new CubePos(1, 2, 3), new CubePos(1, 2, 3));
		assertEquals(new CubePos(1, 2, 3).hashCode(), new CubePos(1, 2, 3).hashCode());
	}
}
