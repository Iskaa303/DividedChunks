package net.iskaa303.dividedchunks.core.client;

import net.iskaa303.dividedchunks.core.client.CubeDebugBorders.Line;
import net.iskaa303.dividedchunks.core.config.DividedChunksConfig;
import net.minecraft.core.SectionPos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CubeDebugBordersTest {

	private static final int SIZE = 16;

	@Test
	void latticeCoversThreeCubesPerAxisAroundTheCamera() {
		List<Line> lines = CubeDebugBorders.lattice(SectionPos.of(0, 64, 0), 2);
		List<Line> columns = lines.stream().filter(line -> line.y1() != line.y2()).toList();

		assertEquals(16, columns.size(), "one vertical per grid crossing of 3x3 cubes");
		for (Line column : columns) {
			assertTrue(column.x1() >= -SIZE && column.x1() <= 2 * SIZE, "column x out of the 3x3 box");
			assertTrue(column.z1() >= -SIZE && column.z1() <= 2 * SIZE, "column z out of the 3x3 box");
		}
	}

	@Test
	void columnsStopAtTheRenderDistance() {
		List<Line> lines = CubeDebugBorders.lattice(SectionPos.of(0, 64, 0), 3);

		for (Line line : lines) {
			double low = Math.min(line.y1(), line.y2());
			double high = Math.max(line.y1(), line.y2());
			assertTrue(low >= 61.0 * SIZE, "line reaches below the render distance");
			assertTrue(high <= 68.0 * SIZE, "line reaches above the render distance");
		}
	}

	@Test
	void cubeLinesSitOnSectionBoundaries() {
		List<Line> lines = CubeDebugBorders.lattice(SectionPos.of(0, 64, 0), 1);
		List<Line> cubes = lines.stream().filter(line -> line.y1() == line.y2()).toList();

		assertEquals(2 * (2 * 1 + 2) * 4, cubes.size(), "eight grid lines on each of the four boundaries");
		for (Line cube : cubes) {
			assertEquals(0.0, cube.y1() % SIZE, "a cube boundary is not a multiple of 16");
		}
	}

	/** A zero render distance still draws the camera's own cube, just no cubes beside it. */
	@Test
	void latticeHoldsTheCameraSectionWithAZeroRenderDistance() {
		int minSectionY = DividedChunksConfig.MIN_ALLOWED_Y >> 4;
		List<Line> lines = CubeDebugBorders.lattice(SectionPos.of(0, minSectionY, 0), 0);

		assertTrue(lines.stream().filter(line -> line.y1() != line.y2())
				.allMatch(line -> line.y1() <= (double) minSectionY * SIZE && line.y2() > (double) minSectionY * SIZE));
	}
}
