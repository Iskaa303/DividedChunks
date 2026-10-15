package net.iskaa303.dividedchunks.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

/**
 * In-game checks for DividedChunks, shared by the loader-specific GameTest entries.
 */
public final class GameTests {

	private GameTests() {
	}

	/** Writes a block far outside the vanilla build height and reads it back. */
	public static void blockOutsideBuildHeight(GameTestHelper helper) {
		BlockPos pos = new BlockPos(0, 1000000, 0);
		helper.setBlock(pos, Blocks.STONE);
		helper.assertBlockPresent(Blocks.STONE, pos);
		helper.succeed();
	}
}
