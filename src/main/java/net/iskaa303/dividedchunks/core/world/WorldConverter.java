package net.iskaa303.dividedchunks.core.world;

import net.iskaa303.dividedchunks.api.util.CubePos;
import net.iskaa303.dividedchunks.core.storage.CubeRegionStorage;
import net.iskaa303.dividedchunks.core.storage.McaFile;
import net.minecraft.nbt.CompoundTag;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Turns the {@code .mca} files of a vanilla world into cube region files: each chunk section becomes one
 * record, the rest of the chunk tag becomes the column payload, and entities and points of interest
 * move over as they are. The world is written into a copy, so the vanilla files stay in the original.
 */
public final class WorldConverter {

	private static final String CONVERTING_SUFFIX = ".converting";
	private static final String STORE_FOLDER = CubeStores.STORE_FOLDER;

	private WorldConverter() {
	}

	@FunctionalInterface
	public interface Progress {
		void report(String file, int done, int total);

		Progress NONE = (file, done, total) -> {
		};
	}

	/** What a conversion wrote. */
	public record Report(int dimensions, int chunks, int sections, int entities, int pointsOfInterest) {
	}

	/**
	 * Converts the world at {@code worldDirectory} into a copy of itself that keeps the original folder
	 * name and everything except the {@code .mca} files, which become cube region files there. The
	 * original is renamed to a {@code _non_cubic} sibling and left as it was.
	 */
	public static Report convert(Path worldDirectory, Progress progress) {
		if (WorldKind.of(worldDirectory) == WorldKind.CUBIC) {
			return new Report(0, 0, 0, 0, 0);
		}
		List<Path> dimensions = dimensionDirectories(worldDirectory);
		Path staging = sibling(worldDirectory, CONVERTING_SUFFIX);
		int done = 0;
		int total = 0;
		for (Path dimension : dimensions) {
			total += regionFiles(dimension).size();
		}
		int chunks = 0;
		int sections = 0;
		int entities = 0;
		int pointsOfInterest = 0;
		if (total == 0) {
			throw new IllegalStateException("No vanilla region files under " + worldDirectory.resolve("dimensions"));
		}
		try {
			delete(staging);
			copyWithoutVanillaFiles(worldDirectory, staging);
			for (Path dimension : dimensions) {
				Path storeDirectory = staging.resolve(worldDirectory.relativize(dimension));
				CubeRegionStorage store = new CubeRegionStorage(storeDirectory.resolve(STORE_FOLDER));
				try {
					for (Path region : regionFiles(dimension)) {
						progress.report(worldDirectory.relativize(region).toString(), done++, total);
						Result result = convertRegion(store, region);
						chunks += result.chunks;
						sections += result.sections;
						entities += result.entities;
						pointsOfInterest += result.pointsOfInterest;
					}
					store.flush();
				} finally {
					store.close();
				}
			}
			Files.move(worldDirectory, freeBackup(worldDirectory));
			Files.move(staging, worldDirectory);
		} catch (IOException e) {
			delete(staging);
			throw new UncheckedIOException("Failed to convert " + worldDirectory, e);
		}
		return new Report(dimensions.size(), chunks, sections, entities, pointsOfInterest);
	}

	private record Result(int chunks, int sections, int entities, int pointsOfInterest) {
	}

	private static Result convertRegion(CubeRegionStorage store, Path region) {
		Path folder = region.getParent();
		String name = folder.getFileName().toString();
		int chunks = 0;
		int sections = 0;
		int entities = 0;
		int pointsOfInterest = 0;
		List<McaFile.Chunk> tags = new ArrayList<>();
		McaFile.read(region, tags::add);
		for (McaFile.Chunk entry : tags) {
			CompoundTag tag = entry.tag();
			switch (name) {
				case "entities" -> {
					store.putTag(CubeRegionStorage.ENTITY_TAG_Y, entry.x(), entry.z(), ChunkTags.toBytes(tag));
					entities++;
				}
				case "poi" -> {
					store.putTag(CubeRegionStorage.POI_TAG_Y, entry.x(), entry.z(), ChunkTags.toBytes(tag));
					pointsOfInterest++;
				}
				default -> {
					for (Map.Entry<Integer, CompoundTag> section : VanillaChunks.sections(tag).entrySet()) {
						store.put(new CubePos(entry.x(), section.getKey(), entry.z()),
								ChunkTags.toBytes(section.getValue()));
						sections++;
					}
					store.putTag(CubeRegionStorage.CHUNK_TAG_Y, entry.x(), entry.z(),
							ChunkTags.toBytes(VanillaChunks.metadata(tag)));
					chunks++;
				}
			}
		}
		return new Result(chunks, sections, entities, pointsOfInterest);
	}

	/** Every {@code dimensions/<namespace>/<path>} folder of the world. */
	static List<Path> dimensionDirectories(Path worldDirectory) {
		List<Path> directories = new ArrayList<>();
		Path dimensions = worldDirectory.resolve("dimensions");
		if (Files.isDirectory(dimensions)) {
			try (Stream<Path> namespaces = Files.list(dimensions)) {
				namespaces.filter(Files::isDirectory).forEach(namespace -> {
					try (Stream<Path> paths = Files.list(namespace)) {
						paths.filter(Files::isDirectory).forEach(directories::add);
					} catch (IOException e) {
						throw new UncheckedIOException("Failed to list " + namespace, e);
					}
				});
			} catch (IOException e) {
				throw new UncheckedIOException("Failed to list " + dimensions, e);
			}
		}
		return directories;
	}

	static List<Path> regionFiles(Path dimensionDirectory) {
		List<Path> files = new ArrayList<>();
		for (String folder : List.of("region", "entities", "poi")) {
			Path path = dimensionDirectory.resolve(folder);
			if (!Files.isDirectory(path)) {
				continue;
			}
			try (Stream<Path> entries = Files.list(path)) {
				entries.filter(file -> file.getFileName().toString().endsWith(".mca"))
						.sorted(Comparator.comparing(Path::toString))
						.forEach(files::add);
			} catch (IOException e) {
				throw new UncheckedIOException("Failed to list " + path, e);
			}
		}
		return files;
	}

	/** Copies the world without its vanilla chunk storage, which the conversion replaces. */
	private static void copyWithoutVanillaFiles(Path source, Path target) throws IOException {
		Files.createDirectories(target);
		try (Stream<Path> paths = Files.walk(source)) {
			for (Path path : (Iterable<Path>) paths::iterator) {
				if (Files.isDirectory(path) || !isKept(path)) {
					continue;
				}
				Path destination = target.resolve(source.relativize(path).toString());
				Files.createDirectories(destination.getParent());
				Files.copy(path, destination);
			}
		}
	}

	private static boolean isKept(Path file) {
		String name = file.getFileName().toString();
		return !name.endsWith(".mca") && !name.endsWith(".mcc") && !name.equals("session.lock");
	}

	private static Path freeBackup(Path worldDirectory) {
		Path backup = sibling(worldDirectory, "_non_cubic");
		for (int i = 2; Files.exists(backup); i++) {
			backup = sibling(worldDirectory, "_non_cubic_" + i);
		}
		return backup;
	}

	private static Path sibling(Path worldDirectory, String suffix) {
		return worldDirectory.resolveSibling(worldDirectory.getFileName() + suffix);
	}

	private static void delete(Path directory) {
		if (!Files.exists(directory)) {
			return;
		}
		try (Stream<Path> paths = Files.walk(directory)) {
			for (Path path : (Iterable<Path>) paths::iterator) {
				Files.deleteIfExists(path);
			}
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to delete " + directory, e);
		}
	}
}
