package net.iskaa303.dividedchunks.core.world;

import net.iskaa303.dividedchunks.api.util.CubePos;
import net.iskaa303.dividedchunks.core.storage.CubeRegionStorage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Reads and writes a whole chunk column through the {@code .dcr} store. */
public final class ColumnPersistence {

	private ColumnPersistence() {
	}

	/**
	 * Writes every non-empty section of the column and drops the records of sections that are gone.
	 * Takes section copies rather than the chunk, so it can run off the thread that saves it.
	 */
	public static void write(CubeRegionStorage store, PalettedContainerFactory factory, int chunkX, int chunkZ,
			Map<Integer, LevelChunkSection> sections) {
		Set<Integer> written = new HashSet<>();
		for (Map.Entry<Integer, LevelChunkSection> entry : sections.entrySet()) {
			LevelChunkSection section = entry.getValue();
			if (section.hasOnlyAir()) {
				continue;
			}
			store.put(new CubePos(chunkX, entry.getKey(), chunkZ), SectionCodec.encode(section, factory));
			written.add(entry.getKey());
		}
		for (Integer sectionY : store.readColumn(chunkX, chunkZ).keySet()) {
			if (!written.contains(sectionY)) {
				store.remove(new CubePos(chunkX, sectionY, chunkZ));
			}
		}
	}

	/** Loads the stored column into {@code chunk}; vanilla's sections stay when the store has none. */
	public static void read(ServerLevel level, ChunkAccess chunk) {
		Map<Integer, byte[]> stored = CubeStores.get(level).readColumn(chunk.getPos().x(), chunk.getPos().z());
		if (stored.isEmpty()) {
			return;
		}
		ColumnSectionAccess access = (ColumnSectionAccess) chunk;
		for (Map.Entry<Integer, byte[]> entry : stored.entrySet()) {
			access.dividedchunks$putSection(entry.getKey(),
					SectionCodec.decode(entry.getValue(), level.palettedContainerFactory()));
		}
	}
}
