package net.iskaa303.dividedchunks.gametest;

import net.iskaa303.dividedchunks.core.world.ColumnSectionAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

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

	/** A section applied as a cube payload must still cast a sky shadow, though the engine only knows the band. */
	public static void skyLightUnderOutOfBandRoof(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		int roofY = 10005;
		int x = 500 * 16;
		int z = 500 * 16;
		LevelChunkSection section = new LevelChunkSection(level.palettedContainerFactory());
		section.setBlockState(0, Math.floorMod(roofY, 16), 0, Blocks.STONE.defaultBlockState());
		LevelChunk chunk = level.getChunk(500, 500);
		((ColumnSectionAccess) chunk).dividedchunks$putSection(SectionPos.blockToSectionCoord(roofY), section);

		int above = level.getBrightness(LightLayer.SKY, new BlockPos(x, roofY + 1, z));
		int below = level.getBrightness(LightLayer.SKY, new BlockPos(x, roofY - 1, z));
		int beside = level.getBrightness(LightLayer.SKY, new BlockPos(x + 1, roofY - 1, z));
		if (above != 15) {
			throw helper.assertionException("sky light above the out-of-band roof is %s, expected 15".formatted(above));
		}
		if (below != 0) {
			throw helper.assertionException("sky light under the out-of-band roof is %s, expected 0".formatted(below));
		}
		if (beside != 15) {
			throw helper.assertionException("sky light beside the out-of-band roof is %s, expected 15".formatted(beside));
		}
		helper.succeed();
	}
}
