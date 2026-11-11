package net.iskaa303.dividedchunks.core.light;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.iskaa303.dividedchunks.core.world.ColumnSectionAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/** Light-emitting blocks, packed four ints per emitter as {@code x, y, z, level}. */
public final class Emitters {

	private static final int STRIDE = 4;
	

	private Emitters() {
	}

	/** Scans a column's stored sections; a chunk usually holds no emitter, so the result is empty. */
	public static int[] of(Level level, ChunkAccess chunk) {
		IntArrayList found = new IntArrayList();
		int baseX = chunk.getPos().getMinBlockX();
		int baseZ = chunk.getPos().getMinBlockZ();
		((ColumnSectionAccess) chunk).dividedchunks$forEachSection((sectionY, section) -> {
			if (section.hasOnlyAir()) {
				return;
			}
			for (int localY = 0; localY < 16; localY++) {
				for (int localZ = 0; localZ < 16; localZ++) {
					for (int localX = 0; localX < 16; localX++) {
						BlockState state = section.getBlockState(localX, localY, localZ);
						int emission = state.getLightEmission();
						if (emission > 0) {
							int x = baseX + localX;
							int y = (sectionY << 4) + localY;
							int z = baseZ + localZ;
							found.add(x);
							found.add(y);
							found.add(z);
							found.add(emission);
						}
					}
				}
			}
		});
		return found.toIntArray();
	}

	/** Emitters within {@code reach} blocks of a section box, taken from the column and its neighbours. */
	public static int[] near(Level level, ChunkAccess chunk, int sectionY, int reach) {
		int minY = (sectionY << 4) - reach;
		int maxY = (sectionY << 4) + 15 + reach;
		ChunkPos pos = chunk.getPos();
		IntArrayList kept = new IntArrayList();
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				ChunkAccess source = dx == 0 && dz == 0 ? chunk
						: level.getChunkSource().getChunkNow(pos.x() + dx, pos.z() + dz);
				if (source == null) {
					continue;
				}
				int[] emitters = ((ColumnSectionAccess) source).dividedchunks$emitters();
				for (int i = 0; i + STRIDE - 1 < emitters.length; i += STRIDE) {
					if (emitters[i + 1] < minY || emitters[i + 1] > maxY) {
						continue;
					}
					kept.add(emitters[i]);
					kept.add(emitters[i + 1]);
					kept.add(emitters[i + 2]);
					kept.add(emitters[i + 3]);
				}
			}
		}
		return kept.toIntArray();
	}
}
