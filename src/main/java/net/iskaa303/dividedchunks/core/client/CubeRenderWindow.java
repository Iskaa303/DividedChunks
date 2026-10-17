package net.iskaa303.dividedchunks.core.client;

import net.iskaa303.dividedchunks.DividedChunks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.SectionPos;

/**
 * The client's render-section band: a fixed-height window that follows the player vertically, so
 * sections outside the vanilla band can be rendered. The window is tall enough to always cover the
 * vanilla band while the player is inside or near it.
 */
public final class CubeRenderWindow {

	private static int lastMinSectionY = Integer.MIN_VALUE;
	private static int lastMaxSectionY = Integer.MIN_VALUE;

	private CubeRenderWindow() {
	}

	private static int radius() {
		return Math.max(0, DividedChunks.config().verticalViewDistanceCubes);
	}

	/** Height of the window in sections, or 0 without a client level. */
	public static int height() {
		ClientLevel level = Minecraft.getInstance().level;
		return level == null ? 0 : level.getMaxSectionY() - level.getMinSectionY() + 1 + 2 * radius();
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

	/**
	 * Lower block bound for the occlusion Octree, which starts its vertical box at the world's
	 * bottom unless the box already spans every section. Anchoring it to the window instead keeps
	 * the camera inside the box once the window has left the vanilla band.
	 */
	public static int octreeMinBlockY(int vanilla) {
		if (lastMinSectionY == Integer.MIN_VALUE) {
			return vanilla;
		}
		// The Octree adds its own box height to this, so stay clear of the int limit.
		return (int) Math.clamp((long) lastMinSectionY * 16, Integer.MIN_VALUE + 1024L, Integer.MAX_VALUE - 1024L);
	}

	/** True once the player has left the middle half of the current window. */
	public static boolean needsRebuild() {
		ClientLevel level = Minecraft.getInstance().level;
		LocalPlayer player = Minecraft.getInstance().player;
		if (level == null || player == null || lastMinSectionY == Integer.MIN_VALUE) {
			return false;
		}
		int margin = (lastMaxSectionY - lastMinSectionY) / 4;
		int sectionY = SectionPos.blockToSectionCoord(player.blockPosition().getY());
		return sectionY < lastMinSectionY + margin || sectionY > lastMaxSectionY - margin;
	}
}
