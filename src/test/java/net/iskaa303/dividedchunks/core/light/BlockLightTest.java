package net.iskaa303.dividedchunks.core.light;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BlockLightTest {

	private static final BlockLight.Cells OPEN = (x, y, z) -> 0;
	private static final BlockLight.Cells STONE = (x, y, z) -> 15;

	private static int at(byte[] light, int x, int y, int z) {
		return light[y << 8 | z << 4 | x] & 0xFF;
	}

	@Test
	void noEmittersIsDark() {
		byte[] light = BlockLight.compute(0, 0, 0, new int[0], OPEN);
		for (byte value : light) {
			assertEquals(0, value & 0xFF);
		}
	}

	/** A lamp loses one level per block of air, which is vanilla's rule. */
	@Test
	void aLampFadesWithDistance() {
		byte[] light = BlockLight.compute(0, 0, 0, new int[] {8, 8, 8, 15}, OPEN);
		assertEquals(15, at(light, 8, 8, 8));
		assertEquals(14, at(light, 9, 8, 8));
		assertEquals(13, at(light, 10, 8, 8));
		assertEquals(8, at(light, 15, 8, 8));
	}

	/** Stone costs a full step, so a lamp lights only through open cells. */
	@Test
	void wallsBlockTheFlood() {
		// Every cell outside the middle of the section is stone.
		BlockLight.Cells walls = (x, y, z) -> x == 8 && z == 8 ? 0 : 15;
		byte[] light = BlockLight.compute(0, 0, 0, new int[] {8, 8, 8, 15}, walls);
		assertEquals(15, at(light, 8, 8, 8));
		assertEquals(0, at(light, 8, 8, 10));
	}

	/** A lamp in the neighbouring section still lights this one. */
	@Test
	void aLampOutsideTheSectionReachesIn() {
		byte[] light = BlockLight.compute(0, 0, 0, new int[] {8, 21, 8, 15}, OPEN);
		assertEquals(15 - 6, at(light, 8, 15, 8));
	}

	/** Emitters beyond the reach of the section are ignored entirely. */
	@Test
	void farEmittersCostNothing() {
		byte[] light = BlockLight.compute(0, 0, 0, new int[] {1000, 1000, 1000, 15}, OPEN);
		for (byte value : light) {
			assertEquals(0, value & 0xFF);
		}
	}

	/** The widest case has to stay affordable: the render thread queries this per section. */
	@Test
	void aLitSectionStaysCheap() {
		long start = System.nanoTime();
		for (int i = 0; i < 20; i++) {
			BlockLight.compute(0, 0, 0, new int[] {8, 8, 8, 15}, OPEN);
		}
		long millis = (System.nanoTime() - start) / 1_000_000;
		assertTrue(millis < 2_000, "20 lit sections took " + millis + " ms");
	}
}
