package net.iskaa303.dividedchunks.core.light;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SkyLightTest {

	/** Open sky: every cell of the section is fully lit. */
	@Test
	void openSkyIsFull() {
		byte[] light = SkyLight.compute(0, 0, 0, 1, (x, z) -> Integer.MIN_VALUE, (x, y, z) -> false, null);
		for (byte value : light) {
			assertEquals(SkyLight.FULL, value & 0xFF);
		}
	}

	/** A roof over every column leaves nothing lit. */
	@Test
	void fullRoofIsDark() {
		byte[] light = SkyLight.compute(0, 0, 0, 1, (x, z) -> 64, (x, y, z) -> false, null);
		for (byte value : light) {
			assertEquals(0, value & 0xFF);
		}
	}

	/** One open column lights the cells around it and fades with distance. */
	@Test
	void aShaftLightsOutwards() {
		byte[] light = SkyLight.compute(0, 0, 0, 1, (x, z) -> x == 8 && z == 8 ? Integer.MIN_VALUE : 64,
				(x, y, z) -> false, null);
		assertEquals(SkyLight.FULL, light[8 << 4 | 8] & 0xFF);
		assertEquals(14, light[8 << 4 | 9] & 0xFF);
		assertEquals(12, light[8 << 4 | 11] & 0xFF);
	}

	/** Light falling in from the section above lights a covered cell that no opening at its own height reaches. */
	@Test
	void lightFallsInFromAbove() {
		byte[] above = new byte[4096];
		above[3 << 4 | 3] = 15;
		byte[] light = SkyLight.compute(0, 0, 0, 1, (x, z) -> 64, (x, y, z) -> false, above);
		assertEquals(15, light[15 << 8 | 3 << 4 | 3] & 0xFF, "the cell under the shaft is fully lit");
		assertTrue((light[14 << 8 | 3 << 4 | 3] & 0xFF) > 0, "and the light keeps falling");
		assertEquals(0, light[14 << 8 | 12 << 4 | 12] & 0xFF, "a far cell with no path stays dark");
	}

	/** Walling off the falling light leaves the section dark again. */
	@Test
	void aCeilingStopsTheFallingLight() {
		byte[] above = new byte[4096];
		above[3 << 4 | 3] = 15;
		byte[] light = SkyLight.compute(0, 0, 0, 1, (x, z) -> 64, (x, y, z) -> y == 15, above);
		assertEquals(0, light[14 << 8 | 3 << 4 | 3] & 0xFF);
	}

	/** A section that is open everywhere ignores the light coming from above. */
	@Test
	void openSectionIgnoresWhatFallsIn() {
		byte[] above = new byte[4096];
		byte[] light = SkyLight.compute(0, 0, 0, 1, (x, z) -> Integer.MIN_VALUE, (x, y, z) -> false, above);
		for (byte value : light) {
			assertEquals(SkyLight.FULL, value & 0xFF);
		}
	}

	/** The render thread queries this per section, so the widest reach has to stay affordable. */
	@Test
	void maximalReachStaysCheap() {
		int blocksPerLevel = SkyLight.blocksPerLevel(80);
		assertEquals(5, blocksPerLevel);
		long start = System.nanoTime();
		for (int i = 0; i < 20; i++) {
			SkyLight.compute(0, 0, 0, blocksPerLevel, (x, z) -> x == 8 && z == 8 ? Integer.MIN_VALUE : 64,
					(x, y, z) -> false, null);
		}
		long millis = (System.nanoTime() - start) / 1_000_000;
		assertTrue(millis < 2_000, "20 sections at the widest reach took " + millis + " ms");
	}
}
