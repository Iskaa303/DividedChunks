package net.iskaa303.dividedchunks.core.client;

import net.iskaa303.dividedchunks.api.util.Coords;
import net.minecraft.core.SectionPos;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The cube lattice behind the vanilla chunk-border overlay: what vanilla draws for one chunk, drawn
 * for the 3x3 cubes around the camera. Verticals are columns, in vanilla's major-line blue at 4
 * pixels; horizontals are cube boundaries, in the thin teal of vanilla's cell grid, so a column and a
 * cube are told apart by weight. The vertical span is the render distance, so the columns stop at the
 * last cube the client loads instead of running on to infinity.
 */
public final class CubeDebugBorders {

	private static final int COLUMN_COLOR = ARGB.colorFromFloat(1.0F, 0.25F, 0.25F, 1.0F);
	private static final float COLUMN_WIDTH = 4.0F;
	private static final int CUBE_COLOR = ARGB.color(255, 0, 155, 155);
	private static final float CUBE_WIDTH = 1.0F;

	/** Cubes drawn to each side of the camera's own column; vanilla draws one chunk either way. */
	private static final int RADIUS = 1;

	private CubeDebugBorders() {
	}

	/** One lattice line in world coordinates. */
	record Line(double x1, double y1, double z1, double x2, double y2, double z2, int color, float width) {
	}

	/** Draws the lattice around {@code camera}, spanning {@code halfHeight} cubes up and down. */
	public static void emit(SectionPos camera, int halfHeight) {
		for (Line line : lattice(camera, halfHeight)) {
			Gizmos.line(new Vec3(line.x1(), line.y1(), line.z1()), new Vec3(line.x2(), line.y2(), line.z2()),
					line.color(), line.width());
		}
	}

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
				lines.add(new Line(x, bottom, z, x, top, z, COLUMN_COLOR, COLUMN_WIDTH));
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
		return lines;
	}
}
