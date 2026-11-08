package net.iskaa303.dividedchunks.core.light;

import net.iskaa303.dividedchunks.core.world.ColumnSectionAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * Block opacity for the light flood; the last chunk is kept because a flood walks one section level
 * at a time and rarely leaves it.
 */
public final class ChunkBlocks implements SkyLight.Blocks {

	private final Level level;

	private LevelChunk chunk;
	private int chunkX = Integer.MIN_VALUE;
	private int chunkZ = Integer.MIN_VALUE;

	public ChunkBlocks(Level level) {
		this.level = level;
	}

	@Override
	public boolean opaque(int worldX, int worldY, int worldZ) {
		int x = worldX >> 4;
		int z = worldZ >> 4;
		if (x != this.chunkX || z != this.chunkZ) {
			this.chunk = this.level.getChunkSource().getChunkNow(x, z);
			this.chunkX = x;
			this.chunkZ = z;
		}
		if (this.chunk == null) {
			return false;
		}
		LevelChunkSection section =
				((ColumnSectionAccess) this.chunk).dividedchunks$getSectionByY(worldY >> 4);
		return section != null && section.getBlockState(worldX & 15, worldY & 15, worldZ & 15).getLightDampening() > 0;
	}
}
