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
		return STORES.computeIfAbsent(directory(level).toString(), ignored -> new CubeRegionStorage(directory(level)));
	}

	public static void closeAll() {
		for (CubeRegionStorage storage : STORES.values()) {
			storage.close();
		}
		STORES.clear();
	}

	private static Path directory(ServerLevel level) {
		Identifier dimension = level.dimension().identifier();
		return level.getServer().getWorldPath(LevelResource.ROOT)
				.resolve("dividedchunks")
				.resolve(dimension.getNamespace())
				.resolve(dimension.getPath());
	}
}
