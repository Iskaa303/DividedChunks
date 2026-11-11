package net.iskaa303.dividedchunks.core.world;

import net.iskaa303.dividedchunks.core.storage.CubeRegionStorage;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One {@link CubeRegionStorage} per dimension, under
 * {@code <world>/dividedchunks/<namespace>/<path>}; closed when the server stops.
 */
public final class CubeStores {

	private static final Map<String, CubeRegionStorage> STORES = new ConcurrentHashMap<>();

	private CubeStores() {
	}

	public static CubeRegionStorage get(ServerLevel level) {
		Identifier dimension = level.dimension().identifier();
		return get(level.getServer().getWorldPath(LevelResource.ROOT), dimension.getNamespace(), dimension.getPath());
	}

	/** The store of a dimension, given its directory {@code <world>/dimensions/<namespace>/<path>}. */
	public static CubeRegionStorage get(Path dimensionDirectory) {
		Path namespace = dimensionDirectory.getParent();
		Path world = namespace == null ? null : namespace.getParent();
		world = world == null ? null : world.getParent();
		if (world == null) {
			throw new IllegalArgumentException("Not a dimension directory: " + dimensionDirectory);
		}
		return get(world, namespace.getFileName().toString(), dimensionDirectory.getFileName().toString());
	}

	public static void closeAll() {
		for (CubeRegionStorage storage : STORES.values()) {
			storage.close();
		}
		STORES.clear();
	}

	private static CubeRegionStorage get(Path world, String namespace, String path) {
		// One handle per file: the chunk store arrives from the server's world path, the entity and
		// point-of-interest stores from their own folder, and interleaved appends corrupt the region.
		Path directory = world.resolve("dividedchunks").resolve(namespace).resolve(path)
				.toAbsolutePath().normalize();
		return STORES.computeIfAbsent(directory.toString(), ignored -> new CubeRegionStorage(directory));
	}
}
