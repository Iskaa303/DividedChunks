package net.iskaa303.dividedchunks.core.storage;

import net.iskaa303.dividedchunks.api.util.CubePos;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Cube storage split into {@link CubeRegionFile#REGION_SIZE}&#215;{@code REGION_SIZE}-column region
 * files, opened on demand with an LRU cap so many regions do not exhaust file handles.
 */
public final class CubeRegionStorage implements CubeStorage {

	/** Section Y of the chunk metadata payload. */
	public static final int CHUNK_TAG_Y = Integer.MIN_VALUE;

	/** Section Y of a column's entity payload. */
	public static final int ENTITY_TAG_Y = Integer.MIN_VALUE + 1;

	/** Section Y of a column's point-of-interest payload. */
	public static final int POI_TAG_Y = Integer.MIN_VALUE + 2;

	/** Whether this section Y holds a column payload instead of a section. */
	public static boolean isTagY(int sectionY) {
		return sectionY >= CHUNK_TAG_Y && sectionY <= POI_TAG_Y;
	}

	private static final int DEFAULT_MAX_OPEN_REGIONS = 64;

	private final Path directory;
	private final int maxOpenRegions;
	private final Map<Long, CubeRegionFile> open = new LinkedHashMap<>(16, 0.75f, true);

	public CubeRegionStorage(Path directory) {
		this(directory, DEFAULT_MAX_OPEN_REGIONS);
	}

	public CubeRegionStorage(Path directory, int maxOpenRegions) {
		this.directory = directory;
		this.maxOpenRegions = Math.max(1, maxOpenRegions);
	}

	@Override
	public synchronized Optional<byte[]> get(CubePos pos) {
		int regionX = Math.floorDiv(pos.x(), CubeRegionFile.REGION_SIZE);
		int regionZ = Math.floorDiv(pos.z(), CubeRegionFile.REGION_SIZE);
		long key = key(regionX, regionZ);
		CubeRegionFile region = this.open.get(key);
		if (region == null) {
			Path file = fileFor(regionX, regionZ);
			if (!Files.exists(file)) {
				return Optional.empty();
			}
			region = openRegion(key, regionX, regionZ, file);
		}
		return region.get(pos);
	}

	@Override
	public synchronized void put(CubePos pos, byte[] data) {
		int regionX = Math.floorDiv(pos.x(), CubeRegionFile.REGION_SIZE);
		int regionZ = Math.floorDiv(pos.z(), CubeRegionFile.REGION_SIZE);
		openOrCreate(key(regionX, regionZ), regionX, regionZ).put(pos, data);
	}

	@Override
	public synchronized boolean remove(CubePos pos) {
		int regionX = Math.floorDiv(pos.x(), CubeRegionFile.REGION_SIZE);
		int regionZ = Math.floorDiv(pos.z(), CubeRegionFile.REGION_SIZE);
		long key = key(regionX, regionZ);
		CubeRegionFile region = this.open.get(key);
		if (region == null) {
			Path file = fileFor(regionX, regionZ);
			if (!Files.exists(file)) {
				return false;
			}
			region = openRegion(key, regionX, regionZ, file);
		}
		return region.remove(pos);
	}

	public synchronized void flush() {
		for (CubeRegionFile region : this.open.values()) {
			region.flush();
		}
	}

	/** Every stored section of one chunk column, keyed by section Y. */
	public synchronized Map<Integer, byte[]> readColumn(int chunkX, int chunkZ) {
		int regionX = Math.floorDiv(chunkX, CubeRegionFile.REGION_SIZE);
		int regionZ = Math.floorDiv(chunkZ, CubeRegionFile.REGION_SIZE);
		long key = key(regionX, regionZ);
		CubeRegionFile region = this.open.get(key);
		if (region == null) {
			Path file = fileFor(regionX, regionZ);
			if (!Files.exists(file)) {
				return Map.of();
			}
			region = openRegion(key, regionX, regionZ, file);
		}
		return region.readColumn(chunkX, chunkZ);
	}

	/** The stored payload of one kind at a column, if any. */
	public synchronized Optional<byte[]> getTag(int tagY, int chunkX, int chunkZ) {
		return get(new CubePos(chunkX, tagY, chunkZ));
	}

	public synchronized void putTag(int tagY, int chunkX, int chunkZ, byte[] data) {
		put(new CubePos(chunkX, tagY, chunkZ), data);
	}

	@Override
	public synchronized void close() {
		for (CubeRegionFile region : this.open.values()) {
			region.close();
		}
		this.open.clear();
	}

	private CubeRegionFile openOrCreate(long key, int regionX, int regionZ) {
		CubeRegionFile region = this.open.get(key);
		if (region != null) {
			return region;
		}
		return openRegion(key, regionX, regionZ, fileFor(regionX, regionZ));
	}

	private CubeRegionFile openRegion(long key, int regionX, int regionZ, Path file) {
		CubeRegionFile region = new CubeRegionFile(file, regionX, regionZ);
		this.open.put(key, region);
		while (this.open.size() > this.maxOpenRegions) {
			Iterator<Map.Entry<Long, CubeRegionFile>> iterator = this.open.entrySet().iterator();
			iterator.next().getValue().close();
			iterator.remove();
		}
		return region;
	}

	private Path fileFor(int regionX, int regionZ) {
		return this.directory.resolve("r." + regionX + "." + regionZ + ".dcr");
	}

	private static long key(int regionX, int regionZ) {
		return ((long) regionX << 32) ^ (regionZ & 0xffffffffL);
	}
}
