package net.iskaa303.dividedchunks.core.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CubeTrackerTest {

	@Test
	void windowFollowsThePlayer() {
		assertEquals(-3, CubeTracker.windowMin(2, 5, -100));
		assertEquals(7, CubeTracker.windowMax(2, 5, 100));
	}

	@Test
	void windowStopsAtTheDimensionBounds() {
		assertEquals(-100, CubeTracker.windowMin(-98, 5, -100));
		assertEquals(100, CubeTracker.windowMax(98, 5, 100));
	}

	/** A player outside the stored range still gets a window, so their own section is always offered. */
	@Test
	void windowHoldsThePlayersOwnSectionOutsideTheBounds() {
		int min = CubeTracker.windowMin(-200, 5, -100);
		int max = CubeTracker.windowMax(-200, 5, 100);
		assertTrue(min <= -200 && max >= -200);
		assertTrue(CubeTracker.windowMin(200, 5, -100) <= 200);
		assertTrue(CubeTracker.windowMax(200, 5, 100) >= 200);
	}
}
