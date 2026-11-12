package net.iskaa303.dividedchunks.core.client;

import net.iskaa303.dividedchunks.core.client.CubeDebugBorders.Line;
import net.iskaa303.dividedchunks.core.config.DividedChunksConfig;
import net.minecraft.core.SectionPos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CubeDebugBordersTest {

	private static final int SIZE = 16;

	private static List<Line> verticals(List<Line> lines, int color) {
		return lines.stream().filter(line -> line.y1() != line.y2() && line.color() == color).toList();
	}

	private static List<Line> rings(List<Line> lines) {
		return lines.stream().filter(line -> line.y1() == line.y2()).toList();
	}

	@Test
	void latticeCoversThreeCubesPerAxisAroundTheCamera() {
		List<Line> columns = verticals(CubeDebugBorders.lattice(SectionPos.of(0, 64, 0), 2), CubeDebugBorders.LATTICE_COLOR);

		assertEquals(16, columns.size(), "one vertical per grid crossing of 3x3 cubes");
		for (Line column : columns) {
			assertTrue(column.x1() >= -SIZE && column.x1() <= 2 * SIZE, "column x out of the 3x3 box");
			assertTrue(column.z1() >= -SIZE && column.z1() <= 2 * SIZE, "column z out of the 3x3 box");
		}
	}

	@Test
	void theCameraChunkCornersCarryVanillasMajorLines() {
		List<Line> corners = verticals(CubeDebugBorders.lattice(SectionPos.of(0, 64, 0), 2), CubeDebugBorders.CORNER_COLOR);

		assertEquals(4, corners.size(), "the camera chunk's four corners");
		for (Line corner : corners) {
			assertTrue(corner.x1() == 0.0 || corner.x1() == SIZE, "corner x is not on the camera chunk");
			assertTrue(corner.z1() == 0.0 || corner.z1() == SIZE, "corner z is not on the camera chunk");
		}
	}

	@Test
	void columnsStopAtTheRenderDistance() {
		for (Line line : CubeDebugBorders.lattice(SectionPos.of(0, 64, 0), 3)) {
			double low = Math.min(line.y1(), line.y2());
			double high = Math.max(line.y1(), line.y2());
			assertTrue(low >= 61.0 * SIZE, "line reaches below the render distance");
			assertTrue(high <= 68.0 * SIZE, "line reaches above the render distance");
		}
	}

	@Test
	void cubeLinesSitOnSectionBoundaries() {
		List<Line> cubes = rings(CubeDebugBorders.lattice(SectionPos.of(0, 64, 0), 1));

		assertEquals(2 * (2 * 1 + 2) * 4, cubes.size(), "eight grid lines on each of the four boundaries");
		for (Line cube : cubes) {
			assertEquals(0.0, cube.y1() % SIZE, "a cube boundary is not a multiple of 16");
			assertEquals(CubeDebugBorders.CUBE_COLOR, cube.color(), "only cube boundaries may use the cell colour");
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

	@Test
	void blockGridStaysInTheCameraChunk() {
		SectionPos camera = SectionPos.of(32, 64, -48);
		List<Line> grid = CubeDebugBorders.blockGrid(camera, 2);

		assertTrue(!grid.isEmpty());
		for (Line line : grid) {
			for (double x : new double[] {line.x1(), line.x2()}) {
				assertTrue(x >= camera.minBlockX() && x <= camera.maxBlockX() + 1, "x " + x + " left the chunk");
			}
			for (double z : new double[] {line.z1(), line.z2()}) {
				assertTrue(z >= camera.minBlockZ() && z <= camera.maxBlockZ() + 1, "z " + z + " left the chunk");
			}
		}
	}

	@Test
	void blockGridStopsAtTheRenderDistance() {
		SectionPos camera = SectionPos.of(0, 64, 0);

		for (Line line : CubeDebugBorders.blockGrid(camera, 3)) {
			assertTrue(Math.min(line.y1(), line.y2()) >= 61.0 * SIZE, "grid reaches below the render distance");
			assertTrue(Math.max(line.y1(), line.y2()) <= 68.0 * SIZE, "grid reaches above the render distance");
		}
	}

	@Test
	void blockGridRunsEveryTwoBlocks() {
		SectionPos camera = SectionPos.of(0, 64, 0);
		List<Line> grid = CubeDebugBorders.blockGrid(camera, 1);

		assertEquals(7 * 4, grid.stream().filter(line -> line.y1() != line.y2()).count(), "seven offsets per edge");
		// One ring per two blocks, one step short of the cube boundaries of a three cube span.
		assertEquals(3 * (SIZE / 2 - 1) * 4, rings(grid).size());
		for (double offset : new double[] {2, 6, 14}) {
			assertTrue(grid.stream().anyMatch(line -> line.y1() != line.y2()
					&& (line.x1() - camera.minBlockX() == offset || line.z1() - camera.minBlockZ() == offset)));
		}
		for (Line ring : rings(grid)) {
			assertEquals(0.0, ring.y1() % 2, "a ring sits between the two-block steps");
			assertNotEquals(0.0, ring.y1() % SIZE, "a ring rests on a cube boundary the lattice already draws");
		}
	}

	@Test
	void theBlockGridIsOnlyEverYellow() {
		List<Line> grid = CubeDebugBorders.blockGrid(SectionPos.of(0, 64, 0), 1);

		assertTrue(grid.stream().allMatch(line -> line.color() == CubeDebugBorders.GRID_COLOR));
		assertTrue(grid.stream().allMatch(line -> line.width() == CubeDebugBorders.GRID_WIDTH));
	}

	/** Cube boundaries tell one section from the next, so they outweigh the block grid. */
	@Test
	void cubeBoundariesAreHeavierThanTheBlockGrid() {
		List<Line> lines = CubeDebugBorders.lattice(SectionPos.of(0, 64, 0), 1);
		Line cube = rings(lines).getFirst();
		Line column = lines.stream().filter(line -> line.y1() != line.y2()).findFirst().orElseThrow();

		assertTrue(cube.width() > CubeDebugBorders.GRID_WIDTH);
		assertTrue(cube.color() != CubeDebugBorders.GRID_COLOR);
		assertTrue(cube.width() < column.width(), "a cube line should stay lighter than a column");
	}

	/** The overlay is only fog-free for opaque colours, so nothing may be translucent. */
	@Test
	void everyLineIsOpaque() {
		List<Line> lines = CubeDebugBorders.lattice(SectionPos.of(0, 64, 0), 2);
		List<Line> grid = CubeDebugBorders.blockGrid(SectionPos.of(0, 64, 0), 2);

		assertTrue(lines.stream().allMatch(line -> line.color() >>> 24 == 255));
		assertTrue(grid.stream().allMatch(line -> line.color() >>> 24 == 255));
	}

	/** Where the grid and a cube boundary overlap, the boundary has to be the one left visible. */
	@Test
	void theBlockGridIsDrawnBeforeTheLattice() {
		List<Line> lines = CubeDebugBorders.lines(SectionPos.of(0, 64, 0), 2);
		int firstLattice = -1;
		int lastGrid = -1;

		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i).color() == CubeDebugBorders.GRID_COLOR) {
				lastGrid = i;
			} else if (firstLattice < 0) {
				firstLattice = i;
			}
		}
		assertTrue(lastGrid < firstLattice, "a lattice line is drawn before a grid line and can be covered");
	}

	@Test
	void theCameraCubeIsTheSectionItStandsIn() {
		var cube = CubeDebugBorders.cameraCube(SectionPos.of(32, 64, -48));

		assertEquals(32 * SIZE, cube.minX);
		assertEquals(64 * SIZE, cube.minY);
		assertEquals(-48 * SIZE, cube.minZ);
		assertEquals(33 * SIZE, cube.maxX);
		assertEquals(65 * SIZE, cube.maxY);
		assertEquals(-47 * SIZE, cube.maxZ);
	}
}
