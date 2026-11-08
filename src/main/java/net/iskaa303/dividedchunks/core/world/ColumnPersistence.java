package net.iskaa303.dividedchunks.core.world;

import net.iskaa303.dividedchunks.api.util.CubePos;
import net.iskaa303.dividedchunks.core.storage.CubeRegionStorage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Reads and writes a whole chunk column through the {@code .dcr} store; {@code .mca} keeps the metadata. */
public final class ColumnPersistence {

	private ColumnPersistence() {
	}

	/** Writes every non-empty section of the column, dropping records for sections that are gone. */
	public static void write(ServerLevel level, ChunkAccess chunk) {
		CubeRegionStorage store = CubeStores.get(level);
		int chunkX = chunk.getPos().x();
		int chunkZ = chunk.getPos().z();
		Set<Integer> written = new HashSet<>();
		((ColumnSectionAccess) chunk).dividedchunks$forEachSection((sectionY, section) -> {
			if (section.hasOnlyAir()) {
				return;
			}
			store.put(new CubePos(chunkX, sectionY, chunkZ), SectionCodec.encode(section, level.palettedContainerFactory()));
			written.add(sectionY);
		});
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
