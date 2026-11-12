package net.iskaa303.dividedchunks.core.client;

import net.iskaa303.dividedchunks.api.util.Coords;
import net.minecraft.core.SectionPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The chunk-border overlay, drawn over cubes with vanilla's line set: red 3x3 lattice, blue camera
 * chunk corners, yellow two-block grid, teal cube boundaries, and the camera's own cube on top of
 * everything. Vertical span is the render distance, so columns end at the last loaded cube.
 */
public final class CubeDebugBorders {

	/** Vanilla's 3x3 chunk grid. */
	static final int LATTICE_COLOR = ARGB.color(255, 200, 0, 0);
	/** Vanilla's major lines: the camera chunk's own corners. */
	static final int CORNER_COLOR = ARGB.color(255, 96, 96, 255);
	/** Vanilla's cell border, here only on cube boundaries. */
	static final int CUBE_COLOR = ARGB.color(255, 0, 200, 200);
	/** Vanilla's block grid. */
	static final int GRID_COLOR = ARGB.color(255, 255, 255, 0);
	/** The cube the camera stands in. */
	static final int CAMERA_COLOR = ARGB.color(255, 40, 90, 255);

	static final float COLUMN_WIDTH = 4.0F;
	static final float CUBE_WIDTH = 2.0F;
	static final float GRID_WIDTH = 1.0F;
	static final float CAMERA_WIDTH = 3.0F;

	/** Cubes drawn to each side of the camera's own column; vanilla draws one chunk either way. */
	private static final int RADIUS = 1;

	private CubeDebugBorders() {
	}

	/** One lattice line in world coordinates. */
	record Line(double x1, double y1, double z1, double x2, double y2, double z2, int color, float width) {
	}

	/** Draws the whole overlay around {@code camera}, spanning {@code halfHeight} cubes up and down. */
	public static void emit(SectionPos camera, int halfHeight) {
		for (Line line : lines(camera, halfHeight)) {
			draw(line);
		}
		Gizmos.cuboid(cameraCube(camera), GizmoStyle.stroke(CAMERA_COLOR, CAMERA_WIDTH)).setAlwaysOnTop();
	}

	/**
	 * Every line, block grid first: at a distance the two-block grid is sub-pixel and lands on the cube
	 * boundaries, and the lines drawn last are the ones that survive the depth test there.
	 */
	static List<Line> lines(SectionPos camera, int halfHeight) {
		List<Line> lines = new ArrayList<>(blockGrid(camera, halfHeight));
		lines.addAll(lattice(camera, halfHeight));
		return lines;
	}

	/** The chunk columns and the cube boundaries of the 3x3 cubes around the camera. */
	static List<Line> lattice(SectionPos camera, int halfHeight) {
		int cells = 2 * RADIUS + 1;
		int grid = cells + 1;
		int size = Coords.CUBE_SIZE;
		int half = Math.max(1, halfHeight);
		int minX = camera.minBlockX() - RADIUS * size;
		int minZ = camera.minBlockZ() - RADIUS * size;
		int bottomSection = camera.y() - half;
		int topSection = camera.y() + half;
		double bottom = (double) bottomSection * size;
		double top = (double) (topSection + 1) * size;
		double span = (double) cells * size;

		List<Line> lines = new ArrayList<>(grid * grid + (2 * half + 2) * grid * 2);
		for (int i = 0; i < grid; i++) {
			for (int j = 0; j < grid; j++) {
				double x = minX + (double) i * size;
				double z = minZ + (double) j * size;
				lines.add(new Line(x, bottom, z, x, top, z, LATTICE_COLOR, COLUMN_WIDTH));
			}
		}
		for (int sectionY = bottomSection; sectionY <= topSection + 1; sectionY++) {
			double y = (double) sectionY * size;
			for (int i = 0; i < grid; i++) {
				double offset = (double) i * size;
				lines.add(new Line(minX + offset, y, minZ, minX + offset, y, minZ + span, CUBE_COLOR, CUBE_WIDTH));
				lines.add(new Line(minX, y, minZ + offset, minX + span, y, minZ + offset, CUBE_COLOR, CUBE_WIDTH));
			}
		}
		// Vanilla marks the camera's own chunk with major lines, so it stays findable in the lattice.
		for (double x : new double[] {camera.minBlockX(), camera.minBlockX() + size}) {
			for (double z : new double[] {camera.minBlockZ(), camera.minBlockZ() + size}) {
				lines.add(new Line(x, bottom, z, x, top, z, CORNER_COLOR, COLUMN_WIDTH));
			}
		}
		return lines;
	}

	/** Vanilla's block grid: a line every two blocks up the camera chunk's edges and a ring every two. */
	static List<Line> blockGrid(SectionPos camera, int halfHeight) {		int size = Coords.CUBE_SIZE;
		int half = Math.max(1, halfHeight);
		double minX = camera.minBlockX();
		double minZ = camera.minBlockZ();
		double maxX = minX + size;
		double maxZ = minZ + size;
		double bottom = (double) (camera.y() - half) * size;
		double top = (double) (camera.y() + half + 1) * size;

		List<Line> lines = new ArrayList<>();
		for (int offset = 2; offset < size; offset += 2) {
			lines.add(grid(minX + offset, bottom, minZ, minX + offset, top, minZ));
			lines.add(grid(minX + offset, bottom, maxZ, minX + offset, top, maxZ));
			lines.add(grid(minX, bottom, minZ + offset, minX, top, minZ + offset));
			lines.add(grid(maxX, bottom, minZ + offset, maxX, top, minZ + offset));
		}
		// The cube boundaries carry the lattice's own horizontals, so the rings stop one step short of them.
		for (double band = bottom; band < top; band += size) {
			for (double y = band + 2; y < band + size; y += 2) {
				lines.add(grid(minX, y, minZ, maxX, y, minZ));
				lines.add(grid(minX, y, maxZ, maxX, y, maxZ));
				lines.add(grid(minX, y, minZ, minX, y, maxZ));
				lines.add(grid(maxX, y, minZ, maxX, y, maxZ));
			}
		}
		return lines;
	}

	/** The cube the camera stands in, drawn over everything. */
	static AABB cameraCube(SectionPos camera) {
		return new AABB(camera.minBlockX(), camera.minBlockY(), camera.minBlockZ(),
				camera.maxBlockX() + 1, camera.maxBlockY() + 1, camera.maxBlockZ() + 1);
	}

	private static Line grid(double x1, double y1, double z1, double x2, double y2, double z2) {
		return new Line(x1, y1, z1, x2, y2, z2, GRID_COLOR, GRID_WIDTH);
	}

	private static void draw(Line line) {
		Gizmos.line(new Vec3(line.x1(), line.y1(), line.z1()), new Vec3(line.x2(), line.y2(), line.z2()),
				line.color(), line.width());
	}
}
