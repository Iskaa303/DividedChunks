package net.iskaa303.dividedchunks.gametest;

import net.iskaa303.dividedchunks.core.storage.CubeRegionStorage;
import net.iskaa303.dividedchunks.core.world.ColumnSectionAccess;
import net.iskaa303.dividedchunks.core.world.CubeStores;
import net.iskaa303.dividedchunks.core.world.SectionCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.Map;

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

	/** Saving a chunk must move its whole column, band included, into the dividedchunks store. */
	public static void saveStoresWholeColumn(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		int chunkX = 700;
		int chunkZ = 700;
		int bandY = 100;
		int farY = 10005;
		LevelChunk chunk = level.getChunk(chunkX, chunkZ);
		chunk.setBlockState(new BlockPos(chunkX * 16, bandY, chunkZ * 16), Blocks.STONE.defaultBlockState(), 2);

		LevelChunkSection farSection = new LevelChunkSection(level.palettedContainerFactory());
		farSection.setBlockState(0, Math.floorMod(farY, 16), 0, Blocks.DIRT.defaultBlockState());
		((ColumnSectionAccess) chunk).dividedchunks$putSection(SectionPos.blockToSectionCoord(farY), farSection);

		level.getChunkSource().save(true);

		CubeRegionStorage store = CubeStores.get(level);
		Map<Integer, byte[]> column = store.readColumn(chunkX, chunkZ);
		int bandSectionY = SectionPos.blockToSectionCoord(bandY);
		int farSectionY = SectionPos.blockToSectionCoord(farY);
		if (!column.containsKey(bandSectionY)) {
			throw helper.assertionException("band section %s is missing from the store".formatted(bandSectionY));
		}
		if (!column.containsKey(farSectionY)) {
			throw helper.assertionException("section %s is missing from the store".formatted(farSectionY));
		}
		LevelChunkSection stored = SectionCodec.decode(column.get(bandSectionY), level.palettedContainerFactory());
		if (stored.getBlockState(0, Math.floorMod(bandY, 16), 0).getBlock() != Blocks.STONE) {
			throw helper.assertionException("the stored band section lost the block written to it");
		}

		CompoundTag vanillaTag = level.getChunkSource().chunkMap.read(new ChunkPos(chunkX, chunkZ)).join().orElse(null);
		if (vanillaTag == null) {
			throw helper.assertionException("the vanilla region file holds no entry for the chunk");
		}
		if (vanillaTag.contains("sections")) {
			throw helper.assertionException("the vanilla region file still stores the chunk's sections");
		}
		if (vanillaTag.contains("isLightOn")) {
			throw helper.assertionException("the vanilla region file still stores the light flag");
		}
		helper.succeed();
	}
}
