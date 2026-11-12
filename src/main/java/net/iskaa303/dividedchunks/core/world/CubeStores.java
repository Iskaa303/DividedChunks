package net.iskaa303.dividedchunks.core.world;

import net.iskaa303.dividedchunks.core.storage.CubeRegionStorage;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One {@link CubeRegionStorage} per dimension, inside the dimension's own directory
 * ({@code <world>/dimensions/<namespace>/<path>/dividedchunks}); closed when the server stops.
 */
public final class CubeStores {

	/** Folder of the cube regions inside a dimension directory. */
	public static final String STORE_FOLDER = "dividedchunks";

	private static final Map<String, CubeRegionStorage> STORES = new ConcurrentHashMap<>();

	private CubeStores() {
	}

	public static CubeRegionStorage get(ServerLevel level) {
		Identifier dimension = level.dimension().identifier();
		return get(level.getServer().getWorldPath(LevelResource.ROOT)
				.resolve("dimensions").resolve(dimension.getNamespace()).resolve(dimension.getPath()));
	}

	/** The store of the dimension whose directory this is. */
	public static CubeRegionStorage get(Path dimensionDirectory) {
		// One handle per file: the chunk store arrives from the server's world path, the entity and
		// point-of-interest stores from their own folder, and interleaved appends corrupt the region.
		Path directory = dimensionDirectory.resolve(STORE_FOLDER).toAbsolutePath().normalize();
		return STORES.computeIfAbsent(directory.toString(), ignored -> new CubeRegionStorage(directory));
	}

	public static void closeAll() {
		for (CubeRegionStorage storage : STORES.values()) {
			storage.close();
		}
		STORES.clear();
	}
}
