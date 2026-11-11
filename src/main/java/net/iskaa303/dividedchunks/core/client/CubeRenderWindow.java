package net.iskaa303.dividedchunks.core.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;

/**
 * The client's render box: a cube of the vanilla render distance around the player. The window is
 * taller than the cube so it survives vertical movement without a rebuild; the occlusion graph's own
 * cull keeps what is drawn a cube.
 */
public final class CubeRenderWindow {

	/** Diagnoses the cube boundary, which the fog hides: sections culled per second get logged. */
	private static final boolean DEBUG_CULL = Boolean.getBoolean("dividedchunks.debugRender");

	private static int lastMinSectionY = Integer.MIN_VALUE;
	private static int lastMaxSectionY = Integer.MIN_VALUE;
	private static long debugWindowStart;
	private static int debugCulled;

	private CubeRenderWindow() {
	}

	/** Records a frame's culled sections, logging once a second when the render debug flag is set. */
	public static void countCulled(int culled) {
		if (!DEBUG_CULL) {
			return;
		}
		long now = System.nanoTime();
		if (debugWindowStart == 0L) {
			debugWindowStart = now;
		}
		debugCulled += culled;
		if (now - debugWindowStart >= 1_000_000_000L) {
			net.iskaa303.dividedchunks.DividedChunks.LOGGER.info(
					"Cube cull: {} sections outside +-{} of section {} dropped this second", debugCulled, radius(),
					cameraSectionY());
			debugWindowStart = now;
			debugCulled = 0;
		}
	}

	/** Radius of the cube in sections. */
	public static int radius() {
		return Math.max(1, Minecraft.getInstance().options.getEffectiveRenderDistance());
	}

	/** Height of the window in sections, or 0 without a client level. */
	public static int height() {
		if (Minecraft.getInstance().level == null) {
			return 0;
		}
		int cube = 2 * radius() + 1;
		// The occlusion Octree is a cube of this size; a taller window would be partly unvisited.
		return Math.min(Mth.smallestEncompassingPowerOfTwo(cube), 2 * cube);
	}

	/** Section Y of the player, or the middle of the level's band without a player. */
	public static int cameraSectionY() {
		ClientLevel level = Minecraft.getInstance().level;
		return center(level);
	}

	/** Lower bound of the window, recording it; {@code Integer.MIN_VALUE} without a client level. */
	public static int min() {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) {
			return Integer.MIN_VALUE;
		}
		int min = center(level) - height() / 2;
		lastMinSectionY = min;
		lastMaxSectionY = min + height() - 1;
		return min;
	}

	private static int center(ClientLevel level) {
		LocalPlayer player = Minecraft.getInstance().player;
		return player == null
				? (level.getMinSectionY() + level.getMaxSectionY()) / 2
				: SectionPos.blockToSectionCoord(player.blockPosition().getY());
	}

	/** Lower bound for the render band, or {@code vanilla} without a client level. */
	public static int minSectionY(int vanilla) {
		int min = min();
		return min == Integer.MIN_VALUE ? vanilla : min;
	}

	/** Upper bound for the render band, or {@code vanilla} without a client level. */
	public static int maxSectionY(int vanilla) {
		int min = min();
		return min == Integer.MIN_VALUE ? vanilla : lastMaxSectionY;
	}

	/** Lower block bound for the window, or {@code vanilla} without a client level. */
	public static int minBlockY(int vanilla) {
		int min = min();
		return min == Integer.MIN_VALUE ? vanilla : min * 16;
	}

	/** Upper block bound for the window, or {@code vanilla} without a client level. */
	public static int maxBlockY(int vanilla) {
		int min = min();
		return min == Integer.MIN_VALUE ? vanilla : lastMaxSectionY * 16 + 15;
	}

	/**
	 * Lower block bound for the occlusion Octree, which starts its vertical box at the world's bottom
	 * unless the box already spans every section; anchoring it to the window keeps the camera inside.
	 */
	public static int octreeMinBlockY(int vanilla) {
		if (lastMinSectionY == Integer.MIN_VALUE) {
			return vanilla;
		}
		// The Octree adds its own box height to this, so stay clear of the int limit.
		return (int) Math.clamp((long) lastMinSectionY * 16, Integer.MIN_VALUE + 1024L, Integer.MAX_VALUE - 1024L);
	}

	/** True once the player is close enough to a window edge that the cube would be cut off. */
	public static boolean needsRebuild() {
		ClientLevel level = Minecraft.getInstance().level;
		LocalPlayer player = Minecraft.getInstance().player;
		if (level == null || player == null || lastMinSectionY == Integer.MIN_VALUE) {
			return false;
		}
		int radius = radius();
		int sectionY = SectionPos.blockToSectionCoord(player.blockPosition().getY());
		return sectionY - lastMinSectionY < radius || lastMaxSectionY - sectionY < radius;
	}
}
