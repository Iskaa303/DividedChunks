package net.iskaa303.dividedchunks.gametest;

import net.iskaa303.dividedchunks.DividedChunks;
import net.iskaa303.dividedchunks.core.config.DividedChunksConfig;
import net.iskaa303.dividedchunks.core.storage.CubeRegionStorage;
import net.iskaa303.dividedchunks.core.world.ColumnSectionAccess;
import net.iskaa303.dividedchunks.core.world.CubeStores;
import net.iskaa303.dividedchunks.core.world.SectionCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

	/**
	 * Cover dims by distance instead of blackening: a lone block barely registers, a wide roof only dims,
	 * and only an enclosed volume goes dark.
	 */
	public static void skyLightUnderCover(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		DividedChunksConfig.DimensionConfig config = DividedChunks.config()
				.forDimension(level.dimension().identifier().toString());
		int savedCone = config.skyConeAngleDegrees;
		int y = 5000;
		try {
			config.skyConeAngleDegrees = 45;
			int loneX = 900 * 16 + 8;
			int loneZ = 900 * 16 + 8;
			level.getChunk(900, 900)
					.setBlockState(new BlockPos(loneX, y + 5000, loneZ), Blocks.STONE.defaultBlockState(), 2);
			assertSkyLight(helper, "under a single block 5000 blocks up", level, loneX, y, loneZ, 15);

			int roofX = 910 * 16 + 8;
			int roofZ = 910 * 16 + 8;
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					((ColumnSectionAccess) level.getChunk(910 + dx, 910 + dz))
							.dividedchunks$putSection(625, stoneSection(level));
				}
			}
			int underRoof = level.getBrightness(LightLayer.SKY, new BlockPos(roofX, 9999, roofZ));
			if (underRoof <= 0 || underRoof >= 15) {
				throw helper.assertionException(
						"sky light under a 3x3 chunk roof is %s, expected a dim value between 1 and 14".formatted(underRoof));
			}
			assertSkyLight(helper, "beside the roof", level, roofX + 40, 9999, roofZ, 15);

			int boxX = 920 * 16 + 8;
			int boxZ = 920 * 16 + 8;
			LevelChunk boxChunk = level.getChunk(920, 920);
			for (int dx = -2; dx <= 2; dx++) {
				for (int dy = 0; dy <= 4; dy++) {
					for (int dz = -2; dz <= 2; dz++) {
						if (Math.abs(dx) == 2 || Math.abs(dz) == 2 || dy == 0 || dy == 4) {
							boxChunk.setBlockState(new BlockPos(boxX + dx, y + dy, boxZ + dz),
									Blocks.STONE.defaultBlockState(), 2);
						}
					}
				}
			}
			assertSkyLight(helper, "inside a sealed box", level, boxX, y + 2, boxZ, 0);

			config.skyConeAngleDegrees = 0;
			((ColumnSectionAccess) level.getChunk(910, 910)).dividedchunks$invalidateSkyAround(9999);
			assertSkyLight(helper, "under the roof with a vanilla reach", level, roofX, 9999, roofZ, 0);
		} finally {
			config.skyConeAngleDegrees = savedCone;
		}
		helper.succeed();
	}

	private static LevelChunkSection stoneSection(ServerLevel level) {
		LevelChunkSection section = new LevelChunkSection(level.palettedContainerFactory());
		for (int localX = 0; localX < 16; localX++) {
			for (int localY = 0; localY < 16; localY++) {
				for (int localZ = 0; localZ < 16; localZ++) {
					section.setBlockState(localX, localY, localZ, Blocks.STONE.defaultBlockState());
				}
			}
		}
		return section;
	}

	/**
	 * Light has to fall: a room whose only opening is above an inner wall still lights up, because the
	 * light that got over the wall drops into the far half instead of stopping at that one height.
	 */
	public static void skyLightFallsUnderCover(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		DividedChunksConfig.DimensionConfig config = DividedChunks.config()
				.forDimension(level.dimension().identifier().toString());
		int savedCone = config.skyConeAngleDegrees;
		int chunkX = 950;
		int chunkZ = 950;
		int baseY = 9008;
		try {
			config.skyConeAngleDegrees = 45;
			LevelChunkSection section = new LevelChunkSection(level.palettedContainerFactory());
			for (int localX = 0; localX < 16; localX++) {
				for (int localZ = 0; localZ < 16; localZ++) {
					boolean edge = localX == 0 || localX == 15 || localZ == 0 || localZ == 15;
					for (int localY = 0; localY <= 10; localY++) {
						boolean solid = localY == 0 || localY == 10 || (edge && localY < 10)
								|| (localX == 8 && localY <= 8);
						if (solid) {
							section.setBlockState(localX, localY, localZ, Blocks.STONE.defaultBlockState());
						}
					}
				}
			}
			// The one opening: a hole in the ceiling of the near room.
			section.setBlockState(3, 10, 3, Blocks.AIR.defaultBlockState());
			LevelChunk chunk = level.getChunk(chunkX, chunkZ);
			((ColumnSectionAccess) chunk).dividedchunks$putSection(baseY >> 4, section);

			int minX = chunkX * 16;
			int minZ = chunkZ * 16;
			int near = level.getBrightness(LightLayer.SKY, new BlockPos(minX + 3, baseY + 1, minZ + 3));
			if (near < 8) {
				throw helper.assertionException("sky light under the shaft is %s, expected a lit value".formatted(near));
			}
			int far = level.getBrightness(LightLayer.SKY, new BlockPos(minX + 14, baseY + 2, minZ + 14));
			if (far <= 0) {
				throw helper.assertionException(
						"sky light past the inner wall is 0, expected light to fall over the wall");
			}
			if (far >= 15) {
				throw helper.assertionException("sky light past the inner wall is %s, expected a dim value".formatted(far));
			}
		} finally {
			config.skyConeAngleDegrees = savedCone;
		}
		helper.succeed();
	}

	private static void assertSkyLight(GameTestHelper helper, String what, ServerLevel level, int x, int y, int z,
			int expected) {
		int light = level.getBrightness(LightLayer.SKY, new BlockPos(x, y, z));
		if (light != expected) {
			throw helper.assertionException("%s: sky light is %s, expected %s".formatted(what, light, expected));
		}
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

		Identifier dimension = level.dimension().identifier();
		Path regionFile = level.getServer().getWorldPath(LevelResource.ROOT)
				.resolve("dimensions").resolve(dimension.getNamespace()).resolve(dimension.getPath())
				.resolve("region").resolve("r." + (chunkX >> 5) + "." + (chunkZ >> 5) + ".mca");
		long sizeBeforeSave = regionSize(regionFile);

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
			throw helper.assertionException("the chunk has no stored metadata tag");
		}
		if (vanillaTag.contains("sections")) {
			throw helper.assertionException("the stored metadata tag still carries sections");
		}
		if (store.getTag(CubeRegionStorage.CHUNK_TAG_Y, chunkX, chunkZ).isEmpty()) {
			throw helper.assertionException("the chunk metadata is not in the dividedchunks store");
		}
		if (regionSize(regionFile) != sizeBeforeSave) {
			throw helper.assertionException("saving wrote a vanilla region file: " + regionFile);
		}
		helper.succeed();
	}

	/** Saving must move a column's entities and points of interest into the dividedchunks store too. */
	public static void saveStoresEntitiesAndPoi(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		int chunkX = 720;
		int chunkZ = 720;
		level.getChunk(chunkX, chunkZ);
		BlockPos poiPos = new BlockPos(chunkX * 16 + 4, 100, chunkZ * 16 + 4);
		level.setBlock(poiPos, Blocks.LECTERN.defaultBlockState(), 3);
		ItemEntity item = new ItemEntity(level, poiPos.getX() + 0.5, poiPos.getY() + 1, poiPos.getZ() + 0.5,
				new ItemStack(Items.STONE));
		if (!level.addFreshEntity(item)) {
			throw helper.assertionException("the item entity was not added to the level");
		}

		Identifier dimension = level.dimension().identifier();
		Path folder = level.getServer().getWorldPath(LevelResource.ROOT)
				.resolve("dimensions").resolve(dimension.getNamespace()).resolve(dimension.getPath());
		String region = "r." + (chunkX >> 5) + "." + (chunkZ >> 5) + ".mca";
		Path entityFile = folder.resolve("entities").resolve(region);
		Path poiFile = folder.resolve("poi").resolve(region);
		long entitySize = regionSize(entityFile);
		long poiSize = regionSize(poiFile);

		level.save(null, true, false);

		CubeRegionStorage store = CubeStores.get(level);
		if (store.getTag(CubeRegionStorage.ENTITY_TAG_Y, chunkX, chunkZ).isEmpty()) {
			throw helper.assertionException("the entities are not in the dividedchunks store");
		}
		if (store.getTag(CubeRegionStorage.POI_TAG_Y, chunkX, chunkZ).isEmpty()) {
			throw helper.assertionException("the points of interest are not in the dividedchunks store");
		}
		if (regionSize(entityFile) != entitySize) {
			throw helper.assertionException("saving wrote a vanilla entity file: " + entityFile);
		}
		if (regionSize(poiFile) != poiSize) {
			throw helper.assertionException("saving wrote a vanilla point-of-interest file: " + poiFile);
		}
		helper.succeed();
	}

	private static long regionSize(Path path) {
		try {
			return Files.exists(path) ? Files.size(path) : -1;
		} catch (IOException e) {
			return -2;
		}
	}
}
