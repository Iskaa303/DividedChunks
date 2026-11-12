package net.iskaa303.dividedchunks.core.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CubeWindowTest {

	@Test
	void alwaysHoldsTheDrawnCube() {
		for (int radius = 1; radius <= 32; radius++) {
			for (int sectionY = -900; sectionY <= 900; sectionY += 7) {
				CubeWindow window = CubeWindow.around(sectionY, radius);
				assertTrue(window.holds(sectionY), "player " + sectionY + " outside r=" + radius);
				assertTrue(window.min() <= sectionY - radius, "cube below the window at r=" + radius);
				assertTrue(window.max() >= sectionY + radius, "cube above the window at r=" + radius);
			}
		}
	}

	@Test
	void movesOnlyOncePerBucket() {
		int radius = 8;
		int moves = 0;
		CubeWindow previous = CubeWindow.around(0, radius);
		for (int sectionY = 0; sectionY < 160; sectionY++) {
			CubeWindow window = CubeWindow.around(sectionY, radius);
			if (!window.equals(previous)) {
				moves++;
				previous = window;
			}
		}
		// A bucket of 32 sections: four boundaries in 160 sections, however the player moves inside them.
		assertEquals(4, moves);
	}

	@Test
	void negativeHeightsUseTheSameBuckets() {
		CubeWindow window = CubeWindow.around(-33, 8);
		assertTrue(window.holds(-33));
		assertTrue(window.min() < -33 && window.max() > -33);
	}

	@Test
	void heightGrowsWithTheRenderDistanceButIsCapped() {
		assertEquals(20, CubeWindow.height(2), "small radius gets the minimum bucket");
		assertEquals(48, CubeWindow.height(8));
		assertEquals(64, CubeWindow.height(16));
		assertTrue(CubeWindow.height(32) < 96, "the largest render distance is still bounded");
		assertTrue(CubeWindow.height(32) > 2 * 32, "and still holds the 65 section cube");
	}
}
